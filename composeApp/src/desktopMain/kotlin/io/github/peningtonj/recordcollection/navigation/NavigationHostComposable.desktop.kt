package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.aakira.napier.Napier

@Composable
actual fun NavigationHost(
    startScreen: Screen,
    navigator: Navigator,
    content: @Composable ((Screen) -> Unit)
) {
    Napier.d("Desktop NavigationHost started with startScreen: $startScreen")

    val currentScreen by navigator.currentScreen.collectAsState()
    Napier.d("Desktop current screen is: $currentScreen")

    // Scope the rendered screen's ViewModels to a per-screen ViewModelStore so
    // they are cleared (and their coroutines/Firestore listeners cancelled) on pop.
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides navigator.viewModelStoreOwnerFor(currentScreen)
    ) {
        content(currentScreen)
    }
}
