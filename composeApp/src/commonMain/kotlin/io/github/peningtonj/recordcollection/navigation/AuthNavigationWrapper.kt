package io.github.peningtonj.recordcollection.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.github.peningtonj.recordcollection.viewmodel.AuthViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberAuthViewModel

/**
 * The screen [authState] should force us to, or `null` to leave navigation untouched.
 *
 * Every token refresh re-emits `Authenticated` (with a brief `Authenticating` in
 * between), so the rules are deliberately narrow:
 *  - `Authenticated` only routes to Library when we're actually *on* Login (first
 *    sign-in, or app start with a cached token). A refresh while the user is deep in the
 *    app returns `null` — it must not yank them back to Library.
 *  - `Authenticating` is transient — always `null`.
 *  - `NotAuthenticated` / `Error` routes to Login so a dead session isn't a silent dead
 *    end (unless we're already there).
 */
internal fun authNavTarget(authState: AuthState, currentScreen: Screen): Screen? = when (authState) {
    is AuthState.Authenticated -> Screen.Library.takeIf { currentScreen == Screen.Login }
    AuthState.NotAuthenticated, is AuthState.Error -> Screen.Login.takeIf { currentScreen != Screen.Login }
    AuthState.Authenticating -> null
}

/** Owns the login-screen ↔ app transition. The only place auth state drives navigation. */
@Composable
fun AuthNavigationWrapper(
    authViewModel: AuthViewModel = rememberAuthViewModel(),
    content: @Composable () -> Unit
) {
    val navigator = LocalNavigator.current
    val authState by authViewModel.authState.collectAsState()
    val currentScreen by navigator.currentScreen.collectAsState()

    LaunchedEffect(authState) {
        authNavTarget(authState, currentScreen)?.let { target ->
            Napier.d("Auth $authState on $currentScreen → $target")
            navigator.navigateTo(target)
        }
    }

    content()
}
