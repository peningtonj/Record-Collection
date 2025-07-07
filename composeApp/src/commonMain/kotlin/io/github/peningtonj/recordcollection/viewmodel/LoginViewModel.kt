package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: SpotifyAuthRepository,
    private val navigator: Navigator
) : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    val authState = _authState.asStateFlow()

    fun startAuth() {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating
            authRepository.authenticate()
                .onSuccess {
                    _authState.value = AuthState.Authenticated(it)
                    navigator.navigateTo(Screen.Library)
                }
                .onFailure { error ->
                    _authState.value = AuthState.Error(error.message ?: "Authentication failed")
                }
        }
    }
}

@Composable
fun rememberLoginViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current,
    navigator: Navigator = LocalNavigator.current
) = remember(dependencies, navigator) {
    LoginViewModel(
        authRepository = dependencies.authRepository,
        navigator = navigator
    )
}