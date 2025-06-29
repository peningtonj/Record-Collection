package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun NavigationHost(
    startScreen: Screen,
    navigator: Navigator,
    content: @Composable ((Screen) -> Unit)
) {
}