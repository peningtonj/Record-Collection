package io.github.peningtonj.recordcollection.ui.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize

// Same as desktop — the web canvas delivers secondary-button pointer events.
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onRightClick(
    density: Density,
    cardSize: IntSize,
    onRightClick: (position: DpOffset) -> Unit
): Modifier = this.onPointerEvent(PointerEventType.Press) { pointerEvent ->
    if (pointerEvent.button == PointerButton.Secondary) {
        val change = pointerEvent.changes.first()
        val position = with(density) {
            DpOffset(change.position.x.toDp(), change.position.y.toDp())
        }
        onRightClick(position)
    }
}
