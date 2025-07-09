package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch

class AlbumViewModel (
    private val albumRepository: AlbumRepository,
    private val ratingRepository: RatingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumScreenUiState>(AlbumScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun getRating(albumId: String) =
        ratingRepository.getAlbumRating(albumId)

    fun setRating(albumId: String, rating: Int) =
        ratingRepository.addRating(albumId, rating.toLong())
    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _uiState.value = AlbumScreenUiState.Loading
            try {
                albumRepository.checkAndUpdateTracksIfNeeded(albumId)

                combine(
                    albumRepository.getAlbumById(albumId),
                    albumRepository.getTracksForAlbum(albumId),
                    ratingRepository.getAlbumRating(albumId)
                ) { album, tracks, rating ->
                    AlbumScreenUiState.Success(
                        album = AlbumDisplayData(
                            album,
                            tracks.sumOf { it.durationMs },
                            rating = rating?.rating ?: 0
                        ),
                        tracks = tracks,
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
        val album: AlbumDisplayData,
        val tracks: List<Track>,
    ) : AlbumScreenUiState
}