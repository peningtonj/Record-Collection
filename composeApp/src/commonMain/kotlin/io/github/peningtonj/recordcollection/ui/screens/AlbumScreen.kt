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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import io.github.peningtonj.recordcollection.ui.components.album.AlbumHeader
import io.github.peningtonj.recordcollection.ui.components.album.TrackListing
import io.github.peningtonj.recordcollection.viewmodel.AlbumScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.common.ErrorMessage
import io.github.peningtonj.recordcollection.ui.components.tag.AddTagDialog


@Composable
fun AlbumScreen(
    albumId: String,
    viewModel: AlbumViewModel = rememberAlbumViewModel(),
    playbackViewModel: PlaybackViewModel = rememberPlaybackViewModel(),
) {
    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }
    var showAddTagDialog by remember { mutableStateOf(false) }

    Napier.d { "AlbumScreen: albumId = $albumId" }

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
            val albumDetail = successState.albumDetail

            // Detailed logging of AlbumDetailUiState
//            Napier.d { "=== AlbumDetailUiState Data ===" }
//            Napier.d { "Album: ${albumDetail.album.name} by ${albumDetail.album.primaryArtist}" }
//            Napier.d { "Album ID: ${albumDetail.album.id}" }
//            Napier.d { "Total Tags: ${albumDetail.tags.size}" }
//            Napier.d { "Release Date: ${albumDetail.album.releaseDate}" }
//            Napier.d { "Total Tracks: ${albumDetail.album.totalTracks}" }
//            Napier.d { "Total Duration: ${albumDetail.totalDuration}ms" }
//            Napier.d { "Rating: ${albumDetail.rating?.rating ?: "No rating"}" }
//            Napier.d { "Is Loading: ${albumDetail.isLoading}" }
//            Napier.d { "Error: ${albumDetail.error ?: "No error"}" }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlbumHeader(
                    albumDetailUiState = successState.albumDetail,
                    onPlayClick = { playbackViewModel.playAlbum(album = successState.albumDetail.album) },
                    modifier = Modifier.fillMaxWidth(),
                    onRatingChange = { rating -> viewModel.setRating(albumId, rating) },
                    onRefreshClick = {
                        Napier.d { "Refreshing album ${successState.albumDetail.album.name}" }
                        viewModel.refreshAlbum(albumDetail.album)
                    },
                    removeTag = { tagId ->
                        viewModel.removeTagFromAlbum(albumId, tagId)
                    },
                    addTag = {
                        showAddTagDialog = true
                    },
                )

                if (showAddTagDialog) {
                    AddTagDialog(
                        onDismiss = { showAddTagDialog = false },
                        onConfirm = { tagKey, tagValue ->
                            viewModel.addTagToAlbum(albumId, tagKey, tagValue)
                            showAddTagDialog = false
                        }
                    )


                    // Display tracks
                    TrackListing(
                        successState.albumDetail.tracks,
                        onPlayClick = { track ->
                            playbackViewModel.playTrackFromAlbum(album = successState.albumDetail.album, track)
                        })
                }
            }
        }
    }
}

