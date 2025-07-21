package io.github.peningtonj.recordcollection.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class CollectionDetailViewModel(
    private val collectionRepository: AlbumCollectionRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    private val collectionName: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()
    
    init {
        loadCollectionDetails()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadCollectionDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                collectionRepository.getCollectionByName(collectionName),
                collectionAlbumRepository.getAlbumsInCollection(collectionName)
            ) { collection, albums ->
                collection to albums
            }.collect { (collection, albums) ->
                val albumDetails = if (albums.isEmpty()) {
                    emptyList()
                } else {
                    supervisorScope {
                        albums.map { album ->
                            async {
                                getAlbumDetailUseCase.execute(album.album.id)
                            }
                        }.awaitAll()
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    collection = collection,
                    albums = albumDetails
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
    
    fun updateCollection(
        existingName: String,
        newCollectionDetails: AlbumCollection) {
        viewModelScope.launch {
            try {
                collectionRepository.updateCollectionByName(
                    newCollectionDetails, existingName)
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
    val albums: List<AlbumDetailUiState> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)