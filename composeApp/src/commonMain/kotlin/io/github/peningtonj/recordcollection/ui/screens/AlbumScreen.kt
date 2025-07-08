package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.viewmodel.AlbumScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumScreenViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumScreenViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.common.ErrorMessage


@Composable
fun AlbumScreen(
    albumId: String,
    viewModel: AlbumScreenViewModel = rememberAlbumScreenViewModel(),
    playbackViewModel: PlaybackViewModel = rememberPlaybackViewModel()
) {
    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AlbumScreenUiState.Loading -> {
            // Show loading state
            LoadingIndicator()
        }
        is AlbumScreenUiState.Error -> {
            // Show error state
            ErrorMessage((uiState as AlbumScreenUiState.Error).message)
        }
        is AlbumScreenUiState.Success -> {
            val successState = uiState as AlbumScreenUiState.Success
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
                        text = "Album: ${successState.album.name}",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                // Display tracks
                successState.tracks.forEach { track ->
                    Text( track.name)
                }
            }
        }
    }
}
