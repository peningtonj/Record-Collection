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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.ui.AppPlatform
import io.github.peningtonj.recordcollection.ui.LocalPlatform
import io.github.peningtonj.recordcollection.viewmodel.CollectionDetailViewModel
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
    val addToLibrarySetting = settings.collectionAddToLibrary[collectionName] ?: OnAddToCollection.DEFAULT

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
    val isAndroid = LocalPlatform.current == AppPlatform.ANDROID
    val shuffleActive = isShuffled && isPlayingFromCollection

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(horizontal = if (isAndroid) 12.dp else 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play button
        FilledIconButton(
            onClick = onPlayAll,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(22.dp)
            )
        }

        // Shuffle button
        IconButton(
            onClick = onShuffle,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = if (shuffleActive)
                    MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
        ) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (shuffleActive) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface
            )
        }

        // Name + album count, inline on one row
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = collectionName,
                style = if (isAndroid) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                text = "$albumCount ${if (albumCount == 1) "album" else "albums"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Settings button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Collection settings",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
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