package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelStoreOwner
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Same in-memory stack model as [io.github.peningtonj.recordcollection.navigation.DesktopNavigator]
 * (there's no desktop-specific code in it). URL/history binding is Phase 4 — see
 * docs/WEB_TARGET_PLAN.md § 8.
 */
@Stable
class WebNavigator : Navigator {

    private val _currentScreenFlow = MutableStateFlow<Screen>(Screen.Login)
    override val currentScreen: StateFlow<Screen> = _currentScreenFlow.asStateFlow()
    override val currentRoute: String? = null
    private val _backStack = mutableStateOf(emptyList<Screen>())

    private val viewModelStores = ScreenViewModelStores()
    override fun viewModelStoreOwnerFor(screen: Screen): ViewModelStoreOwner =
        viewModelStores.ownerFor(screen)

    private val _canNavigateBack = MutableStateFlow(false)
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    override fun navigate(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateTo -> {
                _backStack.value = _backStack.value + currentScreen.value
                _currentScreenFlow.value = event.screen
            }
            NavigationEvent.NavigateBack -> {
                if (_backStack.value.isNotEmpty()) {
                    _currentScreenFlow.value = _backStack.value.last()
                    _backStack.value = _backStack.value.dropLast(1)
                }
            }
            is NavigationEvent.PopUpTo -> {
                val targetIndex = _backStack.value.indexOf(event.screen)
                if (targetIndex != -1) {
                    _backStack.value = _backStack.value.take(targetIndex + 1)
                    if (event.inclusive) _backStack.value = _backStack.value.dropLast(1)
                }
            }
        }
        _canNavigateBack.value = _backStack.value.size > 1
        viewModelStores.retainOnly(_backStack.value.toSet() + _currentScreenFlow.value)
        Napier.d("WebNavigator -> ${_currentScreenFlow.value}")
    }
}
