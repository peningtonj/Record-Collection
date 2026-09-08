package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class CollectionDetailViewModel(
    private val collectionRepository: AlbumCollectionRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val collectionName: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(CollectionDetailUiState())
    val uiState: StateFlow<CollectionDetailUiState> = _uiState.asStateFlow()

    private val showError: (Throwable) -> Unit = { e ->
        _uiState.value = _uiState.value.copy(error = e.message)
    }

    init {
        loadCollectionDetails()
    }

    private fun loadCollectionDetails() = launchSafely(
        operation = "loadCollectionDetails($collectionName)",
        onError = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) },
    ) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                collectionRepository.getCollectionByName(collectionName),
                collectionAlbumRepository.getAlbumsInCollection(collectionName),
            ) { collection, albums ->
                collection to albums
            }.catch { e ->
                LoggingUtils.e(LoggingUtils.Category.VIEWMODEL, "Failed to load collection '$collectionName'", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }.collect { (collection, albums) ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    collection = collection,
                    // Wrap each CollectionAlbum in a lightweight AlbumDetailUiState.
                    // Tracks, tags and release-group data are loaded lazily by the Album
                    // Detail screen; the collection grid only needs the album itself.
                    albums = albums.map { collectionAlbum ->
                        AlbumDetailUiState(
                            album = collectionAlbum.album,
                            tags = emptyList(),
                            collections = emptyList(),
                            tracks = emptyList(),
                            releaseGroup = emptyList(),
                            totalDuration = 0L,
                            rating = collectionAlbum.album.rating
                        )
                    }
                )
            }
    }

    fun addAlbumToCollection(album: io.github.peningtonj.recordcollection.db.domain.Album) =
        launchSafely("addAlbumToCollection(${album.id})", showError) {
            collectionAlbumRepository.addAlbumToCollection(collectionName, album)
        }

    fun removeAlbumFromCollection(albumId: String) =
        launchSafely("removeAlbumFromCollection($albumId)", showError) {
            collectionAlbumRepository.removeAlbumFromCollection(collectionName, albumId)
        }

    fun reorderAlbums(albumPositions: List<Pair<String, Int>>) =
        launchSafely("reorderAlbums($collectionName)", showError) {
            collectionAlbumRepository.reorderAlbums(collectionName, albumPositions)
        }

    fun updateCollection(existingName: String, newCollectionDetails: AlbumCollection) =
        launchSafely("updateCollection($existingName)", showError) {
            collectionRepository.updateCollectionByName(newCollectionDetails, existingName)
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