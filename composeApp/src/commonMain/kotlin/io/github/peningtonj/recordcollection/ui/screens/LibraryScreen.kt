// commonMain/ui/screens/LibraryScreen.kt
package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.ui.AppPlatform
import io.github.peningtonj.recordcollection.ui.LocalPlatform
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumActions
import io.github.peningtonj.recordcollection.ui.components.common.ScreenHeader
import io.github.peningtonj.recordcollection.ui.components.common.HeaderActionButton
import io.github.peningtonj.recordcollection.ui.components.filter.ActiveChips
import io.github.peningtonj.recordcollection.ui.components.filter.HorizontalFilterBar
import io.github.peningtonj.recordcollection.ui.components.filter.TextSearchBar
import io.github.peningtonj.recordcollection.ui.components.library.CreateCollectionButton
import io.github.peningtonj.recordcollection.viewmodel.*
import kotlinx.datetime.LocalDate

@Composable
fun LibraryScreen(
    playbackViewModel: PlaybackViewModel,
    viewModel: LibraryViewModel = rememberLibraryViewModel(),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    settingsViewModel: SettingsViewModel = rememberSettingsViewModel(),
) {
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
        settings = settingsViewModel,
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

    val isAndroid = LocalPlatform.current == AppPlatform.ANDROID
    var filtersExpanded by remember { mutableStateOf(false) }
    val hasActiveFilters = currentFilter.tags.isNotEmpty() ||
        currentFilter.releaseDateRange != null ||
        (currentFilter.minRating ?: 0) > 0

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        ScreenHeader(
            title = "Library",
            subtitle = "${filteredAlbums.size} ${if (filteredAlbums.size == 1) "album" else "albums"}",
            icon = Icons.Default.LibraryMusic,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isAndroid) {
                // Compact Android filter: toggle chip + inline active chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = filtersExpanded || hasActiveFilters,
                        onClick = { filtersExpanded = !filtersExpanded },
                        label = { Text("Filter") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    if (hasActiveFilters) {
                        Box(modifier = Modifier.weight(1f)) {
                            ActiveChips(
                                currentFilter = currentFilter,
                                onFilterChange = { viewModel.updateFilter(it) }
                            )
                        }
                    }
                }

                // Expandable filter controls
                if (filtersExpanded) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextSearchBar(
                                currentFilter,
                                options = filterOptions,
                                onFilterChange = { newFilter -> viewModel.updateFilter(newFilter) },
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
                        }
                    }
                }
            } else {
                // Desktop: full always-visible filter card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextSearchBar(
                            currentFilter,
                            options = filterOptions,
                            onFilterChange = { newFilter -> viewModel.updateFilter(newFilter) },
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
                                onFilterChange = { newFilter -> viewModel.updateFilter(newFilter) },
                            )
                        }
                    }
                }
            }

            // Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HeaderActionButton(
                    onClick = {
                        if (filteredAlbums.isNotEmpty()) {
                            playbackViewModel.playAlbum(filteredAlbums.random())
                        }
                    },
                    icon = Icons.Default.Shuffle,
                    label = "Random Album",
                    primary = true
                )

                CreateCollectionButton(
                    onCreateCollection = { name ->
                        viewModel.createCollectionFromCurrentFilter(name)
                    }
                )
            }

            // Albums Grid
            AlbumGrid(
                filteredAlbums,
                albumActions = albumTileActions
            )
        }
    }
}