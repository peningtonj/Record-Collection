package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseSpotifyAuthRepository(
    protected val authHandler: AuthHandler
) {
    protected val _authState = MutableStateFlow<AuthState>(loadInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    protected abstract fun loadInitialState(): AuthState
    protected abstract fun saveToken(token: AccessToken)
    protected abstract fun getStoredToken(): AccessToken?
    protected abstract fun isTokenValid(): Boolean
    protected abstract fun hasRefreshToken(): Boolean
    protected abstract fun getRefreshToken(): String?

    suspend fun ensureValidToken(): Result<AccessToken> {
        val currentState = _authState.value

        return when {
            currentState is AuthState.Authenticated && !isTokenValid() -> {
                Result.success(currentState.accessToken)
            }
            hasRefreshToken() -> {
                refreshToken()
                    .recoverCatching {
                        authenticate().getOrThrow()
                    }
            }
            else -> {
                authenticate()
            }
        }
    }

    private suspend fun refreshToken(): Result<AccessToken> {
        val refreshToken = getRefreshToken() ?: return Result.failure(IllegalStateException("No refresh token available"))
        _authState.value = AuthState.Authenticating

        return authHandler.refreshToken()
            .onSuccess { token ->
                saveToken(token)
                _authState.value = AuthState.Authenticated(token)
            }
            .onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Token refresh failed")
            }
    }

    suspend fun authenticate(): Result<AccessToken> {
        _authState.value = AuthState.Authenticating
        return authHandler.authenticate()
            .mapCatching { code ->
                authHandler.exchangeCodeForToken(code).getOrThrow()
            }
            .onSuccess { token ->
                saveToken(token)
                _authState.value = AuthState.Authenticated(token)
            }
            .onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Authentication failed")
            }
    }

    abstract fun clearTokens()
}