package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import Playlist
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

@Composable
fun SpotifyPlaylistsSelector(
    showPlaylistsSubmenu: Boolean = false,
    onDismiss: () -> Unit,
    playlistSelectAction: (Playlist) -> Unit,
    playlistByLinkAction: () -> Unit,
    userPlaylists: List<Playlist>,
) {
    DropdownMenu(
        expanded = showPlaylistsSubmenu,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 180.dp, y = (-32).dp),
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = false
        )
    ) {

        DropdownMenuItem(
            text = { Text("Use A Link") },
            onClick = {
                playlistByLinkAction()
                onDismiss()
            }
        )

        HorizontalDivider()

        userPlaylists.forEach { playlist ->
            DropdownMenuItem(
                text = { Text(playlist.name) },
                onClick = {
                    playlistSelectAction(playlist)
                    onDismiss()
                }
            )
        }

    }
}