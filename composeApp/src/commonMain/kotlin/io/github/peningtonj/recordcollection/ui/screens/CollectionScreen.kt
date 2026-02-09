package io.github.peningtonj.recordcollection.ui.screens

/**
 * Collection Screen - Displays a collection of albums with comprehensive actions
 * 
 * ## Core Features:
 * 1. Display collection name and album count
 * 2. Play all albums in order
 * 3. Shuffle and play all albums
 * 4. Configure auto-add-to-library behavior
 * 
 * ## Album Actions (via AlbumGrid):
 * - Navigate to album detail page
 * - Navigate to artist page
 * - Play album
 * - Rate album (1-10)
 * - Add/remove from library
 * - Add to other collections
 * - Add tags
 * - Get release group variants
 * - Add all tracks to saved songs
 * 
 * ## Collection-Specific Actions:
 * - Remove album from collection
 * - Swap album with different release
 * - Play album from collection (maintains queue context)
 * 
 * ## Settings:
 * - Auto add to library: Default/Always/Never
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.components.album.AlbumGrid
import io.github.peningtonj.recordcollection.ui.components.album.getCollectionActionAlbums
import io.github.peningtonj.recordcollection.ui.components.album.rememberAlbumActions
import io.github.peningtonj.recordcollection.ui.components.common.SettingsChipRow
import io.github.peningtonj.recordcollection.viewmodel.*

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
    var showSettings by remember { mutableStateOf(false) }
    
    // Actions
    val albumActions = rememberAlbumActions(
        playbackViewModel = playbackViewModel,
        albumViewModel = albumViewModel,
        libraryViewModel = libraryViewModel,
        collectionsViewModel = collectionsViewModel,
        settings = settingsViewModel,
        navigator = navigator,
    )
    
    val collectionAlbumActions = getCollectionActionAlbums(
        collection = uiState.collection,
        playbackViewModel = playbackViewModel,
        albumViewModel = albumViewModel,
        collectionDetailViewModel = viewModel,
    )
    
    // Playback state
    val currentSession by playbackViewModel.currentSession.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val isShuffled = currentSession?.isShuffled ?: false
    val isPlayingFromCollection = currentSession?.playingFrom?.name == collectionName
    
    // Settings
    val settings by settingsViewModel.settings.collectAsState()
    val addToLibrarySetting = settings.collectionAddToLibrary.getOrDefault(
        collectionName, 
        OnAddToCollection.DEFAULT
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Header with gradient background
            HeroCollectionHeader(
                collectionName = collectionName,
                albumCount = uiState.albums.size,
                isShuffled = isShuffled,
                isPlayingFromCollection = isPlayingFromCollection,
                onPlayAll = {
                    val albums = uiState.albums
                    if (albums.isNotEmpty()) {
                        playbackViewModel.playAlbum(
                            album = albums.first(),
                            queue = albums.drop(1),
                            collection = uiState.collection
                        )
                    }
                },
                onShuffle = {
                    if (playbackState?.isPlaying == true && isPlayingFromCollection) {
                        playbackViewModel.toggleShuffle(uiState.albums)
                    } else {
                        val shuffled = uiState.albums.shuffled()
                        playbackViewModel.playAlbum(
                            album = shuffled.first(),
                            queue = shuffled.drop(1),
                            collection = uiState.collection,
                            isShuffled = true
                        )
                    }
                },
                onSettingsClick = { showSettings = !showSettings }
            )
            
            // Albums Grid
            AlbumGrid(
                albums = uiState.albums,
                albumActions = albumActions,
                collectionAlbumActions = collectionAlbumActions,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
        
        // Settings Overlay
        if (showSettings) {
            CollectionSettingsOverlay(
                addToLibrarySetting = addToLibrarySetting,
                onSettingChange = { newSetting ->
                    settingsViewModel.updateOnAddToLibrarySetting(collectionName, newSetting)
                },
                onDismiss = { showSettings = false }
            )
        }
    }
}

@Composable
private fun HeroCollectionHeader(
    collectionName: String,
    albumCount: Int,
    isShuffled: Boolean,
    isPlayingFromCollection: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
            .padding(32.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Collection icon badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "COLLECTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Collection name
                    Text(
                        text = collectionName,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Album count with styled number
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = albumCount.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (albumCount == 1) "album" else "albums",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                // Settings button
                IconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Collection settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Action buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Play button
                Button(
                    onClick = onPlayAll,
                    modifier = Modifier.height(56.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Shuffle button
                OutlinedButton(
                    onClick = onShuffle,
                    modifier = Modifier.height(56.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    border = if (isShuffled && isPlayingFromCollection) {
                        androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.outlinedButtonBorder
                    },
                    colors = if (isShuffled && isPlayingFromCollection) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionSettingsOverlay(
    addToLibrarySetting: OnAddToCollection,
    onSettingChange: (OnAddToCollection) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 8.dp,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collection Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                }
                
                Divider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Library Behavior",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "Choose when albums added to this collection should be automatically added to your library:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OnAddToCollection.entries.forEach { option ->
                            SettingOptionCard(
                                label = option.displayName,
                                description = when (option) {
                                    OnAddToCollection.DEFAULT -> "Use global setting"
                                    OnAddToCollection.TRUE -> "Always add to library"
                                    OnAddToCollection.FALSE -> "Never add to library"
                                },
                                selected = option == addToLibrarySetting,
                                onClick = { onSettingChange(option) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingOptionCard(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        },
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}