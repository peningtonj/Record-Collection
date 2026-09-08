package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlbumDetailViewModel(
    private val albumId: String,
    private val spotifyId: String,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
): ViewModel() {
    private val _uiState = MutableStateFlow<AlbumScreenUiState>(AlbumScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    override fun onCleared() {
        Napier.d("AlbumDetailViewModel($albumId) cleared")
        super.onCleared()
    }

    fun loadAlbum() = launchSafely(
        operation = "loadAlbum($albumId)",
        onError = { _uiState.value = AlbumScreenUiState.Error(it.message ?: "Unknown error") },
    ) {
        _uiState.value = AlbumScreenUiState.Loading
        getAlbumDetailUseCase.execute(albumId, spotifyId).collect { albumDetail ->
            _uiState.value = AlbumScreenUiState.Success(albumDetail)
        }
    }

    /** Optimistically flip a track's saved heart while the Spotify write is in flight. */
    fun setTrackSaved(trackId: String, saved: Boolean) {
        val current = _uiState.value as? AlbumScreenUiState.Success ?: return
        _uiState.value = AlbumScreenUiState.Success(
            current.albumDetail.copy(
                tracks = current.albumDetail.tracks.map {
                    if (it.id == trackId) it.copy(isSaved = saved) else it
                }
            )
        )
    }
}