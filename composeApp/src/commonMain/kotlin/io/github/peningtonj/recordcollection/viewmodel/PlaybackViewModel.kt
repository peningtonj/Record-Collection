package io.github.peningtonj.recordcollection.viewmodel

import NEXT_ALBUM_TRIGGER_MS
import PlaybackQueueService
import TRANSITION_TRIGGER_MS
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

const val PLAYBACK_ACTIVE_POLLING_DELAY = 1500L
const val PLAYBACK_INACTIVE_POLLING_DELAY = 8000L
const val TRANSITIONING_POLLING_DELAY_MS = 150L

class PlaybackViewModel(
    private val playbackRepository: PlaybackRepository,
    private val queueManager: PlaybackQueueService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _currentSession = MutableStateFlow<PlaybackQueueService.QueueSession?>(null)
    val currentSession: StateFlow<PlaybackQueueService.QueueSession?> = _currentSession.asStateFlow()

    private val _differentPlaybackCount = MutableStateFlow(0)
    val differentPlaybackCount: StateFlow<Int> = _differentPlaybackCount.asStateFlow()

    private val _playbackState = MutableStateFlow<Playback?>(null)
    val playbackState: StateFlow<Playback?> = _playbackState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSessionAppInitialized = MutableStateFlow(true)

    private val playbackPoller = PlaybackPoller(
        playbackRepository = playbackRepository,
        onPlaybackUpdate = { playback ->
            _playbackState.value = playback
            handleQueueTransitions(playback)
        },
        onError = { error -> _error.value = error }
    )

    init {
        playbackPoller.start(viewModelScope)
    }

    private fun checkIfResumingCurrentPlayback(track: Track) : Boolean {
        if (_playbackState.value?.isPlaying ?: false) return false
        if (_playbackState.value?.track?.id == track.id) return true
        return false
    }

    private fun checkIfResumingCurrentPlayback(album: Album) : Boolean {
        if (_playbackState.value?.isPlaying ?: false) return false
        if (_playbackState.value?.track?.album?.id == album.id) return true
        return false
    }


    private suspend fun handleQueueTransitions(playback: Playback?) {
        val session = _currentSession.value
        if (
            _isSessionAppInitialized.value &&
            session != null &&
            playback?.track?.album?.id != session.album.id &&
            playback?.track?.album?.id != null &&
            playback.track.spotifyUri != session.transitionTrackUri
        ) {
            Napier.d("Not a local session because ${playback.track.album.id} != ${session.album.id}")
            _differentPlaybackCount.value += 1

            if (differentPlaybackCount.value > 3) {
                _isSessionAppInitialized.value = false
                _currentSession.value = null
            }
        } else {
            _differentPlaybackCount.value = 0
        }

        if (_isSessionAppInitialized.value && session != null) {
            // Check for transition track addition
            val settings = settingsRepository.settings.first()
            val transitionTime = if (settings.transitionTrack) {TRANSITION_TRIGGER_MS} else {NEXT_ALBUM_TRIGGER_MS}

            if (!settings.transitionTrack && queueManager.albumEnding(session, playback, TRANSITION_TRIGGER_MS)){
                playbackPoller.setPollingDelay(TRANSITIONING_POLLING_DELAY_MS)
            }

            if (queueManager.albumEnding(session, playback, transitionTime)) {
                if (settings.transitionTrack) {
                    addTransitionTrack(session)
                } else {
                    transitionToNextAlbum(session)
                }
            }

            // Check for transitioning to next album
            if (queueManager.shouldTransitionToNextAlbum(session, playback)) {
                transitionToNextAlbum(session)
            }
        }
    }

    private suspend fun addTransitionTrack(session: PlaybackQueueService.QueueSession) {
        queueManager.addTransitionTrack(session).fold(
            onSuccess = {
                _currentSession.value = session.copy(hasAddedTransitionTrack = true)
                playbackPoller.setPollingDelay(TRANSITIONING_POLLING_DELAY_MS)
                Napier.d("Transition track added for album: ${session.album.name}")
            },
            onFailure = { error ->
                _error.value = "Failed to add transition track: ${error.message}"
            }
        )
    }

    private fun transitionToNextAlbum(session: PlaybackQueueService.QueueSession) {
        if (session.queue.isNotEmpty()) {
            playAlbum(
                session.queue.first(), session.queue.drop(1),
                collection = session.playingFrom
            )
            playbackPoller.setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
        }
    }

    suspend fun startAlbumWithOrWithoutTrack(
        album: Album,
        track: Track? = null
    ) {
        if (track != null) {
            if (checkIfResumingCurrentPlayback((track))) {
                playbackRepository.resumePlayback()
            } else {
                playbackRepository.startAlbumPlaybackFromTrack(album, track)
            }
        } else {
            if (checkIfResumingCurrentPlayback((album))) {
                playbackRepository.resumePlayback()
            } else {
                playbackRepository.startAlbumPlayback(album)
            }
        }
    }

    fun playAlbum(
        album: AlbumDetailUiState,
        queue: List<AlbumDetailUiState> = emptyList(),
        startFromTrack: Track? = null,
        collection: AlbumCollection? = null,
        isShuffled: Boolean = false
    ) {
        viewModelScope.launch {
            executeWithLoading {
                val albumWithTracks = queueManager.ensureTracksLoaded(album)

                _currentSession.value = PlaybackQueueService.QueueSession(
                    album = albumWithTracks.album,
                    lastTrack = albumWithTracks.tracks.last(),
                    queue = queue,
                    playingFrom = collection,
                    isShuffled = isShuffled
                )

                playbackRepository.turnOffShuffle()
                startAlbumWithOrWithoutTrack(albumWithTracks.album, startFromTrack)

                updatePlaybackState()

                _isSessionAppInitialized.value = true
                playbackPoller.setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
            }
        }
    }

    fun toggleShuffle(albums: List<AlbumDetailUiState>) {
        val currentAlbum = _playbackState.value?.track?.album
        if (currentAlbum == null) return

        Napier.d { "Currently playing: ${currentAlbum.name}" }
        if (_currentSession.value?.isShuffled ?: false) {
            Napier.d { "Switching shuffle off" }
            val startIndex =  albums.indexOfFirst { it.album.id == currentAlbum.id }
            val queue = albums.drop(startIndex + 1)
            Napier.d { "Next album will be ${queue.first().album.name}" }
            setQueue(queue, false)
        } else {
            Napier.d { "Switching shuffle on" }
            val queue = albums.filter {
                it.album.id != playbackState.value?.track?.album?.id
            }.shuffled()
            Napier.d { "Next album will be ${queue.first().album.name}" }
            setQueue(queue, true)
        }
    }
    fun setQueue(
        queue: List<AlbumDetailUiState>,
        isShuffled: Boolean
    ) {
        _currentSession.value = _currentSession.value?.copy(
            queue = queue,
            isShuffled = isShuffled
        )
    }

    fun skipToNextAlbumInQueue() {
        viewModelScope.launch {
            executeWithLoading {
                val session = _currentSession.value ?: return@executeWithLoading
                val nextAlbum = session.queue.firstOrNull() ?: return@executeWithLoading
                playAlbum(
                    nextAlbum,
                    queue = session.queue.drop(1),
                    collection = session.playingFrom
                )
            }
        }
    }

    // Utility functions
    private suspend fun executeWithLoading(action: suspend () -> Unit) {
        _isLoading.value = true
        try {
            action()
            _error.value = null
        } catch (e: Exception) {
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun executePlaybackAction(action: suspend () -> Unit) {
        try {
            action()
            updatePlaybackState()
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    fun togglePlayPause() = viewModelScope.launch {
        executePlaybackAction {
            val isPlaying = _playbackState.value?.isPlaying == true
            if (isPlaying) playbackRepository.pausePlayback()
            else {
                playbackRepository.resumePlayback()
                playbackPoller.setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
            }
        }
    }

    fun next() {
        Napier.d("Next called")
        if (queueManager.isLastTrackInAlbum(
                _currentSession.value,
                _playbackState.value
            )
        ) {
            Napier.d { "Going to next Album" }
            skipToNextAlbumInQueue()
        } else {
            viewModelScope.launch {
                executePlaybackAction {
                    Napier.d("Next track called")
                    playbackRepository.skipToNext()
                    delay(100)
                }
            }
        }
    }

    fun previous() = viewModelScope.launch {
        executePlaybackAction {
            playbackRepository.skipToPrevious()
            delay(100)
        }
    }

    fun seek(positionMs: Long) = viewModelScope.launch {
        executePlaybackAction { playbackRepository.seekToPosition(positionMs) }
    }

    fun setShuffle(enabled: Boolean) = viewModelScope.launch {
        executePlaybackAction {
            if (enabled) {
                playbackRepository.turnOnShuffle()
                _currentSession.value = null // Clear session as shuffle breaks sequence
            } else {
                playbackRepository.turnOffShuffle()
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSession() {
        _currentSession.value = null
    }

    private suspend fun updatePlaybackState() {
        _playbackState.value = playbackRepository.getCurrentPlayback("From ViewModel")
    }

    override fun onCleared() {
        super.onCleared()
        playbackPoller.stop()
    }

}


    class PlaybackPoller(
        private val playbackRepository: PlaybackRepository,
        private val onPlaybackUpdate: suspend (Playback?) -> Unit,
        private val onError: (String?) -> Unit,
        private val pollerName: String = "PlaybackPoller ${Random.nextInt(100)}"
    ) {
        private var pollingJob: Job? = null
        private var _delayMs = MutableStateFlow(PLAYBACK_ACTIVE_POLLING_DELAY)

        fun start(scope: CoroutineScope) {
            pollingJob?.cancel()
            pollingJob = scope.launch {
                Napier.d("Starting polling for $pollerName")
                while (isActive) {
                    try {
//                        Napier.d("Polling for playback updates ($pollerName) ...")
                        val playback = playbackRepository.getCurrentPlayback(pollerName = pollerName)
//                        Napier.d("Playback update received: $playback")
                        onPlaybackUpdate(playback)
                        if (playback == null || !playback.isPlaying) {
                            _delayMs.value = PLAYBACK_INACTIVE_POLLING_DELAY
                        } else if (_delayMs.value == PLAYBACK_INACTIVE_POLLING_DELAY) {
                            _delayMs.value = PLAYBACK_ACTIVE_POLLING_DELAY
                        }
                        onError(null)
                    } catch (e: Exception) {
                        Napier.e("Error polling for playback updates ($pollerName): ${e.message}", e)
                        onError(e.message)
                    }
//                    Napier.d("Polling for playback updates ($pollerName) complete, delaying ${_delayMs.value}ms ...")
                    delay(_delayMs.value)
//                    Napier.d("Delay done ($pollerName) complete")

                }
                Napier.d("Polling stopped for $pollerName")
            }
        }

        fun setPollingDelay(delayMs: Long) {
            _delayMs.value = delayMs
        }

        fun stop() {
            Napier.d("Stopping polling for $pollerName")
//            Napier.(stackTrace = Throwable("Stack trace for debugging").fillInStackTrace())
            pollingJob?.cancel()
        }
    }
