package io.github.peningtonj.recordcollection.service

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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlin.math.abs
import kotlin.random.Random

// Polling delays — defined here since PlaybackSessionManager owns the poller.
// The now-playing bar interpolates progress client-side (PlaybackBar) and the album
// transition is driven by a timer (see below), so the active delay only bounds how fast
// an external play/pause/skip is picked up — 2.5 s is plenty.
const val PLAYBACK_ACTIVE_POLLING_DELAY = 2500L
const val PLAYBACK_INACTIVE_POLLING_DELAY = 8000L

/** Start the next album this long before the current track (or the SFX) actually ends. */
private const val NEXT_ALBUM_LEAD_MS = 300L

/** Re-arm the transition timer only if the estimated fire time moved by more than this. */
private const val RESCHEDULE_TOLERANCE_MS = 1_500L

// Progressive back-off while nothing is playing (TECH_DEBT 2.8): each consecutive
// idle poll steps the delay up to a cap, so an app left open with no playback settles
// to ~1 req/min instead of hammering /me/player at the flat inactive rate forever.
val PLAYBACK_IDLE_BACKOFF_STEPS = longArrayOf(8_000L, 20_000L, 45_000L, 60_000L)

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
    /** Long-lived scope — cancelled only when [close] is called. Injectable for tests. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
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
    //
    // The album (and optional SFX) transition is driven by a *timer*, not by catching a
    // narrow "remaining <= X ms" window on a poll. Spotify only reports progress once per
    // poll, and every poll is a network round-trip (especially on web, where progressMs
    // arrives stale and the odd poll fails outright with no retry), so a poll-based
    // trigger is flaky. Instead: each poll tells us how much of the current track is left
    // and we (re)schedule a one-shot job to fire the transition at the right instant.
    // Later polls just cancel/re-arm the job when the user pauses, seeks, or skips.

    private val transitionMutex = Mutex()
    private var transitionJob: Job? = null
    private var transitionPlanKey: String? = null
    private var transitionPlanFireAtMs: Long = 0L

    private fun nowMs() = Clock.System.now().toEpochMilliseconds()

    internal suspend fun handleQueueTransitions(playback: Playback?) {
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
            planTransition(playback, session)
        } else {
            cancelTransitionPlan()
        }
    }

    /**
     * Given the latest poll, arm / re-arm / cancel the one-shot timer that performs the
     * transition. Idempotent — called on every poll; only re-arms when the situation or
     * the estimated fire time has meaningfully changed.
     */
    private suspend fun planTransition(
        playback: Playback?,
        session: PlaybackQueueService.QueueSession,
    ) = transitionMutex.withLock {
        if (_currentSession.value !== session) return@withLock // superseded while we waited for the lock
        // A null poll (transient network failure / brief gap) must not tear down a pending
        // timer — keep it and let the next good poll re-arm.
        if (playback == null) return@withLock
        if (session.queue.isEmpty()) { clearTimerLocked(); return@withLock }

        val track = playback.track
        val progressMs = playback.progressMs ?: 0L
        val ageMs = (nowMs() - playback.lastUpdated).coerceAtLeast(0L)
        val remainingMs = (track.durationMs - progressMs - ageMs).coerceAtLeast(0L)

        val onSfx = track.spotifyUri == session.transitionTrackUri
        val onAlbumLastTrack = track.id == session.lastTrack.id

        if (session.hasAddedTransitionTrack) {
            when {
                // SFX still sitting in Spotify's queue; the album's last track is finishing.
                onAlbumLastTrack && playback.isPlaying -> clearTimerLocked()
                // SFX is playing — time the jump to the next album to its end.
                onSfx && playback.isPlaying && remainingMs > NEXT_ALBUM_LEAD_MS ->
                    armTimer(remainingMs - NEXT_ALBUM_LEAD_MS, "sfx:${track.id}", session) {
                        performTransitionToNextAlbum(it)
                    }
                // SFX ended / was skipped / playback stopped on it — advance now.
                else -> {
                    Napier.d("SFX done (playing=${playback.isPlaying}, onSfx=$onSfx) — advancing")
                    clearTimerLocked()
                    performTransitionToNextAlbum(session)
                }
            }
            return@withLock
        }

        // Haven't handled this album's end yet.
        if (!onAlbumLastTrack || !playback.isPlaying) { clearTimerLocked(); return@withLock }

        val settings = settingsRepository.settings.first()
        if (settings.transitionTrack) {
            armTimer(remainingMs - TRANSITION_TRIGGER_MS, "queueSfx:${track.id}", session) {
                performAddTransitionTrack(it)
            }
        } else {
            armTimer(remainingMs - NEXT_ALBUM_LEAD_MS, "next:${track.id}", session) {
                performTransitionToNextAlbum(it)
            }
        }
    }

    /** Caller holds [transitionMutex]. Arms [transitionJob] unless an equivalent one is already pending. */
    private fun armTimer(
        fireInMsRaw: Long,
        key: String,
        session: PlaybackQueueService.QueueSession,
        action: suspend (PlaybackQueueService.QueueSession) -> Unit,
    ) {
        val fireInMs = fireInMsRaw.coerceAtLeast(0L)
        val fireAtMs = nowMs() + fireInMs
        if (key == transitionPlanKey && abs(fireAtMs - transitionPlanFireAtMs) < RESCHEDULE_TOLERANCE_MS) {
            return // already armed for this situation, close enough
        }
        transitionPlanKey = key
        transitionPlanFireAtMs = fireAtMs
        transitionJob?.cancel()
        transitionJob = scope.launch {
            delay(fireInMs)
            transitionMutex.withLock {
                if (_currentSession.value !== session) return@withLock // world moved on
                transitionPlanKey = null
                action(session)
            }
        }
    }

    private fun clearTimerLocked() {
        transitionJob?.cancel()
        transitionJob = null
        transitionPlanKey = null
    }

    private suspend fun cancelTransitionPlan() = transitionMutex.withLock { clearTimerLocked() }

    /** Caller holds [transitionMutex]. */
    private suspend fun performAddTransitionTrack(session: PlaybackQueueService.QueueSession) {
        queueManager.addTransitionTrack(session).fold(
            onSuccess = {
                _currentSession.value = session.copy(hasAddedTransitionTrack = true)
                Napier.d("Transition track queued for album: ${session.album.name}")
            },
            onFailure = { e ->
                // Don't stall the session on a failed queue write — hard-cut to the next album.
                Napier.e("Failed to queue transition track — skipping SFX", e)
                _error.value = "Failed to add transition track: ${e.message}"
                performTransitionToNextAlbum(session)
            },
        )
    }

    /** Caller holds [transitionMutex]. */
    private suspend fun performTransitionToNextAlbum(session: PlaybackQueueService.QueueSession) {
        if (_currentSession.value !== session) return
        val next = session.queue.firstOrNull() ?: return
        Napier.d("Transitioning to next album: ${next.album.name}")
        startAlbum(
            album = next,
            queue = session.queue.drop(1),
            startFromTrack = null,
            collection = session.playingFrom,
            isShuffled = session.isShuffled,
        ) // startAlbum() re-polls at the active cadence
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
        playbackPoller.pollNowActive()
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

    /** Drop to active polling and re-poll immediately (see [PlaybackPoller.pollNowActive]). */
    fun pollNowActive() {
        playbackPoller.pollNowActive()
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

    /** Cuts the current inter-poll wait short so a just-issued command is picked up now. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** Consecutive polls that saw nothing playing — drives the idle back-off. */
    private var idlePolls = 0

    /**
     * Set by [pollNowActive]. Holds the *next* poll at the active cadence even if it sees
     * nothing playing yet, so a play/shuffle command that Spotify hasn't registered by the
     * time we poll is still caught ~2.5 s later rather than after an 8 s+ back-off.
     */
    private var forceActiveNextPoll = false

    fun start(scope: CoroutineScope) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            Napier.d("Starting polling for $pollerName")
            while (isActive) {
                try {
                    val playback = playbackRepository.getCurrentPlayback(pollerName = pollerName)
                    onPlaybackUpdate(playback)
                    if (playback == null || !playback.isPlaying) {
                        if (forceActiveNextPoll) {
                            _delayMs.value = PLAYBACK_ACTIVE_POLLING_DELAY
                            forceActiveNextPoll = false
                        } else {
                            val step = minOf(idlePolls, PLAYBACK_IDLE_BACKOFF_STEPS.lastIndex)
                            _delayMs.value = PLAYBACK_IDLE_BACKOFF_STEPS[step]
                            idlePolls++
                        }
                    } else {
                        idlePolls = 0
                        forceActiveNextPoll = false
                        if (_delayMs.value >= PLAYBACK_INACTIVE_POLLING_DELAY) {
                            _delayMs.value = PLAYBACK_ACTIVE_POLLING_DELAY
                        }
                    }
                    onError(null)
                } catch (e: Exception) {
                    Napier.e("Error polling for playback updates ($pollerName): ${e.message}", e)
                    onError(e.message)
                }
                // Sleep until the next poll — but wake early if a transport command fired.
                withTimeoutOrNull(_delayMs.value) { wake.receive() }
            }
            Napier.d("Polling stopped for $pollerName")
        }
    }

    fun setPollingDelay(delayMs: Long) {
        if (delayMs <= PLAYBACK_ACTIVE_POLLING_DELAY) idlePolls = 0
        _delayMs.value = delayMs
    }

    /**
     * Drop to the active cadence and poll now — call after a user transport command
     * (play / pause / shuffle / skip) so it's reflected without waiting out an idle
     * back-off delay (which can be up to 60 s).
     */
    fun pollNowActive() {
        setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
        forceActiveNextPoll = true
        wake.trySend(Unit)
    }

    fun stop() {
        Napier.d("Stopping polling for $pollerName")
        pollingJob?.cancel()
    }
}



