package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import io.github.peningtonj.recordcollection.util.RankedSearchResults
import io.github.peningtonj.recordcollection.util.rankByRelevance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val albumRepository: AlbumRepository,
    private val getAlbumUseCase: GetAlbumDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchScreenUiState>(SearchScreenUiState.LoadingNewReleases)
    val uiState = _uiState.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery = _currentQuery.asStateFlow()

    private val _newReleaseAlbums = MutableStateFlow<List<AlbumDetailUiState>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            updateNewReleaseAlbums()
        }
    }

    suspend fun updateNewReleaseAlbums() {
        _uiState.value = if(_uiState.value is SearchScreenUiState.Idle) { SearchScreenUiState.LoadingNewReleases } else { _uiState.value }
        val newReleases = albumRepository.fetchAllNewReleases()

        val detailedAlbums = supervisorScope {
            newReleases.map { album ->
                async {
                    getAlbumUseCase.execute(album.id, false, album)
                }
            }.awaitAll()
        }

        _newReleaseAlbums.value = detailedAlbums
        _uiState.value = if(_uiState.value is SearchScreenUiState.LoadingNewReleases) { SearchScreenUiState.Idle } else { _uiState.value }
    }


    fun search(query: String) {
        // Cancel previous search if still running
        searchJob?.cancel()

        _currentQuery.value = query

        if (query.isBlank()) {
            _uiState.value = SearchScreenUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            // Add small delay to avoid too many API calls while typing
            delay(300)

            _uiState.value = SearchScreenUiState.Loading

            try {
                val result = searchRepository.searchSpotify(query)
                _uiState.value = SearchScreenUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = SearchScreenUiState.Error(
                    e.message ?: "An unknown error occurred"
                )
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _currentQuery.value = ""
        _uiState.value = SearchScreenUiState.Loading
    }
}

sealed interface SearchScreenUiState {
    data object LoadingNewReleases : SearchScreenUiState
    data object Idle: SearchScreenUiState
    data object Loading : SearchScreenUiState
    data class Error(val message: String) : SearchScreenUiState
    data class Success(
        val result: SearchResult
//        val result: RankedSearchResults
    ) : SearchScreenUiState
}