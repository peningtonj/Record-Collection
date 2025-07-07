package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.service.LibraryService
import io.github.peningtonj.recordcollection.service.LibraryStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest

class LibraryViewModel(
    private val libraryService: LibraryService,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _currentFilter = MutableStateFlow(AlbumFilter())
    val currentFilter = _currentFilter.asStateFlow()

    // Basic library data
    val earliestReleaseDate = albumRepository.getEarliestReleaseDate()

    val allArtists: StateFlow<List<String>> = albumRepository.getAllArtists()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val allGenres: StateFlow<List<String>> = artistRepository.getAllGenres()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // In LibraryViewModel
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredAlbums: StateFlow<List<Album>> = currentFilter
        .flatMapLatest { filter ->
            libraryService.getFilteredAlbums(filter)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // Library statistics
    val libraryStats: StateFlow<LibraryStats> = libraryService
        .getLibraryStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            LibraryStats(0, 0, emptyMap(), emptyMap())
        )

    // Filter management
    fun updateFilter(filter: AlbumFilter) {
        Napier.d { "Updating Filter from ${_currentFilter.value} to $filter" }
        _currentFilter.value = filter
    }

    fun updateFilterPartial(update: (AlbumFilter) -> AlbumFilter) {
        _currentFilter.value = update(_currentFilter.value)
    }

    // Sync operations
    fun syncLibrary() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                libraryService.syncLibraryData()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                Napier.e("Library sync failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    object Success : SyncState()
    data class Error(val message: String) : SyncState()
}