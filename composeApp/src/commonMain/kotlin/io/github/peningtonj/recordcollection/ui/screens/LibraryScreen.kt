// commonMain/ui/screens/LibraryScreen.kt
package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
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
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.filter.ActiveChips
import io.github.peningtonj.recordcollection.ui.components.filter.ReleaseYearFilter
import io.github.peningtonj.recordcollection.ui.components.filter.TextSearchBar
import io.github.peningtonj.recordcollection.viewmodel.LibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SyncState
import io.github.peningtonj.recordcollection.viewmodel.rememberLibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import kotlinx.datetime.LocalDate

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = rememberLibraryViewModel(),
    playbackViewModel: PlaybackViewModel = rememberPlaybackViewModel()
) {
    val syncState by viewModel.syncState.collectAsState()
    val libraryStats by viewModel.libraryStats.collectAsState()
    val filteredAlbums by viewModel.filteredAlbums.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val genres by viewModel.allGenres.collectAsState()
    val earliestReleaseDate by viewModel.earliestReleaseDate.collectAsState(LocalDate(1950,1,1))

    val navigator = LocalNavigator.current


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
                text = "Saved Albums (${libraryStats.totalAlbums})",
                style = MaterialTheme.typography.headlineMedium
            )

            IconButton(
                onClick = { viewModel.syncLibrary() },
                enabled = syncState !is SyncState.Syncing
            ) {
                when (syncState) {
                    is SyncState.Syncing -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                else -> Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "sync"
                    )
                }
            }
        }

        Row {
            TextSearchBar(
                currentFilter,
                options = filterOptions,
                onFilterChange = { newFilter ->
                    viewModel.updateFilter(newFilter)
                },
                modifier = Modifier.weight(1f)
            )

            ReleaseYearFilter(
                onFilterChange = { start, end ->
                    val newYearRange = DateRange(
                        LocalDate(start, 1, 1),
                        LocalDate(end, 12, 31),
                    )
                    viewModel.updateFilter(currentFilter.copy(releaseDateRange = newYearRange))
                },
                startYear = earliestReleaseDate?.year ?: 1950
            )
        }

        if (currentFilter.tags.isNotEmpty() || currentFilter.releaseDateRange != null) {
            ActiveChips(
                currentFilter = currentFilter,
                onFilterChange = { newFilter ->
                    viewModel.updateFilter(newFilter)
                },
            )
        }

        IconButton(
            onClick = {
                playbackViewModel.playAlbum(filteredAlbums.random())
            }
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Random Album")
        }

        AlbumGrid(filteredAlbums,
            onAlbumClick = { album ->
                navigator.navigateTo(Screen.Album(album.id))
            },
            onPlayClick = { album ->
                playbackViewModel.playAlbum(album)
            }
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