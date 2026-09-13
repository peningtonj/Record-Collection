package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumActions
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.search.AlbumSearchItem
import io.github.peningtonj.recordcollection.ui.components.search.ArtistSearchItem
import io.github.peningtonj.recordcollection.ui.AppPlatform
import io.github.peningtonj.recordcollection.ui.LocalPlatform
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SearchScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.SearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberLibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    playbackViewModel: PlaybackViewModel,
    viewModel: SearchViewModel = rememberSearchViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuery by viewModel.currentQuery.collectAsState()
    val newReleases by viewModel.newReleaseAlbums.collectAsState()
    val isAndroid = LocalPlatform.current == AppPlatform.ANDROID

    val albumActions = rememberAlbumActions(
        playbackViewModel,
        rememberAlbumViewModel(),
        rememberLibraryViewModel(),
        rememberCollectionsViewModel(),
        settings = rememberSettingsViewModel(),
        LocalNavigator.current,
    )


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = currentQuery,
            onValueChange = { viewModel.search(it) },
            label = { Text("Search albums, artists, tracks...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (currentQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Results
        when (uiState) {
            is SearchScreenUiState.Idle -> {
                Column {
                    Row {
                        Text(
                            "New Releases",
                            style = if (isAndroid) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.updateNewReleaseAlbums(forceRefresh = true)
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Update new releases"
                                )
                            },
                            label = { Text("Update New Releases") }
                        )
                    }
                    AlbumGrid(
                        newReleases,
                        albumActions = albumActions,
                        showRating = false
                    )
                }
            }
            is SearchScreenUiState.Loading -> {
                LoadingIndicator()
                if (newReleases.isEmpty() && currentQuery.isEmpty()) {
                    Text("Loading Search Results")
                }
            }

            is SearchScreenUiState.LoadingNewReleases -> {
                LoadingIndicator()
                if (newReleases.isEmpty() && currentQuery.isEmpty()) {
                    Text("Loading Spotify's New Release Recommendations")
                }
            }

            is SearchScreenUiState.Error -> {
                LoadingIndicator()
            }

            is SearchScreenUiState.Success -> {
                SearchResults(
                    result = (uiState as SearchScreenUiState.Success).result,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
@Composable
private fun SearchResults(
    result: SearchResult,
    modifier: Modifier = Modifier
) {
    val albums = result.albums.orEmpty()
    val artists = result.artists.orEmpty()
    var selectedTab by remember { mutableStateOf(0) }
    val itemsToShow = if (selectedTab == 0) albums else artists

    Column(modifier = modifier) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Albums (${albums.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Artists (${artists.size})") }
            )
        }

        if (itemsToShow.isEmpty()) {
            Text(
                text = if (selectedTab == 0) "No matching albums" else "No matching artists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (selectedTab == 0) {
                    items(albums) { album -> AlbumSearchItem(album) }
                } else {
                    items(artists) { artist -> ArtistSearchItem(artist) }
                }
            }
        }
    }
}