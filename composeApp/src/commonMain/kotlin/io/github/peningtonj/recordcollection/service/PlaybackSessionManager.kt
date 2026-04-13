package io.github.peningtonj.recordcollection.service

import NEXT_ALBUM_TRIGGER_MS
import PlaybackQueueService
import TRANSITION_TRIGGER_MS
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

// Polling delays — defined here since PlaybackSessionManager owns the poller
const val PLAYBACK_ACTIVE_POLLING_DELAY = 1500L
const val PLAYBACK_INACTIVE_POLLING_DELAY = 8000L
const val TRANSITIONING_POLLING_DELAY_MS = 150L

/**
 * Long-lived session manager that lives in the DI container (process scope).
 *
 * Owns the [PlaybackPoller] and all playback/queue session state. Because this
 * object is held by the DI container — not by a ViewModel — its coroutine scope
 * and polling loop survive Activity recreation AND phone sleep, as long as an
 * Android Foreground Service keeps the process exempt from Doze.
 */
class PlaybackSessionManager(
    private val playbackRepository: PlaybackRepository,
    private val queueManager: PlaybackQueueService,
    private val settingsRepository: SettingsRepository,
) {
    /** Long-lived scope — cancelled only when [close] is called. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentSession = MutableStateFlow<PlaybackQueueService.QueueSession?>(null)
    val currentSession: StateFlow<PlaybackQueueService.QueueSession?> = _currentSession.asStateFlow()

    private val _playbackState = MutableStateFlow<Playback?>(null)
    val playbackState: StateFlow<Playback?> = _playbackState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _differentPlaybackCount = MutableStateFlow(0)
    val differentPlaybackCount: StateFlow<Int> = _differentPlaybackCount.asStateFlow()

    private val _isSessionAppInitialized = MutableStateFlow(true)

    val playbackPoller = PlaybackPoller(
        playbackRepository = playbackRepository,
        onPlaybackUpdate = { playback ->
            _playbackState.value = playback
            handleQueueTransitions(playback)
        },
        onError = { msg -> _error.value = msg }
    )

    init {
        playbackPoller.start(scope)
    }

    // ── Queue transition logic ────────────────────────────────────────────────

    private suspend fun handleQueueTransitions(playback: Playback?) {
        val session = _currentSession.value

        // Detect if Spotify has drifted to a different album than our session
        if (
            _isSessionAppInitialized.value &&
            session != null &&
            playback?.track?.album?.name != session.album.name &&
            playback?.track?.album?.id != null &&
            playback.track.spotifyUri != session.transitionTrackUri
        ) {
            Napier.d("Not a local session because ${playback.track.album.name} != ${session.album.name}")
            _differentPlaybackCount.value += 1
            if (_differentPlaybackCount.value > 3) {
                _isSessionAppInitialized.value = false
                _currentSession.value = null
            }
        } else {
            _differentPlaybackCount.value = 0
        }

        if (_isSessionAppInitialized.value && session != null) {
            val settings = settingsRepository.settings.first()
            val transitionTime = if (settings.transitionTrack) TRANSITION_TRIGGER_MS else NEXT_ALBUM_TRIGGER_MS

            if (!settings.transitionTrack && queueManager.albumEnding(session, playback, TRANSITION_TRIGGER_MS)) {
                playbackPoller.setPollingDelay(TRANSITIONING_POLLING_DELAY_MS)
            }

            if (queueManager.albumEnding(session, playback, transitionTime)) {
                if (settings.transitionTrack) {
                    addTransitionTrack(session)
                } else {
                    transitionToNextAlbum(session)
                }
            }

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
            onFailure = { e ->
                _error.value = "Failed to add transition track: ${e.message}"
            }
        )
    }

    private suspend fun transitionToNextAlbum(session: PlaybackQueueService.QueueSession) {
        if (session.queue.isNotEmpty()) {
            startAlbum(
                album = session.queue.first(),
                queue = session.queue.drop(1),
                startFromTrack = null,
                collection = session.playingFrom,
                isShuffled = session.isShuffled
            )
            playbackPoller.setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Start playing [album], optionally from [startFromTrack], with an optional [queue]. */
    suspend fun startAlbum(
        album: AlbumDetailUiState,
        queue: List<AlbumDetailUiState> = emptyList(),
        startFromTrack: Track? = null,
        collection: AlbumCollection? = null,
        isShuffled: Boolean = false
    ) {
        val albumWithTracks = queueManager.ensureTracksLoaded(album)

        _currentSession.value = PlaybackQueueService.QueueSession(
            album = albumWithTracks.album,
            lastTrack = albumWithTracks.tracks.last(),
            queue = queue,
            playingFrom = collection,
            isShuffled = isShuffled
        )

        playbackRepository.turnOffShuffle()

        if (startFromTrack != null) {
            if (checkIfResumingCurrentPlayback(startFromTrack)) {
                playbackRepository.resumePlayback()
            } else {
                playbackRepository.startAlbumPlaybackFromTrack(albumWithTracks.album, startFromTrack)
            }
        } else {
            if (checkIfResumingCurrentPlayback(albumWithTracks.album)) {
                playbackRepository.resumePlayback()
            } else {
                playbackRepository.startAlbumPlayback(albumWithTracks.album)
            }
        }

        _isSessionAppInitialized.value = true
        playbackPoller.setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
    }

    fun isLastTrackInAlbum(): Boolean =
        queueManager.isLastTrackInAlbum(currentSession.value, playbackState.value)

    fun clearSession() {
        _currentSession.value = null
    }

    fun setQueue(queue: List<AlbumDetailUiState>, isShuffled: Boolean) {
        _currentSession.value = _currentSession.value?.copy(queue = queue, isShuffled = isShuffled)
    }

    fun clearError() {
        _error.value = null
    }

    fun setError(message: String?) {
        _error.value = message
    }

    fun setPollingDelay(delayMs: Long) {
        playbackPoller.setPollingDelay(delayMs)
    }

    suspend fun refreshPlaybackState(callerName: String = "") {
        _playbackState.value = playbackRepository.getCurrentPlayback(callerName)
    }

    fun close() {
        playbackPoller.stop()
        scope.cancel()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun checkIfResumingCurrentPlayback(track: Track): Boolean {
        if (_playbackState.value?.isPlaying == true) return false
        return _playbackState.value?.track?.id == track.id
    }

    private fun checkIfResumingCurrentPlayback(album: Album): Boolean {
        if (_playbackState.value?.isPlaying == true) return false
        return _playbackState.value?.track?.album?.id == album.id
    }
}

/**
 * Coroutine-based Spotify playback poller.
 * Runs inside [PlaybackSessionManager.scope], which outlives any ViewModel.
 */
class PlaybackPoller(
    private val playbackRepository: PlaybackRepository,
    private val onPlaybackUpdate: suspend (Playback?) -> Unit,
    private val onError: (String?) -> Unit,
    private val pollerName: String = "PlaybackPoller ${Random.nextInt(100)}"
) {
    private var pollingJob: Job? = null
    private val _delayMs = MutableStateFlow(PLAYBACK_ACTIVE_POLLING_DELAY)

    fun start(scope: CoroutineScope) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            Napier.d("Starting polling for $pollerName")
            while (isActive) {
                try {
                    val playback = playbackRepository.getCurrentPlayback(pollerName = pollerName)
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
                delay(_delayMs.value)
            }
            Napier.d("Polling stopped for $pollerName")
        }
    }

    fun setPollingDelay(delayMs: Long) {
        _delayMs.value = delayMs
    }

    fun stop() {
        Napier.d("Stopping polling for $pollerName")
        pollingJob?.cancel()
    }
}



