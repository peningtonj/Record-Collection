package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.aakira.napier.Napier

@Composable
actual fun NavigationHost(
    startScreen: Screen,
    navigator: Navigator,
    content: @Composable ((Screen) -> Unit)
) {
    Napier.d("Desktop NavigationHost started with startScreen: $startScreen")

    val currentScreen by navigator.currentScreen.collectAsState()
    Napier.d("Desktop current screen is: $currentScreen")

    content(currentScreen)
}