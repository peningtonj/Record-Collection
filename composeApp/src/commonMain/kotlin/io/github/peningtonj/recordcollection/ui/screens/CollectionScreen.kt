package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.album.CollectionAlbumContextMenu
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumContextMenuActions
import io.github.peningtonj.recordcollection.ui.components.collections.CollectionHeader
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionDetailViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel

@Composable
fun CollectionScreen(
    collectionName: String,
    playbackViewModel: PlaybackViewModel,
    viewModel: CollectionDetailViewModel = rememberCollectionDetailViewModel(collectionName),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val albumActions = rememberAlbumContextMenuActions(
        defaultCollectionName = collectionName
    )

    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CollectionHeader(
            collectionName = collectionName,
            albumCount = uiState.albums.size,
            onPlayAllClick = {
                playbackViewModel.playAlbum(
                    uiState.albums.first(),
                    uiState.albums.drop(1),
                    collection = uiState.collection
                )
            },
            onRandomClick =
                {
                    val shuffledList = uiState.albums.shuffled()
                    playbackViewModel.playAlbum(
                        shuffledList.first(),
                        shuffledList.drop(1),
                        collection = uiState.collection
                    )
                        }
            )

        AlbumGrid(
            uiState.albums,
            onAlbumClick = { album ->
                navigator.navigateTo(Screen.Album(album.album.id))
            },
            onPlayClick = { album ->
                albumActions["play"]?.action?.invoke(album)
            },
            onRatingChange = { album, rating ->
                albumViewModel.setRating(album.album.id, rating)
            },
            contextMenuContent = { album, onDismiss ->
                CollectionAlbumContextMenu(
                    album = album,
                    actions = albumActions,
                    onDismiss = onDismiss,
                )
            }
        )
    }
}