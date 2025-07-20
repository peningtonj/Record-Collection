package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
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
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel

data class CollectionContextMenuAction(
    val label: String,
    val icon: ImageVector,
    val action: (AlbumCollection) -> Unit,
    val hasSubmenu: Boolean = false
)

@Composable
fun rememberCollectionContextMenuActions(
    collectionsViewModel: CollectionsViewModel,
    onRename: (AlbumCollection) -> Unit,
    onDelete: (AlbumCollection) -> Unit
): Map<String, CollectionContextMenuAction> {
    return remember(collectionsViewModel, onRename, onDelete) {
        mapOf(
            "rename" to CollectionContextMenuAction(
                label = "Rename",
                icon = Icons.Default.Edit,
                action = onRename
            ),
            "move_to_folder" to CollectionContextMenuAction(
                label = "Move to Folder",
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                action = { collection -> /* This will be handled by the submenu */ },
                hasSubmenu = true
            ),
            "delete" to CollectionContextMenuAction(
                label = "Delete",
                icon = Icons.Default.Delete,
                action = onDelete
            )
        )
    }
}

@Composable
fun CollectionContextMenu(
    collection: AlbumCollection,
    actions: Map<String, CollectionContextMenuAction>,
    onDismiss: () -> Unit,
    enabledActions: Set<String> = actions.keys,
    collectionsViewModel: CollectionsViewModel
) {
    var showFolderSubmenu by remember { mutableStateOf(false) }
    val collectionsState by collectionsViewModel.uiState.collectAsState()

    enabledActions.forEach { actionKey ->
        actions[actionKey]?.let { menuAction ->
            if (menuAction.hasSubmenu && actionKey == "move_to_folder") {
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
                        onClick = { showFolderSubmenu = true },
                        leadingIcon = {
                            Icon(menuAction.icon, contentDescription = null)
                        }
                    )

                    // Folders submenu
                    DropdownMenu(
                        expanded = showFolderSubmenu,
                        onDismissRequest = { showFolderSubmenu = false },
                        offset = DpOffset(x = 180.dp, y = (-32).dp),
                        properties = PopupProperties(
                            focusable = true,
                            dismissOnBackPress = true,
                            dismissOnClickOutside = true,
                            clippingEnabled = false
                        )
                    ) {
                        // Move to root (no folder)
                        DropdownMenuItem(
                            text = { Text("Move to Root") },
                            onClick = {
                                collectionsViewModel.updateCollectionParent(collection, null)
                                showFolderSubmenu = false
                                onDismiss()
                            }
                        )
                        
                        // Move to existing folders
                        collectionsState.folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.folderName) },
                                onClick = {
                                    collectionsViewModel.updateCollectionParent(collection, folder.folderName)
                                    showFolderSubmenu = false
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            } else {
                DropdownMenuItem(
                    text = { Text(menuAction.label) },
                    onClick = {
                        menuAction.action(collection)
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