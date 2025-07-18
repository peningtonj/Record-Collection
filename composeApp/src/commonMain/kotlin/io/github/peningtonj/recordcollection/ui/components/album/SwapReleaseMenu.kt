package io.github.peningtonj.recordcollection.ui.components.album

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.github.peningtonj.recordcollection.db.domain.Album
import kotlin.collections.forEach


@Composable
fun SwapReleaseMenu(
    releases: List<Album>,
    swapRelease: (Album) -> Unit,
    onDismiss: () -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box {
        DropdownMenuItem(
            text = {
                Row {
                    Text("Swap Album Release")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null)
                }
            },
            onClick = { showDropdown = true },
            leadingIcon = {
                Icon(Icons.Default.SwapVerticalCircle, contentDescription = null)
            }
        )

        SwapReleaseDropdownContent(
            expanded = showDropdown,
            onDismissRequest = {
                showDropdown = false
                onDismiss()
            },
            releases = releases,
            swapRelease = swapRelease
        )
    }
}

@Composable
fun SwapReleaseDropdownContent(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    releases: List<Album>,
    swapRelease: (Album) -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 0.dp),
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            clippingEnabled = false
        )
    ) {
        releases.forEach { release ->
            DropdownMenuItem(
                text = { Text(release.name) },
                onClick = {
                    swapRelease(release)
                    onDismissRequest()
                }
            )
        }
    }
}