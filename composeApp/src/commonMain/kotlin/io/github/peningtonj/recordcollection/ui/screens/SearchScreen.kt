package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import io.github.peningtonj.recordcollection.util.RankedSearchResults
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SearchScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.SearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberLibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel
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

    val albumActions = rememberAlbumActions(
        playbackViewModel,
        rememberAlbumViewModel(),
        rememberLibraryViewModel(),
        rememberCollectionsViewModel(),
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
                            "New Releases: ",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.weight(1f)
                        )
                        AssistChip(
                            onClick = {
                                viewModel.viewModelScope.launch {
                                    viewModel.updateNewReleaseAlbums()
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
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Albums
        result.albums.let { albums ->
            if (albums?.isNotEmpty() ?: false) {
                item {
                    Text(
                        text = "Albums",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(albums.take(5)) { album ->
                    AlbumSearchItem(album)
                }
            }
        }

        result.artists.let { artists ->
            if (artists?.isNotEmpty() ?: false) {
                item {
                    Text(
                        text = "Artists",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(artists.take(3)) { artist ->
                    ArtistSearchItem(artist)
                }
            }
        }
    }
}