package io.github.peningtonj.recordcollection.ui.screens

import AlbumResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchResult
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.filter.TextSearchBar
import io.github.peningtonj.recordcollection.ui.components.search.AlbumSearchItem
import io.github.peningtonj.recordcollection.ui.components.search.ArtistSearchItem
import io.github.peningtonj.recordcollection.util.RankedSearchResults
import io.github.peningtonj.recordcollection.viewmodel.SearchScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.SearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = rememberSearchViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuery by viewModel.currentQuery.collectAsState()

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
            is SearchScreenUiState.Loading -> {
                LoadingIndicator()
            }

            is SearchScreenUiState.Error -> {
                Text((uiState as SearchScreenUiState.Error).message)
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
    result: RankedSearchResults,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Albums
        result.albums.let { albums ->
            if (albums.isNotEmpty()) {
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

        result.artists?.let { artists ->
            if (artists.isNotEmpty()) {
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