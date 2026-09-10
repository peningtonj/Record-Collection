package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        viewModelScope.launch {
            authState.collect { state ->
                // Navigation is owned by AuthNavigationWrapper — this VM only reflects
                // auth state into the login screen's own UI. Surfacing an Error (e.g. a
                // token refresh that failed mid-session and bounced the user here) keeps
                // the screen from being a dead end — the retry button drives startAuth().
                if (state is AuthState.Error) {
                    _uiState.update { it.copy(isLoading = false, error = state.message, showRetry = true) }
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
}
