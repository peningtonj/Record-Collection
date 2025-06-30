package io.github.peningtonj.recordcollection.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.navigation.*
import io.github.peningtonj.recordcollection.ui.screens.LibraryScreen
import io.github.peningtonj.recordcollection.ui.screens.LoginScreen
import io.github.peningtonj.recordcollection.ui.screens.ProfileScreen

@Composable
fun RecordCollectionApp(
    navigator: Navigator,
) {
    Napier.d("RecordCollectionApp composable started")

    CompositionLocalProvider(LocalNavigator provides navigator) {
        AuthNavigationWrapper {
            NavigationHost(
                startScreen = Screen.Login,
                navigator = navigator
            ) { screen ->
                Napier.d("NavigationHost rendering screen: $screen")
                when (screen) {
                    Screen.Login -> LoginScreen()
                    Screen.Profile -> ProfileScreen()
                    Screen.Library -> LibraryScreen()
                }
            }
        }
    }
}