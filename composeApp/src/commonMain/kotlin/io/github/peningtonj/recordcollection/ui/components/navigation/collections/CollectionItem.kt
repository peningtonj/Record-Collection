package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CollectionItem(
    collection: AlbumCollection,
    isSelected: Boolean,
    onClick: () -> Unit,
    onContextMenu: () -> Unit = {},
    contextMenuContent: @Composable (onDismiss: () -> Unit) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(DpOffset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    
    val density = LocalDensity.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
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
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    clippingEnabled = false
                )
            ) {
                contextMenuContent { showContextMenu = false }
            }
        }
    }
}