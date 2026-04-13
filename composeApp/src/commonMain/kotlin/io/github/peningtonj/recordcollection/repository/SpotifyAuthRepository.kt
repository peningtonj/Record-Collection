package io.github.peningtonj.recordcollection.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class SpotifyAuthRepository(
    private val authHandler: AuthHandler,
    private val settings: Settings
) {
    private val _authState = MutableStateFlow(loadInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private companion object {
        const val KEY_ACCESS_TOKEN = "auth_access_token"
        const val KEY_TOKEN_TYPE = "auth_token_type"
        const val KEY_SCOPE = "auth_scope"
        const val KEY_EXPIRES_IN = "auth_expires_in"
        const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        const val KEY_EXPIRES_AT = "auth_expires_at"
    }

    private data class StoredToken(
        val accessToken: String,
        val tokenType: String,
        val scope: String,
        val expiresIn: Long,
        val refreshToken: String,
        val expiresAt: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() >= expiresAt
        fun toAccessToken(): AccessToken = AccessToken(
            accessToken = accessToken,
            tokenType = tokenType,
            scope = scope,
            expiresIn = expiresIn,
            refreshToken = refreshToken.ifEmpty { null }
        )
    }

    // Primary Public API
    suspend fun authenticate(): Result<AccessToken> {
        _authState.value = AuthState.Authenticating

        return authHandler.authenticate()
            .mapCatching { code ->
                // Get code_verifier from auth handler
                val codeVerifier = authHandler.getCodeVerifier()
                authHandler.exchangeCodeForToken(code, codeVerifier).getOrThrow()
            }
            .onSuccess { token ->
                saveToken(token)
                _authState.value = AuthState.Authenticated(token)
            }
            .onFailure { error ->
                Napier.e("Authentication failed", error)
                _authState.value = AuthState.Error(error.message ?: "Authentication failed")
            }
    }

    suspend fun ensureValidToken(): Result<AccessToken> {
        val currentToken = readStoredToken()
        if (currentToken != null && currentToken.isExpired()) {
            Napier.d { "Found token but it was expired at ${Clock.System.now()}" }
        }

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
                deleteToken()
                _authState.value = AuthState.NotAuthenticated
                Result.failure(Exception("No valid token available"))
            }
        }
    }

    fun logout() {
        // Clear tokens from database
        deleteToken()

        // Update state to not authenticated
        _authState.value = AuthState.NotAuthenticated
    }

    // Token Management
    private suspend fun refreshToken(): Result<AccessToken> {
        _authState.value = AuthState.Authenticating

        val refreshToken = getRefreshToken()
            ?: return Result.failure(Exception("No refresh token available"))

        Napier.d { "Refreshing token with $refreshToken" }

        return authHandler.refreshToken(refreshToken)
            .onSuccess { token ->
                saveToken(token, refreshToken)
                _authState.value = AuthState.Authenticated(token)
            }
            .onFailure { error ->
                deleteToken()
                _authState.value = AuthState.Error(error.message ?: "Token refresh failed")
            }
    }

    // Settings Operations
    private fun saveToken(token: AccessToken, refreshToken: String = "") {
        val expiresAt = System.currentTimeMillis() + (token.expiresIn * 1000) - (60 * 1000)
        Napier.d { "Saving token to settings with refresh token: ${token.refreshToken ?: refreshToken} ($expiresAt)" }
        settings[KEY_ACCESS_TOKEN] = token.accessToken
        settings[KEY_TOKEN_TYPE] = token.tokenType
        settings[KEY_SCOPE] = token.scope
        settings[KEY_EXPIRES_IN] = token.expiresIn
        settings[KEY_REFRESH_TOKEN] = token.refreshToken ?: refreshToken
        settings[KEY_EXPIRES_AT] = expiresAt
    }

    private fun deleteToken() {
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_TOKEN_TYPE)
        settings.remove(KEY_SCOPE)
        settings.remove(KEY_EXPIRES_IN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_EXPIRES_AT)
    }

    private fun readStoredToken(): StoredToken? {
        val accessToken = settings.getStringOrNull(KEY_ACCESS_TOKEN) ?: return null
        return StoredToken(
            accessToken = accessToken,
            tokenType = settings.getString(KEY_TOKEN_TYPE, "Bearer"),
            scope = settings.getString(KEY_SCOPE, ""),
            expiresIn = settings.getLong(KEY_EXPIRES_IN, 0L),
            refreshToken = settings.getString(KEY_REFRESH_TOKEN, ""),
            expiresAt = settings.getLong(KEY_EXPIRES_AT, 0L)
        )
    }

    /** Returns the stored token as [AccessToken], or null if none is stored. */
    fun getStoredToken(): AccessToken? = readStoredToken()?.toAccessToken()

    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)?.ifEmpty { null }

    private fun hasRefreshToken(): Boolean = !settings.getStringOrNull(KEY_REFRESH_TOKEN).isNullOrEmpty()

    // Initial State
    private fun loadInitialState(): AuthState {
        return readStoredToken()?.let { token ->
            if (!token.isExpired()) {
                AuthState.Authenticated(token.toAccessToken())
            } else {
                AuthState.NotAuthenticated
            }
        } ?: AuthState.NotAuthenticated
    }
}