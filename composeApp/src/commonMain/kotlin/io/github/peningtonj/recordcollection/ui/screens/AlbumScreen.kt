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
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumActions
import io.github.peningtonj.recordcollection.viewmodel.AlbumScreenUiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.ui.components.common.ErrorMessage
import io.github.peningtonj.recordcollection.ui.components.playback.rememberPlaybackActions
import io.github.peningtonj.recordcollection.ui.components.tag.AddTagDialog
import io.github.peningtonj.recordcollection.viewmodel.AlbumDetailViewModel
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.LibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumDetailViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberLibraryViewModel


@Composable
fun AlbumScreen(
    albumId: String,
    playbackViewModel: PlaybackViewModel,
    viewModel: AlbumDetailViewModel = rememberAlbumDetailViewModel(albumId),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
    collectionViewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    libraryViewModel: LibraryViewModel = rememberLibraryViewModel()
) {
    LaunchedEffect(albumId) {
        viewModel.loadAlbum()
    }
    var showAddTagDialog by remember { mutableStateOf(false) }
    val playbackState by playbackViewModel.playbackState.collectAsState()

    Napier.d { "AlbumScreen: albumId = $albumId" }
    val navigator = LocalNavigator.current

    val uiState by viewModel.uiState.collectAsState()
    val releaseGroupStatus by albumViewModel.releaseGroupStatus.collectAsState()
    val collectionState by collectionViewModel.uiState.collectAsState()
    val albumActions = rememberAlbumActions(
        playbackViewModel = playbackViewModel,
        albumViewModel = albumViewModel,
        libraryViewModel = libraryViewModel,
        collectionsViewModel = collectionViewModel,
        navigator = navigator
    )

    val playbackActions = rememberPlaybackActions(
        playbackViewModel
    )

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
                    releaseGroupStatus = releaseGroupStatus,
                    collections = collectionState.collections,
                    modifier = Modifier.fillMaxWidth(),
                    showAddTagDialogClick = {
                        showAddTagDialog = true
                    },
                    isPlaying = {
                        playbackState?.track?.album?.id == albumDetail.album.id &&
                                playbackState?.isPlaying == true
                    },
                    onReleaseSelect = { albumId ->
                        navigator.navigateTo(Screen.Album(albumId))
                    },
                    albumActions = albumActions,
                    playbackActions = playbackActions
                )

                if (showAddTagDialog) {
                    AddTagDialog(
                        onDismiss = { showAddTagDialog = false },
                        onConfirm = { tagKey, tagValue ->
                            albumViewModel.addTagToAlbum(albumId, tagKey, tagValue)
                            showAddTagDialog = false
                        }
                    )
                }


                // Display tracks
                TrackListing(
                    successState.albumDetail.tracks,
                    onPlayClick = { track ->
                        if (playbackState?.track?.id == track.id && playbackState?.isPlaying == true) {
                            playbackViewModel.togglePlayPause()
                        } else {
                            playbackViewModel.playAlbum(
                                album = successState.albumDetail,
                                startFromTrack = track
                            )
                        }
                    },
                    onPauseClick = {
                        playbackViewModel.togglePlayPause()
                    },
                    isPlaying = { track ->
                            playbackState?.track?.id == track.id && playbackState?.isPlaying == true
                    }
                )

            }
        }
    }
}

