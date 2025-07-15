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
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.album.AlbumHeader
import io.github.peningtonj.recordcollection.ui.components.album.TrackListing
import io.github.peningtonj.recordcollection.viewmodel.AlbumScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.common.ErrorMessage
import io.github.peningtonj.recordcollection.ui.components.tag.AddTagDialog


@Composable
fun AlbumScreen(
    albumId: String,
    playbackViewModel: PlaybackViewModel,
    viewModel: AlbumViewModel = rememberAlbumViewModel(),
) {
    LaunchedEffect(albumId) {
        viewModel.loadAlbum(albumId)
    }
    var showAddTagDialog by remember { mutableStateOf(false) }

    Napier.d { "AlbumScreen: albumId = $albumId" }
    val navigator = LocalNavigator.current

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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AlbumHeader(
                    albumDetailUiState = successState.albumDetail,
                    onPlayClick = { playbackViewModel.playAlbum(album = successState.albumDetail) },
                    modifier = Modifier.fillMaxWidth(),
                    onRatingChange = { rating -> viewModel.setRating(albumId, rating) },
                    onRefreshClick = {
                        Napier.d { "Refreshing album ${successState.albumDetail.album.name}" }
                        viewModel.refreshAlbum(albumDetail.album)
                    },
                    onArtistClick = {
                        navigator.navigateTo(Screen.Artist(albumDetail.album.artists.first().id))
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
                }


                // Display tracks
                TrackListing(
                    successState.albumDetail.tracks,
                    onPlayClick = { track ->
                        playbackViewModel.playAlbum(
                            album = successState.albumDetail,
                            startFromTrack = track)
                    })

            }
        }
    }
}

