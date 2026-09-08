package io.github.peningtonj.recordcollection

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.ui.RecordCollectionApp

// commonMain/ui/App.kt
@Composable
fun App(dependencyContainer: DependencyContainer, navigator: Navigator) {
    // App-session-scoped store for the always-on ViewModels (playback, search,
    // settings, auth). Per-screen ViewModels get their own store from the navigator
    // (see NavigationHost). Cleared when the whole app composition leaves.
    val appViewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(Unit) {
        onDispose { appViewModelStoreOwner.viewModelStore.clear() }
    }

    MaterialTheme {
        CompositionLocalProvider(
            LocalDependencyContainer provides dependencyContainer,
            LocalNavigator provides navigator,
            LocalViewModelStoreOwner provides appViewModelStoreOwner,
        ) {
            RecordCollectionApp(navigator)
        }
    }
}
