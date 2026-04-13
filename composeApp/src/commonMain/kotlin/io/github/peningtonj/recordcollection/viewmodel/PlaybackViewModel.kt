package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.service.PLAYBACK_ACTIVE_POLLING_DELAY
import io.github.peningtonj.recordcollection.service.PlaybackSessionManager
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Thin UI wrapper around [PlaybackSessionManager].
 *
 * All session state and polling logic live in [sessionManager] which is
 * process-scoped (held by the DI container). This ViewModel is responsible
 * only for UI-triggered commands and the loading indicator.
 */
class PlaybackViewModel(
    private val playbackRepository: PlaybackRepository,
    val sessionManager: PlaybackSessionManager,
) : ViewModel() {

    // Delegate state reads to the session manager
    val currentSession = sessionManager.currentSession
    val playbackState = sessionManager.playbackState
    val error = sessionManager.error
    val differentPlaybackCount = sessionManager.differentPlaybackCount

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ── Album / queue commands ────────────────────────────────────────────────

    fun playAlbum(
        album: AlbumDetailUiState,
        queue: List<AlbumDetailUiState> = emptyList(),
        startFromTrack: Track? = null,
        collection: AlbumCollection? = null,
        isShuffled: Boolean = false
    ) {
        viewModelScope.launch {
            executeWithLoading {
                sessionManager.startAlbum(album, queue, startFromTrack, collection, isShuffled)
                sessionManager.refreshPlaybackState("After playAlbum")
            }
        }
    }

    fun toggleShuffle(albums: List<AlbumDetailUiState>) {
        val currentAlbum = sessionManager.playbackState.value?.track?.album ?: return
        Napier.d { "Currently playing: ${currentAlbum.name}" }
        if (sessionManager.currentSession.value?.isShuffled == true) {
            Napier.d { "Switching shuffle off" }
            val startIndex = albums.indexOfFirst { it.album.id == currentAlbum.id }
            val queue = albums.drop(startIndex + 1)
            Napier.d { "Next album will be ${queue.firstOrNull()?.album?.name}" }
            sessionManager.setQueue(queue, false)
        } else {
            Napier.d { "Switching shuffle on" }
            val queue = albums.filter { it.album.id != currentAlbum.id }.shuffled()
            Napier.d { "Next album will be ${queue.firstOrNull()?.album?.name}" }
            sessionManager.setQueue(queue, true)
        }
    }

    fun setQueue(queue: List<AlbumDetailUiState>, isShuffled: Boolean) {
        sessionManager.setQueue(queue, isShuffled)
    }

    fun skipToNextAlbumInQueue() {
        viewModelScope.launch {
            executeWithLoading {
                val session = sessionManager.currentSession.value ?: return@executeWithLoading
                val nextAlbum = session.queue.firstOrNull() ?: return@executeWithLoading
                sessionManager.startAlbum(
                    album = nextAlbum,
                    queue = session.queue.drop(1),
                    collection = session.playingFrom,
                    isShuffled = session.isShuffled
                )
            }
        }
    }

    // ── Playback transport commands ───────────────────────────────────────────

    fun togglePlayPause() = viewModelScope.launch {
        executePlaybackAction {
            val isPlaying = sessionManager.playbackState.value?.isPlaying == true
            if (isPlaying) {
                playbackRepository.pausePlayback()
            } else {
                playbackRepository.resumePlayback()
                sessionManager.setPollingDelay(PLAYBACK_ACTIVE_POLLING_DELAY)
            }
        }
    }

    fun next() {
        Napier.d("Next called")
        if (sessionManager.isLastTrackInAlbum()) {
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
                sessionManager.clearSession()
            } else {
                playbackRepository.turnOffShuffle()
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    fun clearError() = sessionManager.clearError()
    fun clearSession() = sessionManager.clearSession()

    private suspend fun executeWithLoading(action: suspend () -> Unit) {
        _isLoading.value = true
        try {
            action()
        } catch (e: Exception) {
            sessionManager.setError(e.message)
        } finally {
            _isLoading.value = false
        }
    }

    private suspend fun executePlaybackAction(action: suspend () -> Unit) {
        try {
            action()
            sessionManager.refreshPlaybackState("From ViewModel")
        } catch (e: Exception) {
            sessionManager.setError(e.message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Polling continues in PlaybackSessionManager — do NOT stop it here.
    }
}
