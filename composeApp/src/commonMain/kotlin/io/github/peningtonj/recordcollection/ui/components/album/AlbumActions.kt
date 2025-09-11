package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.LibraryViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel

data class AlbumActions(
    val navigateToPage: (AlbumDetailUiState) -> Unit = {},
    val play: (AlbumDetailUiState) -> Unit = {},
    val pause: (AlbumDetailUiState) -> Unit = {},
    val showContextMenu: (AlbumDetailUiState) -> Unit = {},
    val updateRating: (AlbumDetailUiState, Int) -> Unit = { _, _ -> },
    val navigateToArtist: (AlbumDetailUiState) -> Unit = {},
    val addToLibrary: (AlbumDetailUiState) -> Unit = {},
    val removeFromLibrary: (AlbumDetailUiState) -> Unit = {},
    val toggleLibraryStatus: (AlbumDetailUiState) -> Unit = {},
    val getReleaseGroup: (AlbumDetailUiState) -> Unit = {},
    val addToCollection: (AlbumDetailUiState, String) -> Unit = {_, _, -> },
    val addToNewCollection: (AlbumDetailUiState) -> Unit = {},
    val removeTag: (AlbumDetailUiState, String) -> Unit = {_, _ -> },
    val addTag: (AlbumDetailUiState, Tag) -> Unit = {_, _ -> },
    val addAllSongsToSavedSongs: (AlbumDetailUiState) -> Unit = {},
)

@Composable
fun rememberAlbumActions(
    playbackViewModel: PlaybackViewModel,
    albumViewModel: AlbumViewModel,
    libraryViewModel: LibraryViewModel,
    collectionsViewModel: CollectionsViewModel,
    settings: SettingsViewModel,
    navigator: Navigator = LocalNavigator.current,
): AlbumActions {
    val rateAction: (AlbumDetailUiState, Int) -> Unit = remember(albumViewModel) {
        albumViewModel.let { vm ->
            {
                album, rating -> vm.setRating(album.album.id, rating)
                val settings = settings.settings.value

                if (rating == 10 && settings.addTracksOnMaxRating) {
                    libraryViewModel.addAllSongsFromAlbumToSavedSongs(album.album)
                }
            }
        }
    }

    return AlbumActions(
        navigateToPage = { album -> navigator.navigateTo(Screen.Album(album.album.id)) },
        navigateToArtist = { album -> navigator.navigateTo(Screen.Artist(album.album.artists.first().id)) },
        play = { album -> playbackViewModel.playAlbum(album) },
        updateRating = rateAction,
        addToLibrary = { album -> libraryViewModel.addAlbumToLibrary(album.album) },
        removeFromLibrary = { album -> libraryViewModel.removeAlbumFromLibrary(album.album) },
        getReleaseGroup = { album ->
            albumViewModel.updateReleaseGroup(album = album.album)
        },
        addToCollection = {album, collectionName ->
            albumViewModel.addAlbumToCollection(album.album, collectionName)
        },
        addToNewCollection = {album ->
            collectionsViewModel.createCollection(album.album.name)
            albumViewModel.addAlbumToCollection(album.album, album.album.name)
        },
        toggleLibraryStatus = {album ->
            if (album.album.inLibrary) {
                libraryViewModel.removeAlbumFromLibrary(album.album)
            } else {
                libraryViewModel.addAlbumToLibrary(album.album)
            }
        },
        addAllSongsToSavedSongs = {album ->
            libraryViewModel.addAllSongsFromAlbumToSavedSongs(album.album)
        }
    )
}

data class CollectionAlbumActions(
    val removeFromCollection:  (AlbumDetailUiState) -> Unit = {},
    val swapWithRelease:  (AlbumDetailUiState, Album) -> Unit = {_, _ -> },
    val playFromCollection: (AlbumDetailUiState) -> Unit = {},
)

fun getCollectionActionAlbums(
    collection: AlbumCollection?,
    playbackViewModel: PlaybackViewModel,
    albumViewModel: AlbumViewModel,
    collectionDetailViewModel: CollectionDetailViewModel
) : CollectionAlbumActions {
    if (collection == null) {
        return CollectionAlbumActions()
    } else {
        return CollectionAlbumActions(
            removeFromCollection = { album ->
                albumViewModel.removeAlbumFromCollection(album.album, collection.name)
            },
            swapWithRelease = { album, replacement ->
                albumViewModel.removeAlbumFromCollection(
                    album.album,
                    collection.name,
                    )
                albumViewModel.addAlbumToCollection(
                    replacement,
                    collection.name
                )
            },
            playFromCollection = { album ->
                val allAlbums = collectionDetailViewModel.uiState.value.albums
                val startIndex = allAlbums.indexOfFirst { it.album.id == album.album.id }

                if (startIndex != -1) {
                    val queue = allAlbums.drop(startIndex + 1)
                    playbackViewModel.playAlbum(
                        album,
                        queue = queue,
                        collection = collection
                    )
                } else {
                    Napier.e { "Album not found in collection for playback" }
                }
            }
        )
    }
}

