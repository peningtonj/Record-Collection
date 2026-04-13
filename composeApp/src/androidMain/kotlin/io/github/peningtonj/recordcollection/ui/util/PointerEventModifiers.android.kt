package io.github.peningtonj.recordcollection.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize

// Android has no secondary mouse button; long-press via combinedClickable handles context menus.
actual fun Modifier.onRightClick(
    density: Density,
    cardSize: IntSize,
    onRightClick: (position: DpOffset) -> Unit
): Modifier = this

