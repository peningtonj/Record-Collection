package io.github.peningtonj.recordcollection.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbum
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.ui.models.CollectionAlbumDisplayData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CollectionDetailViewModel(
    private val collectionRepository: AlbumCollectionRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val ratingRepository: RatingRepository,
    private val collectionName: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadCollectionDetails()
    }


    private fun loadCollectionDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            combine(
                collectionRepository.getCollectionByName(collectionName),
                collectionAlbumRepository.getAlbumsInCollection(collectionName),
                ratingRepository.getAllRatings()
            ) { collection, albums, ratings -> // 'rating' is a List<Rating> here
                val ratingsMap = ratings.associate { it.albumId to it.rating }

                Pair(
                    collection,
                    albums.map { album ->
                        CollectionAlbumDisplayData(
                            album,
                            ratingsMap[album.album.id] ?: 0
                        )
                    }
                )
            }
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { (collection, albums) ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        collection = collection,
                        albums = albums,
                        error = null
                    )
                }
        }
    }
    
    fun addAlbumToCollection(albumId: String) {
        viewModelScope.launch {
            try {
                collectionAlbumRepository.addAlbumToCollection(collectionName, albumId)
                // Albums will automatically update via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun removeAlbumFromCollection(albumId: String) {
        viewModelScope.launch {
            try {
                collectionAlbumRepository.removeAlbumFromCollection(collectionName, albumId)
                // Albums will automatically update via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun reorderAlbums(albumPositions: List<Pair<String, Int>>) {
        viewModelScope.launch {
            try {
                collectionAlbumRepository.reorderAlbums(collectionName, albumPositions)
                // Albums will automatically update via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun updateCollection(name: String, description: String? = null) {
        viewModelScope.launch {
            try {
                collectionRepository.updateCollection(collectionName, name, description)
                // Collection will automatically update via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CollectionDetailUiState(
    val collection: AlbumCollection? = null,
    val albums: List<CollectionAlbumDisplayData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)