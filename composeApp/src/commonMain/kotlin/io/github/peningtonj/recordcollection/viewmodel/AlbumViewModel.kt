package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.catch

class AlbumViewModel (
    private val albumRepository: AlbumRepository,
    private val ratingRepository: RatingRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val tagRepository: TagRepository,
    private val albumTagRepository: AlbumTagRepository
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

                getAlbumDetailUseCase.execute(albumId)
                    .catch { e ->
                        _uiState.value = AlbumScreenUiState.Error(e.message ?: "Unknown error")
                    }
                    .collect { albumDetailState ->
                        _uiState.value = AlbumScreenUiState.Success(albumDetailState)
                    }
            } catch (e: Exception) {
                _uiState.value = AlbumScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun addAlbumToCollection(album: Album, collectionName: String) = viewModelScope.launch {
        collectionAlbumRepository.addAlbumToCollection(collectionName, album.id)
    }

    fun removeAlbumFromCollection(album: Album, collectionName: String) = viewModelScope.launch {
        collectionAlbumRepository.removeAlbumFromCollection(collectionName, album.id)
    }

    fun refreshAlbum(album: Album) = viewModelScope.launch {
        albumRepository.fetchAlbum(album.id, true)
    }

    fun removeTagFromAlbum(albumId: String, tagId: String) = viewModelScope.launch {
        try {
            albumTagRepository.removeTagFromAlbum(albumId, tagId)
            loadAlbum(albumId) // Refresh the album data
            Napier.d { "Removed tag $tagId from album $albumId" }
        } catch (e: Exception) {
            Napier.e(e) { "Error removing tag from album" }
        }
    }

    fun addTagToAlbum(albumId: String, tagKey: String, tagValue: String) = viewModelScope.launch {
        try {
            val newTag = Tag(
                key = tagKey,
                value = tagValue,
                type = TagType.USER // Adjust based on your TagType enum
            )

            // Insert the tag first
            tagRepository.insertTag(newTag)

            // Then add it to the album
            albumTagRepository.addTagToAlbum(albumId, newTag.id)

            // Refresh the album data
            loadAlbum(albumId)

            Napier.d { "Added tag ${newTag.key}:${newTag.value} to album $albumId" }
        } catch (e: Exception) {
            Napier.e(e) { "Error adding tag to album" }
        }
    }
}

sealed interface AlbumScreenUiState {
    data object Loading : AlbumScreenUiState
    data class Error(val message: String) : AlbumScreenUiState
    data class Success(
        val albumDetail: AlbumDetailUiState
    ) : AlbumScreenUiState
}