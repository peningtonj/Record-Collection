package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: SpotifyAuthRepository,
    private val navigator: Navigator
) : ViewModel() {
    data class LoginUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val showRetry: Boolean = false
    )

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    // Expose repository auth state for login status
    val authState = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.NotAuthenticated
        )

    init {
        // Handle navigation when authenticated
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    navigateToLibrary()
                }
            }
        }
    }

    fun startAuth() {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            
            authRepository.authenticate()
                .onFailure { error ->
                    _uiState.value = LoginUiState(
                        error = error.message ?: "Authentication failed",
                        showRetry = true
                    )
                }
        }
    }

    private fun navigateToLibrary() {
        navigator.navigateTo(Screen.Library)
    }
}
