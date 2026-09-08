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
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val libraryService: LibraryService,
    private val collectionsService: CollectionsService,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : ViewModel() {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _trackSyncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val trackSyncState = _trackSyncState.asStateFlow()

    init {
        // Fetch the Spotify user profile and cache the user ID so all user-scoped
        // repositories (collections, ratings, tags) can resolve their Firestore paths.
        launchSafely("initUserSession") {
            libraryService.initUserSession()
        }
    }

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
        .map { albumDisplayData ->
            albumDisplayData.map { displayData ->
                AlbumDetailUiState(
                    album = displayData.album,
                    tags = emptyList(),
                    collections = emptyList(),
                    tracks = emptyList(),
                    releaseGroup = emptyList(),
                    totalDuration = 0L,
                    rating = displayData.album.rating,
                    isLoading = false,
                    error = null
                )
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

    fun createCollectionFromCurrentFilter(name: String) = launchSafely("createCollectionFromCurrentFilter") {
        collectionsService.createCollectionFromAlbums(filteredAlbums.value.map { it.album }, name)
    }

    fun import() = launchSafely("import") {
        collectionsService.import()
    }

    fun addAlbumToLibrary(album: Album) = launchSafely("addAlbumToLibrary(${album.id})") {
        libraryService.addAlbumToLibrary(album)
    }

    fun removeAlbumFromLibrary(album: Album) = launchSafely("removeAlbumFromLibrary(${album.id})") {
        libraryService.removeAlbumFromLibrary(album)
    }

    fun launchSync(syncAction: SyncAction, removeDuplicates: Boolean) =
        viewModelScope.launch {
            val state = _syncState.value
            if (state !is SyncState.Ready) {
                Napier.d { "Tried to start a sync with $state" }
                return@launch
            }
            _syncState.value = SyncState.Syncing
            _syncState.value = try {
                libraryService.applySync(state.differences, syncAction, removeDuplicates)
                SyncState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("applySync failed", e)
                SyncState.Error(e.message ?: "Sync failed")
            }
        }

    fun startTrackSync() =
        viewModelScope.launch {
            _trackSyncState.value = SyncState.Syncing
            _trackSyncState.value = try {
                libraryService.updateLibraryTracksFromSpotify()
                SyncState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Napier.e("track sync failed", e)
                SyncState.Error(e.message ?: "Track sync failed")
            }
        }

    fun saveTrack(trackId: String) = launchSafely("saveTrack($trackId)") {
        libraryService.saveTrackLocalAndRemote(trackId)
    }

    fun removeTrack(trackId: String) = launchSafely("removeTrack($trackId)") {
        libraryService.removeTrackLocalAndRemote(trackId)
    }

    fun addAllSongsFromAlbumToSavedSongs(album: Album) = launchSafely("addAllSongsFromAlbumToSavedSongs(${album.id})") {
        libraryService.addAllSongsFromAlbumToSavedSongs(album)
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
    val userSavedAlbumsDuplicates: List<Album>,
)