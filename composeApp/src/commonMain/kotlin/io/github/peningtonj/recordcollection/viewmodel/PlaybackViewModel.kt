package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackViewModel(
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    init {
        startPlaybackPolling()
    }

    private val _playbackState = MutableStateFlow<Playback?>(null)
    val playbackState: StateFlow<Playback?> = _playbackState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var pollingJob: Job? = null

    fun startPlaybackPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val playback = playbackRepository.getCurrentPlayback()
                    _playbackState.value = playback
                    _error.value = null // Clear any previous errors
                } catch (e: Exception) {
                    _error.value = e.message
                    // Handle error - maybe stop polling if unauthorized
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun stopPlaybackPolling() {
        pollingJob?.cancel()
    }

    // Existing album playback methods
    fun playAlbum(album: Album) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                playbackRepository.turnOffShuffle()
                playbackRepository.startAlbumPlayback(album)
                // Update state immediately after starting playback
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playTrackFromAlbum(album: Album, track: Track) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                playbackRepository.turnOffShuffle()
                playbackRepository.startAlbumPlaybackFromTrack(album, track)
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // New playback control methods
    fun play() {
        viewModelScope.launch {
            try {
                playbackRepository.resumePlayback()
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun pause() {
        viewModelScope.launch {
            try {
                playbackRepository.pausePlayback()
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            try {
                val currentState = _playbackState.value
                if (currentState?.isPlaying == true) {
                    playbackRepository.pausePlayback()
                } else {
                    playbackRepository.resumePlayback()
                }
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun next() {
        viewModelScope.launch {
            try {
                playbackRepository.skipToNext()
                delay(100) // Small delay to ensure state is updated on Spotify's side
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun previous() {
        viewModelScope.launch {
            try {
                playbackRepository.skipToPrevious()
                delay(100)
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun seek(positionMs: Long) {
        viewModelScope.launch {
            try {
                playbackRepository.seekToPosition(positionMs)
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun setShuffle(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    playbackRepository.turnOnShuffle()
                } else {
                    playbackRepository.turnOffShuffle()
                }
                updatePlaybackState()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private suspend fun updatePlaybackState() {
        try {
            val playback = playbackRepository.getCurrentPlayback()
            _playbackState.value = playback
        } catch (e: Exception) {
            _error.value = e.message
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPlaybackPolling()
    }
}