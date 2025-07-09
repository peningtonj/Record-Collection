package io.github.peningtonj.recordcollection.ui.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CollectionsViewModel(
    private val repository: AlbumCollectionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CollectionsUiState())
    val uiState: StateFlow<CollectionsUiState> = _uiState.asStateFlow()
    
    init {
        loadCollections()
    }
    
    private fun loadCollections() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.getAllCollections()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { collections ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        collections = collections,
                        error = null
                    )
                }
        }
    }
    
    fun createCollection(name: String, description: String? = null) {
        viewModelScope.launch {
            try {
                repository.createCollection(name, description)
                // Collections will automatically update via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun deleteCollection(name: String) {
        viewModelScope.launch {
            try {
                repository.deleteCollection(name)
                // Collections will automatically update via Flow
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CollectionsUiState(
    val collections: List<AlbumCollection> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)