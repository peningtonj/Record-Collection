package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionFolder
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CollectionsViewModel(
    private val repository: AlbumCollectionRepository,
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()
    
    private val _currentFolder = MutableStateFlow<String?>(null)
    val currentFolder: StateFlow<String?> = _currentFolder.asStateFlow()


    init {
        loadTopLevelItems()
    }

    private fun loadTopLevelItems() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            combine(
                repository.getAllTopLevelCollections(),
                repository.getAllTopLevelFolders()
            ) { collections, folders ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    collections = collections,
                    folders = folders,
                    error = null
                )
            }.catch { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }.collect { }
        }
    }
    
    fun navigateToFolder(folderName: String) {
        _currentFolder.value = folderName
        loadFolderContents(folderName)
    }
    
    fun navigateBack() {
        _currentFolder.value = null
        loadTopLevelItems()
    }

    fun updateCollectionParent(
        collection: AlbumCollection,
        newParentName: String?
    ) = repository
        .updateCollectionByName(
            collection.copy(parentName = newParentName),
            collection.name
        )

    fun updateCollection(
        existingName: String,
        newCollectionDetails: AlbumCollection) =
    repository.updateCollectionByName(newCollectionDetails, existingName)

    private fun loadFolderContents(folderName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            combine(
                repository.getCollectionsByFolder(folderName),
                repository.getFoldersByParent(folderName)
            ) { collections, folders ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    collections = collections,
                    folders = folders,
                    error = null
                )
            }.catch { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }.collect { }
        }
    }
    
    fun createCollection(name: String, description: String? = null) {
        viewModelScope.launch {
            try {
                repository.createCollection(name, description, _currentFolder.value)
                // Reload current view
                if (_currentFolder.value != null) {
                    loadFolderContents(_currentFolder.value!!)
                } else {
                    loadTopLevelItems()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun createTopLevelFolder(name: String) {
        viewModelScope.launch {
            try {
                repository.createFolder(
                    CollectionFolder(
                        folderName = name,
                        collections = emptyList(),
                        folders = emptyList(),
                        parentName = null
                    )
                )
                // Reload current view
                if (_currentFolder.value != null) {
                    loadFolderContents(_currentFolder.value!!)
                } else {
                    loadTopLevelItems()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteCollection(name: String) {
        viewModelScope.launch {
            try {
                repository.deleteCollection(name)
                // Reload current view
                if (_currentFolder.value != null) {
                    loadFolderContents(_currentFolder.value!!)
                } else {
                    loadTopLevelItems()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}

data class CollectionsUiState(
    val collections: List<AlbumCollection> = emptyList(),
    val folders: List<CollectionFolder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

