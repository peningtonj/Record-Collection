package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
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
import io.github.peningtonj.recordcollection.viewmodel.CollectionImportViewModel
import io.github.peningtonj.recordcollection.viewmodel.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.ImportSource
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel
import io.github.peningtonj.recordcollection.viewmodel.UiState
import io.github.peningtonj.recordcollection.viewmodel.rememberAlbumViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberArticleImportViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel

@Composable
fun CollectionsSection(
    currentScreen: Screen,
    navigator: Navigator,
    viewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    albumViewModel: AlbumViewModel = rememberAlbumViewModel(),
    articleImportViewModel: CollectionImportViewModel = rememberArticleImportViewModel(),
    settingsViewModel: SettingsViewModel = rememberSettingsViewModel()
) {
    var showDropdown by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showCollectionFromLinkDialog by remember { mutableStateOf(false) }
    var showImportCollectionDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPlaylistsDropdown by remember { mutableStateOf(false) }
    var collectionToRename by remember { mutableStateOf<AlbumCollection?>(null) }
    var collectionToDelete by remember { mutableStateOf<AlbumCollection?>(null) }
    // Use the same viewModel instance for both state and actions
    val collectionsUiState by viewModel.uiState.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    var importSource by remember { mutableStateOf<ImportSource?>(null) }

    val importUiState by articleImportViewModel.uiState.collectAsState()
    val userPlaylists by articleImportViewModel.userPlaylists.collectAsState()
    var collectionNameSuggestion by remember { mutableStateOf("") }
    val settings = settingsViewModel.settings.collectAsState()

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
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurface,
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
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new collection or folder",
                    modifier = Modifier.size(16.dp),
                    tint = colorScheme.onSurface,
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
                        showCollectionFromLinkDialog = true
                        importSource = ImportSource.ARTICLE
                    },
                    enabled = settings.value.openAiApiKeyValid,
                )

                DropdownMenuItem(
                    text = {  Row {
                        Text("Collection From Spotify Playlist")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowRight,
                            contentDescription = null
                        )
                    } },
                    onClick = {
                        showPlaylistsDropdown = true                    }
                )

                SpotifyPlaylistsSelector(
                    showPlaylistsSubmenu = showPlaylistsDropdown,
                    onDismiss = {
                        showPlaylistsDropdown = false
                        showDropdown = false
                    },
                    playlistSelectAction = { playlist ->
                        importSource = ImportSource.PLAYLIST
                        articleImportViewModel.getAlbumsFromPlaylist(playlist.id)
                        collectionNameSuggestion = playlist.name
                        showImportCollectionDialog = true
                    },
                    playlistByLinkAction = {
                        showDropdown = false
                        showCollectionFromLinkDialog = true
                        importSource = ImportSource.PLAYLIST
                    },
                    userPlaylists = userPlaylists
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
            collectionNameSuggestion = ""
        },
        confirmButtonText = "Create",
    )

    TextInputDialog(
        title = when (importSource) {
            ImportSource.PLAYLIST -> "Collection From Playlist"
            else -> "Collection From Article"
        },
        label = when (importSource) {
            ImportSource.PLAYLIST -> "Spotify Playlist URL or ID"
            else -> "Article URL"
        },
        placeholder = "Enter URL",
        isVisible = showCollectionFromLinkDialog,
        onDismiss = {
            showCollectionFromLinkDialog = false
            importSource = null
        },
        onConfirm = { input ->
            showImportCollectionDialog = true
            when (importSource) {
                ImportSource.PLAYLIST -> articleImportViewModel.getAlbumsFromPlaylist(input)
                ImportSource.ARTICLE -> articleImportViewModel.draftCollectionFromUrl(input)
                null -> {}
            }
        },
        confirmButtonText = "Create"
    )

    ImportCollectionDialog(
        isVisible = showImportCollectionDialog,
        onDismiss = { showImportCollectionDialog = false },
        onSearch = {
            when (importSource) {
                ImportSource.PLAYLIST -> {
                    Napier.d { "Shouldn't be here onSearch" }
                }
                ImportSource.ARTICLE -> articleImportViewModel.getAlbumsFromDraft()
                null -> {}
            }
        },
        onMakeCollection = { collectionName, addToLibrary ->
            if (importUiState is UiState.ReadyToImport) {
                viewModel.createCollection(collectionName)
                (importUiState as UiState.ReadyToImport).albums.forEach { album ->
                    album.let {
                        albumViewModel.addAlbumToCollection(it, collectionName, addToLibrary)
                    }
                }
                showImportCollectionDialog = false
            }
        },
        uiState = importUiState,
        initialValue = collectionNameSuggestion,
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
                navigator.navigateTo(Screen.Library)
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