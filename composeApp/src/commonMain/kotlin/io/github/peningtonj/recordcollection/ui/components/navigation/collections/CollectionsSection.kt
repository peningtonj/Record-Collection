package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.common.TextInputDialog
import io.github.peningtonj.recordcollection.viewmodel.AlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel

@Composable
fun CollectionsSection(
    currentScreen: Screen,
    navigator: Navigator,
    viewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel()
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showCollectionFromArticleDialog by remember { mutableStateOf(false) }
    var showCollectionFromArticle by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var collectionToRename by remember { mutableStateOf<AlbumCollection?>(null) }
    var collectionToDelete by remember { mutableStateOf<AlbumCollection?>(null) }
    // Use the same viewModel instance for both state and actions
    val collectionsUiState by viewModel.uiState.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val importResult by viewModel.importResult.collectAsState()

    Napier.d("CollectionsSection: currentFolder: $currentFolder")
    Napier.d("CollectionsSection: collectionsUiState: $collectionsUiState")

    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        // Back button if we're in a folder
        if (currentFolder != null) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = currentFolder ?: "Collections",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
                .weight(1f)
        )

        Box {
            IconButton(
                onClick = { showDropdown = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new collection or folder",
                    modifier = Modifier.size(16.dp)
                )
            }

            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("New Collection") },
                    onClick = {
                        showDropdown = false
                        showCollectionDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Collection From Article") },
                    onClick = {
                        showDropdown = false
                        showCollectionFromArticleDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("New Folder") },
                    onClick = {
                        showDropdown = false
                        showFolderDialog = true
                    }
                )
            }
        }
    }

    // Collection dialog
    TextInputDialog(
        title = "New Collection",
        label = "Collection Name",
        placeholder = "Enter collection name",
        isVisible = showCollectionDialog,
        onDismiss = { showCollectionDialog = false },
        onConfirm = { name ->
            viewModel.createCollection(name)
        },
        confirmButtonText = "Create"
    )

    TextInputDialog(
        title = "Collection From Article",
        label = "Article URL",
        placeholder = "Enter Article URL",
        isVisible = showCollectionFromArticleDialog,
        onDismiss = { showCollectionFromArticleDialog = false },
        onConfirm = { name ->
            showCollectionFromArticle = true
            viewModel.draftCollectionFromUrl(name)
        },
        confirmButtonText = "Create"
    )

    ImportCollectionDialog(
        isVisible = showCollectionFromArticle,
        content = importResult,
        onDismiss = { showCollectionFromArticle = false },
        onSearch = {
            Napier.d("Importing collection from draft????")
            viewModel.getAlbumsFromDraft() },
        onMakeCollection = { collectionName ->
            viewModel.createCollection(collectionName)
            importResult.albums.forEach { album ->
                if (album.album != null) {
                    albumViewModel.addAlbumToCollection(album.album, collectionName)
                }
            }
            showCollectionFromArticle = false
        }
    )

    // Folder dialog
    TextInputDialog(
        title = "New Folder",
        label = "Folder Name",
        placeholder = "Enter folder name",
        isVisible = showFolderDialog,
        onDismiss = { showFolderDialog = false },
        onConfirm = { name ->
            viewModel.createTopLevelFolder(name)
        },
        confirmButtonText = "Create"
    )

    // Rename dialog
    collectionToRename?.let { collection ->
        TextInputDialog(
            title = "Rename Collection",
            label = "Collection Name",
            placeholder = "Enter new collection name",
            initialValue = collection.name,
            isVisible = showRenameDialog,
            onDismiss = { 
                showRenameDialog = false
                collectionToRename = null
            },
            onConfirm = { newName ->
                viewModel.updateCollection(collection.name, collection.copy(name = newName))
                showRenameDialog = false
                collectionToRename = null
            },
            confirmButtonText = "Rename"
        )
    }

    // Delete dialog
    collectionToDelete?.let { collection ->
        CollectionDeleteDialog(
            collection = collection,
            isVisible = showDeleteDialog,
            onDismiss = { 
                showDeleteDialog = false
                collectionToDelete = null
            },
            onConfirm = {
                viewModel.deleteCollection(collection.name)
                showDeleteDialog = false
                collectionToDelete = null
            }
        )
    }

    // ... existing loading/empty state handling ...

    // Collections display with delete handler
    if (!collectionsUiState.isLoading && (collectionsUiState.collections.isNotEmpty() || collectionsUiState.folders.isNotEmpty())) {
        // Show folders first
        collectionsUiState.folders.forEach { folder ->
            FolderItem(
                folder = folder,
                onClick = { viewModel.navigateToFolder(folder.folderName) }
            )
        }

        // Then show collections with context menu including delete
        collectionsUiState.collections.forEach { collection ->
            val contextMenuActions = rememberCollectionContextMenuActions(
                collectionsViewModel = viewModel,
                onRename = {
                    collectionToRename = it
                    showRenameDialog = true
                },
                onDelete = {
                    collectionToDelete = it
                    showDeleteDialog = true
                }
            )

            CollectionItem(
                collection = collection,
                isSelected = currentScreen is Screen.Collection &&
                        currentScreen.collectionName == collection.name,
                onClick = {
                    navigator.navigateTo(Screen.Collection(collection.name))
                },
                onContextMenu = { /* Context menu will be handled by the item itself */ },
                contextMenuContent = { onDismiss ->
                    CollectionContextMenu(
                        collection = collection,
                        actions = contextMenuActions,
                        onDismiss = onDismiss,
                        collectionsViewModel = viewModel
                    )
                }
            )
        }
    }
}