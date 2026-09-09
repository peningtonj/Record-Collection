package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
actual fun NavigationHost(
    startScreen: Screen,
    navigator: Navigator,
    content: @Composable ((Screen) -> Unit)
) {
    val currentScreen by navigator.currentScreen.collectAsState()

    // Per-screen ViewModelStore so ViewModels (and their Firestore listeners) are cleared on pop.
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides navigator.viewModelStoreOwnerFor(currentScreen)
    ) {
        content(currentScreen)
    }
}
