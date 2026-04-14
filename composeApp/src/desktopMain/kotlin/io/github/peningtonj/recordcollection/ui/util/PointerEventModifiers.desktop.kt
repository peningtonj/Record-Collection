package io.github.peningtonj.recordcollection.ui.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onRightClick(
    density: Density,
    cardSize: IntSize,
    onRightClick: (position: DpOffset) -> Unit
): Modifier = this.onPointerEvent(PointerEventType.Press) { pointerEvent ->
    if (pointerEvent.button == PointerButton.Secondary) {
        val mouseX = pointerEvent.changes.first().position.x
        val mouseY = pointerEvent.changes.first().position.y
        val position = with(density) {
            // Position is used to offset a zero-size anchor Box within the card,
            // so just pass the raw cursor coordinates within the component.
            DpOffset(
                x = mouseX.toDp(),
                y = mouseY.toDp()
            )
        }
        onRightClick(position)
    }
}

