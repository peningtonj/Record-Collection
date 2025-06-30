
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
    
    LaunchedEffect(isAuthenticated) {
        Napier.d("Checking if already authenticated")

        when (isAuthenticated) {
            true -> {
                // If we're on the login screen, navigate to library
                navigator.navigateTo(Screen.Library)
            }
            false -> {
                // If we're not on the login screen, navigate to login
                if (navigator.currentRoute != Screen.Login.route) {
                    navigator.navigateTo(Screen.Login)
                }
            }
            null -> {
                // Still checking auth state, could show a splash screen here
            }
        }
    }
    
    content()
}