package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.Auths
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
    private val _authState = MutableStateFlow(loadInitialState())
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
                _authState.value = AuthState.NotAuthenticated
                return Result.failure(Exception("No valid token available"))
            }
        }
    }

    fun logout() {
        // Clear tokens from database
        database.authsQueries.deleteToken()
        
        // Update state to not authenticated
        _authState.value = AuthState.NotAuthenticated
    }

    // Token Management
    private suspend fun refreshToken(): Result<AccessToken> {
        _authState.value = AuthState.Authenticating

        val refreshToken = getRefreshToken()
            ?: return Result.failure(Exception("No refresh token available"))


        return authHandler.refreshToken(refreshToken)
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
        val expiresAt = System.currentTimeMillis() + (token.expiresIn * 1000) - (60 * 1000)
        
        database.authsQueries.insertOrUpdateToken(
            Auths(
                id = 1,
                access_token = token.accessToken,
                token_type = token.tokenType,
                scope = token.scope,
                expires_in = token.expiresIn,
                refresh_token = token.refreshToken ?: "",
                expires_at = expiresAt
            )
        )
    }

    fun getStoredToken(): Auths? =
        database.authsQueries.getStoredToken().executeAsOneOrNull()

    fun getRefreshToken(): String? =
        database.authsQueries.getRefreshToken().executeAsOneOrNull()

    private fun hasRefreshToken(): Boolean =
        database.authsQueries.hasRefreshToken().executeAsOne()

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
    private fun Auths.isExpired(): Boolean =
        System.currentTimeMillis() >= expires_at

    private fun Auths.toAccessToken(): AccessToken =
        AccessToken(
            accessToken = access_token,
            tokenType = token_type,
            scope = scope,
            expiresIn = expires_in,
            refreshToken = refresh_token
        )
}