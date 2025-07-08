package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

class AlbumScreenViewModel(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumScreenUiState>(AlbumScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _uiState.value = AlbumScreenUiState.Loading
            try {
                albumRepository.checkAndUpdateTracksIfNeeded(albumId)

                combine(
                    albumRepository.getAlbumById(albumId),
                    albumRepository.getTracksForAlbum(albumId)
                ) { album, tracks ->
                    AlbumScreenUiState.Success(
                        album = album,
                        tracks = tracks
                    )
                }.catch { e ->
                    _uiState.value = AlbumScreenUiState.Error(e.message ?: "Unknown error")
                }.collect { successState ->
                    _uiState.value = successState
                }
            } catch (e: Exception) {
                _uiState.value = AlbumScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface AlbumScreenUiState {
    data object Loading : AlbumScreenUiState
    data class Error(val message: String) : AlbumScreenUiState
    data class Success(
        val album: Album,
        val tracks: List<Track>
    ) : AlbumScreenUiState
}