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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.ui.components.rating.StarRating
import androidx.compose.ui.window.PopupProperties
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState


@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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

    val density = LocalDensity.current
    
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .onSizeChanged { cardSize = it }
            .combinedClickable(
                onClick = onClick,
                onLongClick = { 
                    onContextMenu()
                    // For long press, center the menu on the card
                    contextMenuPosition = with(density) {
                        DpOffset(
                            x = (cardSize.width / 2).toDp(),
                            y = (cardSize.height / 2).toDp()
                        )
                    }
                    showContextMenu = true 
                }
            )
            .onPointerEvent(PointerEventType.Press) { pointerEvent ->
                if (pointerEvent.button == PointerButton.Secondary) {
                    onContextMenu()
                    // For right click, the position menu at mouse location
                    // Clamp the position to ensure it stays within the card bounds
                    val mouseX = pointerEvent.changes.first().position.x
                    val mouseY = pointerEvent.changes.first().position.y

                    contextMenuPosition = with(density) {
                        DpOffset(
                            x = mouseX.toDp().coerceIn(0.dp, (cardSize.width).toDp()),
                            y = mouseY.toDp().coerceIn(0.dp, (cardSize.height).toDp())
                        )
                    }
                    showContextMenu = true
                }
            }.hoverable(
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

                    if (isHovered) {
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
                        .fillMaxHeight()          // give the Box the whole remaining height
                ) {
                    // ─── Text + stars ────────────────────────────────
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(end = 12.dp) // keep text away from the heart
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
                                album.rating?.rating ?: 0,
                                starSpacing = 0.dp,
                                onRatingChange = onRatingChange,
                            )
                        }
                    }

                    // ─── Heart icon pinned to the real bottom‑right ──
                    HeartButton(
                        isInLibrary = album.album.inLibrary,
                        onToggle = onLibraryToggle,
                        modifier = Modifier
                            .align(Alignment.BottomEnd) // now flush to the edge
                            .padding(4.dp)               // small margin if you want it
                    )
                }
            }

            // Context menu positioned at mouse location
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
                offset = contextMenuPosition,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                    clippingEnabled = false // Allow the menu to extend beyond parent bounds
                )
            ) {
                contextMenuContent { showContextMenu = false }
            }
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 175.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums) { album ->
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
                onRatingChange = { rating ->
                    albumActions.updateRating(album, rating)
                },
                onArtistClick = { albumActions.navigateToArtist(album) },
                onLibraryToggle = { albumActions.toggleLibraryStatus(album) },
                showRating = showRating
            )
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
        modifier = modifier
            .size(30.dp) // put size here
    ) {
        Icon(
            imageVector = if (isInLibrary) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = if (isInLibrary) "Remove from Library" else "Add to Library",
            tint = if (isInLibrary) Color.Red.copy(alpha = 0.6f) else Color.White,
        )
    }
}