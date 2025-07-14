package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
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
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.ui.components.rating.StarRating
import io.github.peningtonj.recordcollection.ui.components.rating.StarRatingDisplay
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
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
    contextMenuContent: @Composable (onDismiss: () -> Unit) -> Unit = { _ -> }
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(DpOffset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    
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
                    // For right click, position menu at mouse location
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
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = album.album.images.firstOrNull()?.url ?: "",
                    contentDescription = "Album cover for ${album.album.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                ) {
                    Column {
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
                            modifier = Modifier.fillMaxWidth(0.75f)
                                .clickable { onArtistClick() }
                        )

                        StarRating(
                            album.rating?.rating ?: 0,
                            starSpacing = 0.dp,
                            onRatingChange = onRatingChange,
                        )

                    }

                    PlayButton(
                        onPlayClick = onPlayClick,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.CenterEnd)
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
                    clippingEnabled = false // Allow menu to extend beyond parent bounds
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
    onAlbumClick: (AlbumDetailUiState) -> Unit = {},
    onPlayClick: (AlbumDetailUiState) -> Unit,
    onContextMenu: (AlbumDetailUiState) -> Unit = {},
    onRatingChange: (AlbumDetailUiState, Int) -> Unit,
    onArtistClick: (AlbumDetailUiState) -> Unit = {},
    contextMenuContent: @Composable (album: AlbumDetailUiState, onDismiss: () -> Unit) -> Unit = { _, _ -> }
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
                onClick = { onAlbumClick(album) },
                onPlayClick = { onPlayClick(album) },
                onContextMenu = { onContextMenu(album) },
                contextMenuContent = { onDismiss -> 
                    contextMenuContent(album, onDismiss)
                },
                onRatingChange = { rating ->
                    onRatingChange(album, rating)
                },
                onArtistClick = { onArtistClick(album) }
            )
        }
    }
}