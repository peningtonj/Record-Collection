// commonMain/ui/screens/LibraryScreen.kt
package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.ui.components.library.CreateCollectionButton
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumActions
import io.github.peningtonj.recordcollection.ui.components.filter.ActiveChips
import io.github.peningtonj.recordcollection.ui.components.filter.HorizontalFilterBar
import io.github.peningtonj.recordcollection.ui.components.filter.TextSearchBar
import io.github.peningtonj.recordcollection.ui.components.library.SyncLibraryUi
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.LibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SyncState
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberLibraryViewModel
import kotlinx.datetime.LocalDate

@Composable
fun LibraryScreen(
    playbackViewModel: PlaybackViewModel,
    viewModel: LibraryViewModel = rememberLibraryViewModel(),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel()
) {
    val syncState by viewModel.syncState.collectAsState()
    val filteredAlbums by viewModel.filteredAlbums.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val genres by viewModel.allGenres.collectAsState()
    val earliestReleaseDate by viewModel.earliestReleaseDate.collectAsState(LocalDate(1950, 1, 1))

    val navigator = LocalNavigator.current

    val albumTileActions = rememberAlbumActions(
        playbackViewModel,
        albumViewModel,
        viewModel,
        collectionsViewModel,
        navigator,
    )

    val filterOptions by remember(artists, genres) {
        derivedStateOf {
            mutableMapOf(
                "Artist" to artists.toMutableList(),
                "Genre" to genres.toMutableList()
            )
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SyncLibraryUi(
                onClick = {
                    viewModel.startSync()
                },
                syncState = syncState,
                launchSync = { syncAction, removeDuplicates ->
                    viewModel.launchSync(syncAction, removeDuplicates)
                }
            )
        }

        TextSearchBar(
            currentFilter,
            options = filterOptions,
            onFilterChange = { newFilter ->
                viewModel.updateFilter(newFilter)
            },
        )

        HorizontalFilterBar(
            currentFilter = currentFilter,
            onRatingChange = { newRating ->
                    viewModel.updateFilter(currentFilter.copy(minRating = newRating))
                },
            onYearFilterChange = { start, end, label ->
                    val newYearRange = DateRange(
                        LocalDate(start, 1, 1),
                        LocalDate(end, 12, 31),
                        name = label
                    )
                    viewModel.updateFilter(currentFilter.copy(releaseDateRange = newYearRange))
                },
            startYear = earliestReleaseDate?.year ?: 1950
        )

        if (currentFilter.tags.isNotEmpty() || currentFilter.releaseDateRange != null) {
            ActiveChips(
                currentFilter = currentFilter,
                onFilterChange = { newFilter ->
                    viewModel.updateFilter(newFilter)
                },
            )
        }

        Row() {
            AssistChip(
                onClick = {
                    playbackViewModel.playAlbum(filteredAlbums.random())
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Random Album")
                },
                label = { Text("Play Random Album From Filtered Albums") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            CreateCollectionButton(
                onCreateCollection = { name ->
                    viewModel.createCollectionFromCurrentFilter(name)
                }
            )
        }

        Text(
            "${filteredAlbums.size} albums",
            color = MaterialTheme.colorScheme.onSurface,
        )


        AlbumGrid(
            filteredAlbums,
            albumActions = albumTileActions
        )

        if (syncState is SyncState.Error) {
            Text(
                text = (syncState as SyncState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}