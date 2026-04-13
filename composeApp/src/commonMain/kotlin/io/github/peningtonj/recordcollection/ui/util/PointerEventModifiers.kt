package io.github.peningtonj.recordcollection.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize

/**
 * Attaches a right-click (secondary-button) listener on desktop.
 * On Android this is a no-op – long-press via combinedClickable handles the
 * context-menu trigger instead.
 */
expect fun Modifier.onRightClick(
    density: Density,
    cardSize: IntSize,
    onRightClick: (position: DpOffset) -> Unit
): Modifier

