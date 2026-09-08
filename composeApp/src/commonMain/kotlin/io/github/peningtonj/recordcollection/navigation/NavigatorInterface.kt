// commonMain/navigation/Navigator.kt
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.StateFlow

interface Navigator {
    fun navigate(event: NavigationEvent)
    val currentScreen: StateFlow<Screen>
    val currentRoute: String?
    val canNavigateBack: StateFlow<Boolean>

    /**
     * The [ViewModelStoreOwner] scoped to [screen]'s back-stack entry. Screen-scoped
     * ViewModels are cleared when the screen is popped — see [ScreenViewModelStores].
     */
    fun viewModelStoreOwnerFor(screen: Screen): ViewModelStoreOwner


    // Convenience methods
    fun navigateTo(screen: Screen) = navigate(NavigationEvent.NavigateTo(screen))
    fun navigateBack() = navigate(NavigationEvent.NavigateBack)
    fun popUpTo(screen: Screen, inclusive: Boolean = false) = 
        navigate(NavigationEvent.PopUpTo(screen, inclusive))


}

val LocalNavigator = staticCompositionLocalOf<Navigator> { 
    error("No Navigator provided") 
}