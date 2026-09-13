package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.util.SearchRankingUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val albumRepository: AlbumRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchScreenUiState>(SearchScreenUiState.LoadingNewReleases)
    val uiState = _uiState.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery = _currentQuery.asStateFlow()

    private val _newReleaseAlbums = MutableStateFlow<List<AlbumDetailUiState>>(emptyList())
    val newReleaseAlbums = _newReleaseAlbums.asStateFlow()

    private var searchJob: Job? = null

    // Fetched once and reused — Spotify's own relevance ordering is more accurate with
    // a market set, and the user's market doesn't change mid-session.
    private var cachedMarket: String? = null

    private suspend fun resolveMarket(): String? {
        cachedMarket?.let { return it }
        return profileRepository.getCurrentUserProfile()?.country?.also { cachedMarket = it }
    }

    init {
        launchSafely(
            operation = "updateNewReleaseAlbums",
            onError = { _uiState.value = SearchScreenUiState.Error(it.message ?: "Failed to load new releases") },
        ) {
            updateNewReleaseAlbums()
        }
    }

    suspend fun updateNewReleaseAlbums(forceRefresh: Boolean = false) {
        _uiState.value = if (_uiState.value is SearchScreenUiState.Idle) SearchScreenUiState.LoadingNewReleases else _uiState.value

        val newReleases = albumRepository.fetchAllNewReleases(forceRefresh)

        if (newReleases.isEmpty()) {
            _newReleaseAlbums.value = emptyList()
            _uiState.value = if (_uiState.value is SearchScreenUiState.LoadingNewReleases) SearchScreenUiState.Idle else _uiState.value
            return
        }

        // Single batched Firestore query: find which of these albums the user already has
        // (gives us inLibrary status, rating, etc. without touching Spotify at all).
        val albumIds = newReleases.map { it.id }
        val firestoreAlbums = albumRepository.getAlbumsByIds(albumIds).first()
            .associateBy { it.id }

        _newReleaseAlbums.value = newReleases.map { album ->
            val knownAlbum = firestoreAlbums[album.id] ?: album  // prefer Firestore data if available
            AlbumDetailUiState(
                album = knownAlbum,
                tags = emptyList(),
                collections = emptyList(),
                tracks = emptyList(),
                totalDuration = 0L,
                rating = knownAlbum.rating,
                isLoading = false,
                error = null,
                releaseGroup = emptyList()
            )
        }

        _uiState.value = if (_uiState.value is SearchScreenUiState.LoadingNewReleases) SearchScreenUiState.Idle else _uiState.value
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
                val market = resolveMarket()
                val rawResult = searchRepository.searchSpotify(query, market = market)

                val libraryAlbums = albumRepository.getAllAlbumsInLibrary().first()
                val libraryAlbumIds = libraryAlbums.mapNotNull { it.spotifyId.takeIf(String::isNotEmpty) }.toSet()
                val libraryArtistIds = libraryAlbums.flatMap { it.artists.map { artist -> artist.id } }.toSet()

                val refined = SearchRankingUtils.refineSearchResults(
                    results = rawResult,
                    query = query,
                    libraryAlbumSpotifyIds = libraryAlbumIds,
                    libraryArtistIds = libraryArtistIds,
                )
                _uiState.value = SearchScreenUiState.Success(refined)
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
        _uiState.value = SearchScreenUiState.Idle
    }
}

sealed interface SearchScreenUiState {
    data object LoadingNewReleases : SearchScreenUiState
    data object Idle: SearchScreenUiState
    data object Loading : SearchScreenUiState
    data class Error(val message: String) : SearchScreenUiState
    data class Success(
        val result: SearchResult
    ) : SearchScreenUiState
}