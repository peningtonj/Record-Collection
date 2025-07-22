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
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.album.getCollectionActionAlbums
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumActions
import io.github.peningtonj.recordcollection.ui.components.collections.CollectionHeader
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.LibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionDetailViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberLibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel

@Composable
fun CollectionScreen(
    collectionName: String,
    playbackViewModel: PlaybackViewModel,
    viewModel: CollectionDetailViewModel = rememberCollectionDetailViewModel(collectionName),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
    libraryViewModel: LibraryViewModel = rememberLibraryViewModel(),
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    settingsViewModel: SettingsViewModel = rememberSettingsViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val navigator = LocalNavigator.current
    val albumTileActions = rememberAlbumActions(
        playbackViewModel,
        albumViewModel,
        libraryViewModel,
        collectionsViewModel,
        navigator,
    )

    val currentSession = playbackViewModel.currentSession.collectAsState()
    val playbackState = playbackViewModel.playbackState.collectAsState()

    val settings = settingsViewModel.settings.collectAsState()
    val addToLibrarySetting = settings.value.collectionAddToLibrary.getOrDefault(collectionName, OnAddToCollection.DEFAULT)

    val collectionAlbumActions = getCollectionActionAlbums(
        collection = uiState.collection,
        playbackViewModel = playbackViewModel,
        albumViewModel = albumViewModel,
        collectionDetailViewModel = viewModel,
    )

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
                    if (playbackState.value?.isPlaying ?: false &&
                        currentSession.value?.playingFrom?.name == collectionName
                    ) {
                        playbackViewModel.toggleShuffle(uiState.albums)
                    } else {
                        val shuffledList = uiState.albums.shuffled()
                        playbackViewModel.playAlbum(
                            shuffledList.first(),
                            shuffledList.drop(1),
                            collection = uiState.collection,
                            isShuffled = true
                        )
                    }
                },
            isShuffled = currentSession.value?.isShuffled ?: false,
            addToLibrarySetting = addToLibrarySetting,
            onAddToLibrarySettingChange = { newSetting ->
                settingsViewModel.updateOnAddToLibrarySetting(collectionName, newSetting)
            }

            )

        AlbumGrid(
            uiState.albums,
            albumActions = albumTileActions,
            collectionAlbumActions = collectionAlbumActions
        )
    }
}