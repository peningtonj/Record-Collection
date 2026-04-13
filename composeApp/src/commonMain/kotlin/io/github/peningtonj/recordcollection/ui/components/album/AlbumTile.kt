package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import io.github.peningtonj.recordcollection.ui.util.onRightClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.ui.components.rating.StarRating
import androidx.compose.ui.window.PopupProperties
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.AppPlatform
import io.github.peningtonj.recordcollection.ui.LocalPlatform


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactAlbumTile(
    album: AlbumDetailUiState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onContextMenu: () -> Unit = {},
    onRatingChange: (Int) -> Unit = {},
    onArtistClick: () -> Unit = {},
    contextMenuContent: @Composable (onDismiss: () -> Unit) -> Unit = { _ -> },
    onLibraryToggle: () -> Unit = {},
    showRating: Boolean = true
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(DpOffset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isAndroid = LocalPlatform.current == AppPlatform.ANDROID

    // On Android hover never fires — always show the play button so the tile is usable
    val showPlayButton = isHovered || isAndroid

    val density = LocalDensity.current

    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .onSizeChanged { cardSize = it }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { 
                    onContextMenu()
                    contextMenuPosition = with(density) {
                        DpOffset(
                            x = (cardSize.width / 2).toDp(),
                            y = (cardSize.height / 2).toDp()
                        )
                    }
                    showContextMenu = true 
                }
            )
            .onRightClick(density, cardSize) { position ->
                onContextMenu()
                contextMenuPosition = position
                showContextMenu = true
            }
            .hoverable(
                    interactionSource
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    AsyncImage(
                        model = album.album.images.firstOrNull()?.url ?: "",
                        contentDescription = "Album cover for ${album.album.name}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )

                    if (showPlayButton) {
                        PlayButton(
                            onPlayClick = onPlayClick,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = (-16).dp, y = (-16).dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = album.album.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(0.75f)
                        )

                        Text(
                            text = album.album.primaryArtist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .clickable { onArtistClick() }
                        )
                        if (showRating) {
                            StarRating(
                                album.rating ?: 0,
                                starSpacing = 0.dp,
                                onRatingChange = onRatingChange,
                            )
                        }
                    }

                    HeartButton(
                        isInLibrary = album.album.inLibrary,
                        onToggle = onLibraryToggle,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = contextMenuPosition,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = false
                )
            ) {
                contextMenuContent { showContextMenu = false }
            }
        }
    }
}

/** Spotify-style list row: small album art on the left, title/artist/rating on the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumListItem(
    album: AlbumDetailUiState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onPlayClick: () -> Unit = {},
    onContextMenu: () -> Unit = {},
    onRatingChange: (Int) -> Unit = {},
    onArtistClick: () -> Unit = {},
    contextMenuContent: @Composable (onDismiss: () -> Unit) -> Unit = { _ -> },
    onLibraryToggle: () -> Unit = {},
    showRating: Boolean = true,
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        onContextMenu()
                        showContextMenu = true
                    }
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Small square album art
            AsyncImage(
                model = album.album.images.firstOrNull()?.url ?: "",
                contentDescription = album.album.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )

            // Title + artist (+ optional rating)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.album.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.album.primaryArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onArtistClick() }
                )
                if (showRating && (album.rating ?: 0) > 0) {
                    StarRating(
                        album.rating ?: 0,
                        starSpacing = 0.dp,
                        onRatingChange = onRatingChange
                    )
                }
            }

            // Play button
            IconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play ${album.album.name}",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Library heart
            HeartButton(
                isInLibrary = album.album.inLibrary,
                onToggle = onLibraryToggle
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            contextMenuContent { showContextMenu = false }
        }
    }
}

@Composable
fun AlbumGrid(
    albums: List<AlbumDetailUiState>,
    modifier: Modifier = Modifier,
    albumActions: AlbumActions,
    collectionAlbumActions: CollectionAlbumActions? = null,
    showRating: Boolean = true
) {
    val isAndroid = LocalPlatform.current == AppPlatform.ANDROID

    if (isAndroid) {
        // Spotify-style list on Android
        LazyColumn(modifier = modifier) {
            items(albums) { album ->
                AlbumListItem(
                    album = album,
                    onClick = { albumActions.navigateToPage(album) },
                    onPlayClick = { collectionAlbumActions?.playFromCollection(album) ?: albumActions.play(album) },
                    onContextMenu = { albumActions.showContextMenu(album) },
                    contextMenuContent = { onDismiss ->
                        DefaultAlbumContextMenu(
                            album = album,
                            actions = albumActions,
                            onDismiss = onDismiss,
                            collectionAlbumActions = collectionAlbumActions
                        )
                    },
                    onRatingChange = { rating -> albumActions.updateRating(album, rating) },
                    onArtistClick = { albumActions.navigateToArtist(album) },
                    onLibraryToggle = { albumActions.toggleLibraryStatus(album) },
                    showRating = showRating
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        // Desktop grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 175.dp),
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            gridItems(albums) { album ->
                CompactAlbumTile(
                    album = album,
                    onClick = { albumActions.navigateToPage(album) },
                    onPlayClick = { collectionAlbumActions?.playFromCollection(album) ?: albumActions.play(album) },
                    onContextMenu = { albumActions.showContextMenu(album) },
                    contextMenuContent = { onDismiss ->
                        DefaultAlbumContextMenu(
                            album = album,
                            actions = albumActions,
                            onDismiss = onDismiss,
                            collectionAlbumActions = collectionAlbumActions
                        )
                    },
                    onRatingChange = { rating -> albumActions.updateRating(album, rating) },
                    onArtistClick = { albumActions.navigateToArtist(album) },
                    onLibraryToggle = { albumActions.toggleLibraryStatus(album) },
                    showRating = showRating
                )
            }
        }
    }
}

@Composable
fun HeartButton(
    isInLibrary: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(30.dp)
    ) {
        Icon(
            imageVector = if (isInLibrary) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isInLibrary) "Remove from Library" else "Add to Library",
            tint = if (isInLibrary) Color.Red.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

