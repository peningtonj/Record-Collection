package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
class AndroidNavigator : Navigator {

    private val _currentScreenFlow = MutableStateFlow<Screen>(Screen.Login)
    override val currentScreen: StateFlow<Screen> = _currentScreenFlow.asStateFlow()
    override val currentRoute: String? = null

    private val _backStack = mutableListOf<Screen>()

    private val _canNavigateBack = MutableStateFlow(false)
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    override fun navigate(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateTo -> {
                _backStack.add(_currentScreenFlow.value)
                _currentScreenFlow.value = event.screen
            }
            NavigationEvent.NavigateBack -> {
                if (_backStack.isNotEmpty()) {
                    _currentScreenFlow.value = _backStack.removeLast()
                }
            }
            is NavigationEvent.PopUpTo -> {
                val targetIndex = _backStack.indexOf(event.screen)
                if (targetIndex != -1) {
                    val trimmed = _backStack.take(targetIndex + 1).toMutableList()
                    _backStack.clear()
                    _backStack.addAll(trimmed)
                    if (event.inclusive && _backStack.isNotEmpty()) {
                        _backStack.removeLast()
                    }
                }
                _currentScreenFlow.value = _backStack.lastOrNull() ?: Screen.Login
            }
        }
        _canNavigateBack.value = _backStack.size > 1
    }
}

