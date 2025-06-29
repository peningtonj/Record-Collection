// commonMain/navigation/NavigationHost.kt
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun NavigationHost(
    startScreen: Screen,
    navigator: Navigator,
    content: @Composable (Screen) -> Unit
)