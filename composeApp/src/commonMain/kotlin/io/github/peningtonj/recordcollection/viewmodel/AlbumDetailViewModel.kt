package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumDetailViewModel(
    private val albumId: String,
    private val spotifyId: String,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val trackRepository: TrackRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow<AlbumScreenUiState>(AlbumScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadAlbum() {
        viewModelScope.launch {
            _uiState.value = AlbumScreenUiState.Loading
            try {
                trackRepository.checkAndUpdateTracksIfNeeded(albumId, spotifyId)
                getAlbumDetailUseCase.execute(albumId, spotifyId).collect { albumDetail ->
                    _uiState.value = AlbumScreenUiState.Success(albumDetail)
                }
            } catch (e: Exception) {
                _uiState.value = AlbumScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}