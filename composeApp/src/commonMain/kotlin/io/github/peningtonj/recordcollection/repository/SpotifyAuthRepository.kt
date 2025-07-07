package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.Spotify_auth
import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpotifyAuthRepository(
    private val authHandler: AuthHandler,
    private val database: RecordCollectionDatabase
) {
    private val _authState = MutableStateFlow<AuthState>(loadInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Primary Public API
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

    suspend fun ensureValidToken(): Result<AccessToken> {
        val currentToken = getStoredToken()
        
        return when {
            // Valid token exists
            currentToken != null && !currentToken.isExpired() -> {
                Result.success(currentToken.toAccessToken())
            }
            // Try refresh if possible
            hasRefreshToken() -> {
                refreshToken().recoverCatching {
                    authenticate().getOrThrow()
                }
            }
            // Fall back to new authentication
            else -> {
                authenticate()
            }
        }
    }

    // Token Management
    private suspend fun refreshToken(): Result<AccessToken> {
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

    // Database Operations
    private fun saveToken(token: AccessToken) {
        val expiresAt = System.currentTimeMillis() + (token.expiresIn * 1000)
        
        database.spotifyAuthQueries.insertOrUpdateToken(
            Spotify_auth(
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

    fun getStoredToken(): Spotify_auth? =
        database.spotifyAuthQueries.getStoredToken().executeAsOneOrNull()

    fun getRefreshToken(): String? =
        database.spotifyAuthQueries.getRefreshToken().executeAsOneOrNull()

    private fun hasRefreshToken(): Boolean =
        database.spotifyAuthQueries.hasRefreshToken().executeAsOne()

    // Initial State
    private fun loadInitialState(): AuthState {
        return getStoredToken()?.let { token ->
            if (!token.isExpired()) {
                AuthState.Authenticated(token.toAccessToken())
            } else {
                AuthState.NotAuthenticated
            }
        } ?: AuthState.NotAuthenticated
    }

    // Extensions
    private fun Spotify_auth.isExpired(): Boolean =
        System.currentTimeMillis() >= expires_at

    private fun Spotify_auth.toAccessToken(): AccessToken =
        AccessToken(
            accessToken = access_token,
            tokenType = token_type,
            scope = scope,
            expiresIn = expires_in,
            refreshToken = refresh_token
        )
}