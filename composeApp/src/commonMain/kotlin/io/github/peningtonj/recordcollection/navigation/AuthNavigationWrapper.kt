
package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.viewmodel.AuthViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAuthViewModel

@Composable
fun AuthNavigationWrapper(
    authViewModel: AuthViewModel = rememberAuthViewModel(),
    content: @Composable () -> Unit
) {
    val navigator = LocalNavigator.current
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val currentScreen by navigator.currentScreen.collectAsState()
    
    LaunchedEffect(isAuthenticated) {
        Napier.d("Auth state changed: isAuthenticated=$isAuthenticated, currentScreen=$currentScreen")

        when (isAuthenticated) {
            true -> {
                // Only navigate to library if we're currently on the login screen
                if (currentScreen == Screen.Login) {
                    Napier.d("User authenticated, navigating from Login to Library")
                    navigator.navigateTo(Screen.Library)
                }
            }
            false -> {
                // If not authenticated and not on login screen, go to login
                if (currentScreen != Screen.Login) {
                    Napier.d("User not authenticated, navigating to Login")
                    navigator.navigateTo(Screen.Login)
                }
            }
        }
    }
    
    content()
}