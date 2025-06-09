package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthState
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Properties

class SpotifyAuthRepository(
    private val authHandler: AuthHandler,
    private val propertiesFile: File = File(System.getProperty("user.home"), ".recordcollection/spotify_auth.properties")
) {
    private val properties = Properties()
    private val _authState = MutableStateFlow<AuthState>(loadInitialState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        propertiesFile.parentFile?.mkdirs()
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }
    }

    private fun loadInitialState(): AuthState {
        val token = properties.getProperty("access_token")
        val expiresAt = properties.getProperty("expires_at")?.toLongOrNull() ?: 0
//        val refreshToken = properties.getProperty("refresh_token")

        return when {
            token != null && System.currentTimeMillis() < expiresAt ->
                AuthState.Authenticated(createAccessToken())
            else -> AuthState.NotAuthenticated
        }
    }

    private fun createAccessToken(): AccessToken {
        return AccessToken(
            accessToken = properties.getProperty("access_token") ?: "",
            tokenType = properties.getProperty("token_type") ?: "",
            scope = properties.getProperty("scope") ?: "",
            expiresIn = properties.getProperty("expires_in")?.toIntOrNull() ?: 0,
            refreshToken = properties.getProperty("refresh_token") ?: ""
        )
    }

    fun getStoredToken(): AccessToken? {
        return if (isTokenValid()) {
            createAccessToken()
        } else {
            null
        }
    }

    private fun isTokenValid(): Boolean {
        val expiresAt = properties.getProperty("expires_at")?.toLongOrNull() ?: 0
        return System.currentTimeMillis() < expiresAt
    }

    suspend fun authenticate(): Result<AccessToken> {
        return authHandler.authenticate()
            .mapCatching { code ->
                authHandler.exchangeCodeForToken(code).getOrThrow()
            }
            .onSuccess { token ->
                saveToken(token)
                _authState.value = AuthState.Authenticated(token)
            }
    }

    private fun saveToken(token: AccessToken) {
        properties.apply {
            setProperty("access_token", token.accessToken)
            setProperty("refresh_token", token.refreshToken)
            setProperty("token_type", token.tokenType)
            setProperty("scope", token.scope)
            setProperty("expires_in", token.expiresIn.toString())
            setProperty("expires_at", (System.currentTimeMillis() + (token.expiresIn * 1000)).toString())
        }
        propertiesFile.outputStream().use {
            properties.store(it, "Spotify Authentication Data")
        }
    }

    fun clearTokens() {
        properties.clear()
        propertiesFile.delete()
        _authState.value = AuthState.NotAuthenticated
    }
}
