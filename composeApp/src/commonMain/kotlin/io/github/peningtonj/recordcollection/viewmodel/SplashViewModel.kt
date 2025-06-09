package io.github.peningtonj.recordcollection.viewmodel

import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SplashViewModel(

) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotAuthenticated)
    private val _profileState = MutableStateFlow<SpotifyProfile?>(null)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    val profileState: StateFlow<SpotifyProfile?> = _profileState.asStateFlow()

    private val spotifyApi = SpotifyApi(HttpClientProvider.create())

    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val authHandler = AuthHandler()

    fun startAuth() {
        viewModelScope.launch {
            _authState.value = AuthState.Authenticating

            authHandler.authenticate()
                .onSuccess { code ->
                    // Here you would typically exchange the code for an access token
                    authHandler.exchangeCodeForToken(code)
                        .onSuccess { accessToken ->
                            onAuthSuccess(accessToken)
                        }
                        .onFailure { error ->
                            onAuthError(error.message ?: "Exchange code failed")
                        }

                }
                    // For now, we'll just pass the code

                .onFailure { error ->
                    onAuthError(error.message ?: "Authentication failed")
                }
        }
    }


    fun onAuthSuccess(token: AccessToken) {
        _authState.value = AuthState.Authenticated(token)
        // Fetch profile after successful authentication
        viewModelScope.launch {
            spotifyApi.getCurrentUserProfile(token.accessToken)
                .onSuccess { profile ->
                    _profileState.value = profile
                }
                .onFailure { error ->
                    onAuthError(error.message ?: "Failed to fetch profile")
                }
        }
    }

    fun onAuthError(error: String) {
        _authState.value = AuthState.Error(error)
    }
    
    fun clear() {
        viewModelScope.cancel()
    }
}