package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel

@Composable
fun DefaultAlbumContextMenu(
    album: AlbumDetailUiState,
    actions: AlbumActions,
    onDismiss: () -> Unit,
    collectionAlbumActions: CollectionAlbumActions? = null,
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel()
) {
    val collectionsState by collectionsViewModel.uiState.collectAsState()

    DropdownMenuItem(
        text = { Text("Play Album") },
        onClick = {
            collectionAlbumActions?.playFromCollection(album) ?: actions.play(album)
            onDismiss()
        },
        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
    )

    DropdownMenuItem(
        text = { Text(if (album.album.inLibrary) "Remove from Library" else "Add to Library") },
        onClick = {
            actions.toggleLibraryStatus(album)
            onDismiss()
        },
        leadingIcon = {
            Icon(
                if (album.album.inLibrary) Icons.Default.Delete else Icons.Default.Add,
                contentDescription = null
            )
        }
    )

    if (collectionAlbumActions != null) {
        if (album.releaseGroup.size > 1) {
            SwapReleaseMenu(
                releases = album.releaseGroup.filter { it.id != album.album.id },
                swapRelease = { release ->
                    collectionAlbumActions.swapWithRelease(album, release)
                },
                onDismiss = onDismiss
            )
        }

        DropdownMenuItem(
            text = { Text("Remove from this collection") },
            onClick = {
                collectionAlbumActions.removeFromCollection(album)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Default.Delete,
                    contentDescription = "Remove from collection"
                )
            }

        )
    }


    // Submenu
    AddToCollectionDropdown(
        collections = collectionsState.collections,
        album = album.album,
        addAlbumToCollection = { name -> actions.addToCollection(album, name) },
        createCollectionWithAlbum = { actions.addToNewCollection(album) },
        onDismiss = onDismiss
    )

}

@Composable
fun AddToCollectionDropdown(
    collections: List<AlbumCollection>,
    album: Album,
    addAlbumToCollection: (String) -> Unit,
    createCollectionWithAlbum: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box {
        DropdownMenuItem(
            text = {
                Row {
                    Text("Add to Collection")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null)
                }
            },
            onClick = { showDropdown = true },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        )

        CollectionDropdownContent(
            expanded = showDropdown,
            onDismissRequest = {
                showDropdown = false
                onDismiss()
            },
            collections = collections,
            album = album,
            addAlbumToCollection = addAlbumToCollection,
            createCollectionWithAlbum = createCollectionWithAlbum
        )
    }
}


@Composable
fun CollectionDropdownContent(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    collections: List<AlbumCollection>,
    album: Album,
    addAlbumToCollection: (String) -> Unit,
    createCollectionWithAlbum: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 0.dp),
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = false
        )
    ) {
        collections.forEach { collection ->
            DropdownMenuItem(
                text = { Text(collection.name) },
                onClick = {
                    addAlbumToCollection(collection.name)
                    onDismissRequest()
                }
            )
        }

        DropdownMenuItem(
            text = { Text("Create New Collection...") },
            onClick = {
                Napier.d { "Create new collection for ${album.name}" }
                createCollectionWithAlbum()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        )
    }
}


