package io.github.peningtonj.recordcollection

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.di.DependencyContainerFactory
import io.github.peningtonj.recordstore.App

fun main() = application {
    val dependencies = DependencyContainerFactory.create()
    Napier.base(DebugAntilog())

    Window(
        onCloseRequest = ::exitApplication,
        title = "recordcollection",
    ) {
        App(dependencies)
    }
}