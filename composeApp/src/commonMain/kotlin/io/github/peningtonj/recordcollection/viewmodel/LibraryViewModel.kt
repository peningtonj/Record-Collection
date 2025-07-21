package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.service.CollectionsService
import io.github.peningtonj.recordcollection.service.LibraryService
import io.github.peningtonj.recordcollection.service.LibraryStats
import io.github.peningtonj.recordcollection.service.SyncAction
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import jdk.jfr.internal.OldObjectSample.emit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class LibraryViewModel(
    private val libraryService: LibraryService,
    private val collectionsService: CollectionsService,
    private val getAlbumDetailUseCase: GetAlbumDetailUseCase,
    albumRepository: AlbumRepository,
    artistRepository: ArtistRepository,
) : ViewModel() {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    // Load saved filter state on initialization
    private val _currentFilter = MutableStateFlow(loadSavedFilter())
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredAlbums: StateFlow<List<AlbumDetailUiState>> = currentFilter
        .flatMapLatest { filter ->
            libraryService.getFilteredAlbums(filter)
        }
        .flatMapLatest { albumDisplayData ->
            flow {
                if (albumDisplayData.isEmpty()) {
                    emit(emptyList())
                } else {
                    val albumDetails = coroutineScope {
                        albumDisplayData.map { displayData ->
                            async {
                                getAlbumDetailUseCase.execute(displayData.album.id)
                            }
                        }.awaitAll()
                    }
                    emit(albumDetails)
                }
            }
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
        saveFilter(filter)
    }

    fun updateFilterPartial(update: (AlbumFilter) -> AlbumFilter) {
        val newFilter = update(_currentFilter.value)
        _currentFilter.value = newFilter
        saveFilter(newFilter)
    }

    // Sync operations
    fun startSync() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                val differences = libraryService.getLibraryDifferences()
                _syncState.value = SyncState.Ready(
                    differences
                )
            } catch (e: Exception) {
                Napier.e("Library sync failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Sync failed")
            }
        }
    }

    private fun saveFilter(filter: AlbumFilter) {
        // Save to persistent storage
        FilterPreferences.saveFilter(filter)
    }

    private fun loadSavedFilter(): AlbumFilter {
        // Load from persistent storage
        return FilterPreferences.loadFilter()
    }

    fun createCollectionFromCurrentFilter(name: String) =
        collectionsService.createCollectionFromAlbums(filteredAlbums.value.map { it.album }, name)

    fun import() =
        viewModelScope.launch {
            collectionsService.import()
        }

    fun addAlbumToLibrary(album: Album) =
        libraryService.addAlbumToLibrary(album)


    fun removeAlbumFromLibrary(album: Album) =
        libraryService.removeAlbumFromLibrary(album)

    fun launchSync(syncAction: SyncAction, removeDuplicates: Boolean) =
        viewModelScope.launch {
            if (_syncState.value is SyncState.Ready) {
                val differences = (_syncState.value as SyncState.Ready).differences
                _syncState.value = SyncState.Syncing
                libraryService.applySync(differences, syncAction, removeDuplicates)
                _syncState.value = SyncState.Idle
            } else {
                Napier.d { "Tried to start a sync with ${_syncState.value}" }
            }
        }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Ready(
        val differences: LibraryDifferences,
        val removeDuplicates: Boolean = true
    ): SyncState()
    data class Error(val message: String) : SyncState()
}

data class LibraryDifferences(
    val localCount: Int,
    val spotifyCount: Int,
    val onlyInLocal: Int,
    val onlyInSpotify: Int,
    val inBoth: Int,
    val userSavedAlbums: List<Album>,
    val localLibrary: List<Album>,
    val localDuplicates: List<Album>,
    val userSavedAlbumsDuplicates: List<Album>
)
