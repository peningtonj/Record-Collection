// commonMain/navigation/Navigator.kt
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Navigator {
    fun navigate(event: NavigationEvent)
    val currentScreen: StateFlow<Screen>
    val currentRoute: String?
    val canNavigateBack: StateFlow<Boolean>


    // Convenience methods
    fun navigateTo(screen: Screen) = navigate(NavigationEvent.NavigateTo(screen))
    fun navigateBack() = navigate(NavigationEvent.NavigateBack)
    fun popUpTo(screen: Screen, inclusive: Boolean = false) = 
        navigate(NavigationEvent.PopUpTo(screen, inclusive))


}

val LocalNavigator = staticCompositionLocalOf<Navigator> { 
    error("No Navigator provided") 
}