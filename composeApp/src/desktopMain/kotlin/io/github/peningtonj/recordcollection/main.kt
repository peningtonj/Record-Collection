package io.github.peningtonj.recordcollection

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.peningtonj.recordstore.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "recordcollection",
    ) {
        App()
    }
}