package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchResult
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.github.peningtonj.recordcollection.util.RankedSearchResults
import io.github.peningtonj.recordcollection.util.rankByRelevance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchScreenUiState>(SearchScreenUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery = _currentQuery.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        // Cancel previous search if still running
        searchJob?.cancel()

        _currentQuery.value = query

        if (query.isBlank()) {
            _uiState.value = SearchScreenUiState.Loading
            return
        }

        searchJob = viewModelScope.launch {
            // Add small delay to avoid too many API calls while typing
            delay(300)

            _uiState.value = SearchScreenUiState.Loading

            try {
                val result = searchRepository.searchSpotify(query)
                _uiState.value = SearchScreenUiState.Success(result.rankByRelevance(query))
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
    data object Loading : SearchScreenUiState
    data class Error(val message: String) : SearchScreenUiState
    data class Success(
        val result: RankedSearchResults
    ) : SearchScreenUiState
}