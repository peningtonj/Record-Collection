package io.github.peningtonj.recordcollection.repository

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.Spotify_auth
import io.github.peningtonj.recordcollection.db.mapper.AuthMapper.toAccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BaseSpotifyAuthRepository(
    protected val authHandler: AuthHandler,
    protected val database: RecordCollectionDatabase
) {
    protected val _authState = MutableStateFlow<AuthState>(loadInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    protected fun loadInitialState(): AuthState {
        Napier.d { "Loading initial auth state" }
        return database.spotifyAuthQueries
            .getStoredToken()
            .executeAsOneOrNull()
            ?.let { token ->
                val currentTime = System.currentTimeMillis()
                val isValid = currentTime < token.expires_at
                Napier.d { "Found stored token. Expires at: ${token.expires_at}, Current time: $currentTime, Valid: $isValid" }
                if (isValid) {
                    AuthState.Authenticated(token.toAccessToken())
                } else {
                    Napier.d { "Stored token is expired" }
                    AuthState.NotAuthenticated
                }
            } ?: run {
                Napier.d { "No stored token found" }
                AuthState.NotAuthenticated
            }
    }

    protected fun saveToken(token: AccessToken) {
        val expiresAt = System.currentTimeMillis() + (token.expiresIn * 1000)
        Napier.d { "Saving new token. Expires in: ${token.expiresIn} seconds (at: $expiresAt)" }
        
        database.spotifyAuthQueries.insertOrUpdateToken(
            spotify_auth = Spotify_auth(
                id = 1,
                access_token = token.accessToken,
                token_type = token.tokenType,
                scope = token.scope,
                expires_in = token.expiresIn,
                refresh_token = token.refreshToken,
                expires_at = expiresAt
            )
        )
    }

    protected fun isTokenValid(): Boolean {
        val result = database.spotifyAuthQueries
            .getStoredToken()
            .executeAsOneOrNull()
            ?.let { token ->
                val currentTime = System.currentTimeMillis()
                val isValid = currentTime < token.expires_at
                Napier.d { "Token validity check - Expires at: ${token.expires_at}, Current time: $currentTime, Valid: $isValid" }
                isValid
            } ?: run {
                Napier.d { "Token validity check - No token found" }
                false
            }
        return result
    }

    protected fun hasRefreshToken(): Boolean {
        val hasToken = database.spotifyAuthQueries
            .hasRefreshToken()
            .executeAsOne()
        Napier.d { "Refresh token availability check: $hasToken" }
        return hasToken
    }

    fun getRefreshToken(): String? {
        val token = database.spotifyAuthQueries
            .getRefreshToken()
            .executeAsOneOrNull()
        Napier.d { "Retrieved refresh token: ${token?.take(5)}..." }
        return token
    }

    fun getAccessToken(): Spotify_auth? {
        val token = database.spotifyAuthQueries
            .getStoredToken()
            .executeAsOneOrNull()
        Napier.d { "Retrieved Access token: ${token}..." }
        return token
    }


    suspend fun ensureValidToken(): Result<AccessToken> {
        Napier.d { "Ensuring valid token..." }
        val currentState = _authState.value

        return when {
            currentState is AuthState.Authenticated && !isTokenValid() -> {
                Napier.d { "Current token is valid, returning existing token" }
                Result.success(currentState.accessToken)
            }
            hasRefreshToken() -> {
                Napier.d { "Token needs refresh, attempting refresh" }
                refreshToken()
                    .recoverCatching {
                        Napier.w(throwable = it) { "Refresh failed, falling back to full authentication" }
                        authenticate().getOrThrow()
                    }
            }
            else -> {
                Napier.d { "No valid token or refresh token, starting fresh authentication" }
                authenticate()
            }
        }
    }

    private suspend fun refreshToken(): Result<AccessToken> {
        val refreshToken = getRefreshToken() ?: return Result.failure(IllegalStateException("No refresh token available"))
        Napier.d { "Starting token refresh process" }
        _authState.value = AuthState.Authenticating

        return authHandler.refreshToken()
            .onSuccess { token ->
                Napier.d { "Token refresh successful. New token expires in: ${token.expiresIn} seconds" }
                saveToken(token)
                _authState.value = AuthState.Authenticated(token)
            }
            .onFailure { error ->
                Napier.e(throwable = error) { "Token refresh failed" }
                _authState.value = AuthState.Error(error.message ?: "Token refresh failed")
            }
    }

    suspend fun authenticate(): Result<AccessToken> {
        Napier.d { "Starting fresh authentication" }
        _authState.value = AuthState.Authenticating
        
        return authHandler.authenticate()
            .mapCatching { code ->
                Napier.d { "Auth code received, exchanging for token" }
                authHandler.exchangeCodeForToken(code).getOrThrow()
            }
            .onSuccess { token ->
                Napier.d { "Authentication successful. Token expires in: ${token.expiresIn} seconds" }
                saveToken(token)
                _authState.value = AuthState.Authenticated(token)
            }
            .onFailure { error ->
                Napier.e(throwable = error) { "Authentication failed" }
                _authState.value = AuthState.Error(error.message ?: "Authentication failed")
            }
    }
}