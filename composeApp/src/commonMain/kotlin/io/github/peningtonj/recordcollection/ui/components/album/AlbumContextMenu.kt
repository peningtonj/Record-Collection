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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel

data class AlbumContextMenuAction(
    val label: String,
    val icon: ImageVector,
    val action: (AlbumDetailUiState) -> Unit,
    val hasSubmenu: Boolean = false
)

@Composable
fun rememberAlbumContextMenuActions(
    playbackViewModel: PlaybackViewModel = rememberPlaybackViewModel(),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    defaultCollectionName: String? = null,
    collectionDetailViewModel: CollectionDetailViewModel? = null
): Map<String, AlbumContextMenuAction> {
    return remember(
        playbackViewModel,
        collectionsViewModel,
        albumViewModel,
        collectionDetailViewModel,
        defaultCollectionName
    ) {
        mapOf(
            "play" to AlbumContextMenuAction(
                label = "Play Album",
                icon = Icons.Default.PlayArrow,
                action = { album -> playbackViewModel.playAlbum(album) }
            ),
            "add_to_collection" to AlbumContextMenuAction(
                label = "Add to Collection",
                icon = Icons.Default.Add,
                action = { album -> /* This will be handled by the submenu */ },
                hasSubmenu = true
            ),
            "remove_from_collection" to AlbumContextMenuAction(
                label = "Remove from Collection",
                icon = Icons.Default.Delete,
                action = { album ->
                    Napier.d { "Trying to remove from $defaultCollectionName collection" }

                    defaultCollectionName?.let { collectionName ->
                        if (collectionDetailViewModel != null) {
                            collectionDetailViewModel.removeAlbumFromCollection(album.album.id)
                        } else {
                            albumViewModel.removeAlbumFromCollection(album.album, collectionName)
                        }
                    }
                }
            ),
        )
    }
}

@Composable
fun AlbumContextMenu(
    album: AlbumDetailUiState,
    actions: Map<String, AlbumContextMenuAction>,
    onDismiss: () -> Unit,
    enabledActions: Set<String> = actions.keys,
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel()
) {
    var showCollectionSubmenu by remember { mutableStateOf(false) }
    val collectionsState by collectionsViewModel.uiState.collectAsState()

    enabledActions.forEach { actionKey ->
        actions[actionKey]?.let { menuAction ->
            if (menuAction.hasSubmenu && actionKey == "add_to_collection") {
                Box {
                    DropdownMenuItem(
                        text = {
                            Row {
                                Text(menuAction.label)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = null
                                )
                            }
                        },
                        onClick = { showCollectionSubmenu = true },
                        leadingIcon = {
                            Icon(menuAction.icon, contentDescription = null)
                        }
                    )

                    // Collections submenu with reliable positioning
                    DropdownMenu(
                        expanded = showCollectionSubmenu,
                        onDismissRequest = { showCollectionSubmenu = false },
                        offset = DpOffset(x = 180.dp, y = (-32).dp), // Position to the right and slightly up
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            clippingEnabled = false // Allow menu to extend beyond parent bounds
                        )
                    ) {
                        collectionsState.collections.forEach { collection ->
                            DropdownMenuItem(
                                text = { Text(collection.name) },
                                onClick = {
                                    albumViewModel.addAlbumToCollection(album.album, collection.name)
                                    Napier.d { "Added ${album.album.name} to ${collection.name}" }
                                    showCollectionSubmenu = false
                                    onDismiss()
                                }
                            )
                        }
                        
                        // Option to create new collection
                        DropdownMenuItem(
                            text = { Text("Create New Collection...") },
                            onClick = {
                                Napier.d { "Create new collection for ${album.album.name}" }
                                collectionsViewModel.createCollection(album.album.name)
                                showCollectionSubmenu = false
                                onDismiss()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        )
                    }
                }
            } else {
                DropdownMenuItem(
                    text = { Text(menuAction.label) },
                    onClick = {
                        menuAction.action(album)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(menuAction.icon, contentDescription = null)
                    }
                )
            }
        }
    }
}

// Convenience functions for common use cases
@Composable
fun StandardAlbumContextMenu(
    album: AlbumDetailUiState,
    actions: Map<String, AlbumContextMenuAction>,
    onDismiss: () -> Unit
) {
    AlbumContextMenu(
        album = album,
        actions = actions,
        onDismiss = onDismiss,
        enabledActions = setOf("play", "add_to_collection")
    )
}

@Composable
fun CollectionAlbumContextMenu(
    album: AlbumDetailUiState,
    actions: Map<String, AlbumContextMenuAction>,
    onDismiss: () -> Unit,
    defaultCollectionName: String? = null
) {
    AlbumContextMenu(
        album = album,
        actions = actions,
        onDismiss = onDismiss,
        enabledActions = setOf("play", "remove_from_collection", "add_to_collection")
    )
}