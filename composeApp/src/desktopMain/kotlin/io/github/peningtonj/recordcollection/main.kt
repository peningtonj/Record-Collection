package io.github.peningtonj.recordcollection

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.di.DependencyContainerFactory
import io.github.peningtonj.recordcollection.navigation.DesktopNavigator
import io.github.peningtonj.recordcollection.util.FilteredConsoleAntilog
import io.github.peningtonj.recordcollection.util.FirebaseFileAntilog

fun main() {
    // Initialise logging before anything else so every message — including
    // Firebase boot-time logs — is captured and routed correctly.
    Napier.base(FilteredConsoleAntilog()) // console: all categories except Firebase debug
    Napier.base(FirebaseFileAntilog())    // file:    Firebase-tagged messages only

    application {
        val dependencies = remember { DependencyContainerFactory.create() }
        val navigator = remember { DesktopNavigator() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Record Collection",
            state = WindowState(
                width = 1200.dp,
                height = 800.dp
            )
        ) {
            App(dependencies, navigator)
        }
    }
}