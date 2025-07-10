package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackViewModel(
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    private val _playbackState = MutableStateFlow<Playback?>(null)
    val playbackState: StateFlow<Playback?> = _playbackState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: Job? = null

    fun startPlaybackPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val playback = playbackRepository.getCurrentPlayback()
                    _playbackState.value = playback
                } catch (e: Exception) {
                    // Handle error - maybe stop polling if unauthorized
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    fun stopPlaybackPolling() {
        pollingJob?.cancel()
    }

    fun playAlbum(album: Album) {
        viewModelScope.launch {
            playbackRepository.turnOffShuffle()
            playbackRepository.startAlbumPlayback(album)
        }
    }

    fun playTrackFromAlbum(album: Album, track: Track) {
        viewModelScope.launch {
            playbackRepository.turnOffShuffle()
            playbackRepository.startAlbumPlaybackFromTrack(album, track)
        }
    }

}
