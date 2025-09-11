// desktopMain/navigation/DesktopNavigator.kt
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
class DesktopNavigator : Navigator {

    private val _currentScreenFlow = MutableStateFlow<Screen>(Screen.Login)
    override val currentScreen: StateFlow<Screen> = _currentScreenFlow.asStateFlow()
    override val currentRoute: String? = null
    private val _backStack = mutableStateOf(emptyList<Screen>())

    init {
        Napier.d("DesktopNavigator initialized with Login screen")
    }

    private val _canNavigateBack = MutableStateFlow(false)
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    override fun navigate(event: NavigationEvent) {
        Napier.d("Navigation event received: $event")
        when (event) {
            is NavigationEvent.NavigateTo -> {
                Napier.d("Navigating to screen: ${event.screen}")
                _backStack.value = _backStack.value + currentScreen.value
                _currentScreenFlow.value = event.screen
            }
            NavigationEvent.NavigateBack -> {
                Napier.d("Navigating back. Current backstack size: ${_backStack.value.size}")
                if (_backStack.value.isNotEmpty()) {
                    _currentScreenFlow.value = _backStack.value.last()
                    _backStack.value = _backStack.value.dropLast(1)
                }
            }
            is NavigationEvent.PopUpTo -> {
                Napier.d("Pop up to screen: ${event.screen}")
                val targetIndex = _backStack.value.indexOf(event.screen)
                if (targetIndex != -1) {
                    _backStack.value = _backStack.value.take(targetIndex + 1)
                    if (event.inclusive) {
                        _backStack.value = _backStack.value.dropLast(1)
                    }
                }
            }
        }
        _canNavigateBack.value = _backStack.value.size > 1

    }
}