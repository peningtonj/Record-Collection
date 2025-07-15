package io.github.peningtonj.recordcollection.viewmodel

import PlaybackQueueService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val PLAYBACK_ACTIVE_POLLING_DELAY = 1000L
const val PLAYBACK_INACTIVE_POLLING_DELAY = 1000L
const val TRANSITIONING_POLLING_DELAY_MS = 200L

class PlaybackViewModel(
    private val playbackRepository: PlaybackRepository,
    private val queueManager: PlaybackQueueService
) : ViewModel() {

    private val _currentSession = MutableStateFlow<PlaybackQueueService.QueueSession?>(null)
    val currentSession: StateFlow<PlaybackQueueService.QueueSession?> = _currentSession.asStateFlow()

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

    private suspend fun handleQueueTransitions(playback: Playback?) {
        val session = _currentSession.value
        if (
            _isSessionAppInitialized.value &&
            session != null &&
            playback?.track?.album?.id != session.album.id &&
            playback?.track?.spotifyUri != session.transitionTrackUri
        ) {
            Napier.d("Not a local session because ${playback?.track?.album?.id} != ${session.album.id}")
            _isSessionAppInitialized.value = false
        }

        if (_isSessionAppInitialized.value && session != null) {
            // Check for transition track addition
            if (queueManager.shouldAddTransitionTrack(session, playback)) {
                addTransitionTrack(session)
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
            playbackRepository.startAlbumPlaybackFromTrack(album, track)
        } else {
            playbackRepository.startAlbumPlayback(album)
        }
    }

    fun playAlbum(
        album: AlbumDetailUiState,
        queue: List<AlbumDetailUiState> = emptyList(),
        startFromTrack: Track? = null,
        collection: AlbumCollection? = null
    ) {
        viewModelScope.launch {
            executeWithLoading {
                val albumWithTracks = queueManager.ensureTracksLoaded(album)

                _currentSession.value = PlaybackQueueService.QueueSession(
                    album = albumWithTracks.album,
                    lastTrack = albumWithTracks.tracks.last(),
                    queue = queue,
                    playingFrom = collection
                )

                playbackRepository.turnOffShuffle()
                startAlbumWithOrWithoutTrack(albumWithTracks.album, startFromTrack)

                updatePlaybackState()

                _isSessionAppInitialized.value = true
            }
        }
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
            else playbackRepository.resumePlayback()
        }
    }

    fun next() = {
        if (queueManager.isLastTrackInAlbum(
                _currentSession.value,
                _playbackState.value
            )
        ) {
            skipToNextAlbumInQueue()
        } else {
            viewModelScope.launch {
                executePlaybackAction {
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
        _playbackState.value = playbackRepository.getCurrentPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        playbackPoller.stop()
    }

}


    class PlaybackPoller(
        private val playbackRepository: PlaybackRepository,
        private val onPlaybackUpdate: suspend (Playback?) -> Unit,
        private val onError: (String?) -> Unit
    ) {
        private var pollingJob: Job? = null
        private var _delayMs = MutableStateFlow(PLAYBACK_ACTIVE_POLLING_DELAY)

        fun start(scope: CoroutineScope) {
            pollingJob?.cancel()
            pollingJob = scope.launch {
                while (isActive) {
                    try {
                        val playback = playbackRepository.getCurrentPlayback()
                        onPlaybackUpdate(playback)
                        if (playback == null || !playback.isPlaying) {
                            _delayMs.value = PLAYBACK_INACTIVE_POLLING_DELAY
                        } else if (_delayMs.value == PLAYBACK_INACTIVE_POLLING_DELAY) {
                            _delayMs.value = PLAYBACK_ACTIVE_POLLING_DELAY
                        }
                        onError(null)
                    } catch (e: Exception) {
                        onError(e.message)
                    }
                    delay(_delayMs.value)
                }
            }
        }

        fun setPollingDelay(delayMs: Long) {
            _delayMs.value = delayMs
        }

        fun stop() {
            pollingJob?.cancel()
        }
    }
