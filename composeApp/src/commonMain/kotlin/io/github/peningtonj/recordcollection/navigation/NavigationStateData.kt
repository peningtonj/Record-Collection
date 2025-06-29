// commonMain/navigation/NavigationState.kt
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Stable

@Stable
data class NavigationState(
    val currentScreen: Screen,
    val backStack: List<Screen> = emptyList()
)