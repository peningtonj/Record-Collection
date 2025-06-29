// commonMain/navigation/NavigationEvent.kt
package io.github.peningtonj.recordcollection.navigation

sealed interface NavigationEvent {
    data class NavigateTo(val screen: Screen) : NavigationEvent
    data object NavigateBack : NavigationEvent
    data class PopUpTo(
        val screen: Screen,
        val inclusive: Boolean = false
    ) : NavigationEvent
}