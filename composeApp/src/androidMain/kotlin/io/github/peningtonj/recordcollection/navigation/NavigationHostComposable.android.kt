package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
actual fun NavigationHost(
    startScreen: Screen,
    navigator: Navigator,
    content: @Composable ((Screen) -> Unit)
) {
    val currentScreen by navigator.currentScreen.collectAsState()
    content(currentScreen)
}