package io.github.peningtonj.recordcollection

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.di.DependencyContainerFactory
import io.github.peningtonj.recordcollection.navigation.DesktopNavigator

fun main() = application {
    val dependencies = DependencyContainerFactory.create()
    val navigator = remember { DesktopNavigator() }
    Napier.base(DebugAntilog())

    Window(
        onCloseRequest = ::exitApplication,
        title = "Record Collection",
    ) {
        App(dependencies, navigator)
    }
}
