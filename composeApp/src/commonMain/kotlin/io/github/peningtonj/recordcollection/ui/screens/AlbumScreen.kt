package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.ui.components.album.AlbumHeader
import io.github.peningtonj.recordcollection.ui.components.album.TrackListing
import io.github.peningtonj.recordcollection.viewmodel.AlbumScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.common.ErrorMessage


@Composable
fun AlbumScreen(
    albumId: String,
    viewModel: AlbumViewModel = rememberAlbumViewModel(),
    playbackViewModel: PlaybackViewModel = rememberPlaybackViewModel()
) {
    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is AlbumScreenUiState.Loading -> {
            LoadingIndicator()
        }
        is AlbumScreenUiState.Error -> {
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
                AlbumHeader(
                    albumDisplayData = successState.album,
                    onPlayClick = { playbackViewModel.playAlbum(album = successState.album.album) },
                    modifier = Modifier.fillMaxWidth(),
                    onRatingChange = { rating -> viewModel.setRating(albumId, rating)}
                )

                // Display tracks
                TrackListing(successState.tracks,
                    onPlayClick = { track ->
                        playbackViewModel.playTrackFromAlbum(album = successState.album.album, track)
                    })
            }
        }
    }
}

