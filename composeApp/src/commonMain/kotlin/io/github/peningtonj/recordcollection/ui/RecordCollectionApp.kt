// commonMain/ui/RecordCollectionApp.kt
package io.github.peningtonj.recordcollection.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.peningtonj.recordcollection.navigation.*
import io.github.peningtonj.recordcollection.ui.screens.LibraryScreen
import io.github.peningtonj.recordcollection.ui.screens.LoginScreen
import io.github.peningtonj.recordcollection.ui.screens.ProfileScreen
import io.github.aakira.napier.Napier

@Composable
fun RecordCollectionApp(navigator: Navigator) {
    Napier.d("RecordCollectionApp composable started")
    CompositionLocalProvider(LocalNavigator provides navigator) {
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