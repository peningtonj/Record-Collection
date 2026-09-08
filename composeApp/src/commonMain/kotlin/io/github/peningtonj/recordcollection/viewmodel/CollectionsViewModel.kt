package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionFolder
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

class CollectionsViewModel(
    private val repository: AlbumCollectionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()

    private val _currentFolder = MutableStateFlow<String?>(null)
    val currentFolder: StateFlow<String?> = _currentFolder.asStateFlow()

    private val showError: (Throwable) -> Unit = { e ->
        _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
    }

    init {
        loadTopLevelItems()
    }

    private fun loadTopLevelItems() = launchSafely("loadTopLevelItems", showError) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        combine(
            repository.getAllTopLevelCollections(),
            repository.getAllTopLevelFolders()
        ) { collections, folders ->
            _uiState.value = _uiState.value.copy(
                isLoading = false, collections = collections, folders = folders, error = null
            )
        }.catch { error ->
            _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
        }.collect { }
    }

    fun navigateToFolder(folderName: String) {
        _currentFolder.value = folderName
        loadFolderContents(folderName)
    }

    fun navigateBack() {
        _currentFolder.value = null
        loadTopLevelItems()
    }

    fun updateCollectionParent(collection: AlbumCollection, newParentName: String?) =
        launchSafely("updateCollectionParent(${collection.name})", showError) {
            repository.updateCollectionByName(collection.copy(parentName = newParentName), collection.name)
        }

    fun updateCollection(existingName: String, newCollectionDetails: AlbumCollection) =
        launchSafely("updateCollection($existingName)", showError) {
            repository.updateCollectionByName(newCollectionDetails, existingName)
        }

    private fun loadFolderContents(folderName: String) = launchSafely("loadFolderContents($folderName)", showError) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        combine(
            repository.getCollectionsByFolder(folderName),
            repository.getFoldersByParent(folderName)
        ) { collections, folders ->
            _uiState.value = _uiState.value.copy(
                isLoading = false, collections = collections, folders = folders, error = null
            )
        }.catch { error ->
            _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
        }.collect { }
    }

    private fun reloadCurrentView() {
        _currentFolder.value?.let { loadFolderContents(it) } ?: loadTopLevelItems()
    }

    fun createCollection(name: String, description: String? = null) =
        launchSafely("createCollection($name)", showError) {
            repository.createCollection(name, description, _currentFolder.value)
            reloadCurrentView()
        }

    fun createTopLevelFolder(name: String) = launchSafely("createTopLevelFolder($name)", showError) {
        repository.createFolder(
            CollectionFolder(folderName = name, collections = emptyList(), folders = emptyList(), parentName = null)
        )
        reloadCurrentView()
    }

    fun deleteCollection(name: String) = launchSafely("deleteCollection($name)", showError) {
        repository.deleteCollection(name)
        reloadCurrentView()
    }
}

data class CollectionsUiState(
    val collections: List<AlbumCollection> = emptyList(),
    val folders: List<CollectionFolder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
