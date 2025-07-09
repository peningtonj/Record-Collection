package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionDetailViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel

@Composable
fun CollectionScreen(
    collectionName: String,
    viewModel: CollectionDetailViewModel = rememberCollectionDetailViewModel(collectionName),
    playbackViewModel: PlaybackViewModel = rememberPlaybackViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val navigator = LocalNavigator.current

    AlbumGrid(uiState.albums.map { AlbumDisplayData(it.album.album,0, it.rating) },
        onAlbumClick = { album ->
            navigator.navigateTo(Screen.Album(album.id))
        },
        onPlayClick = { album ->
            playbackViewModel.playAlbum(album)
        }
    )

}