package io.github.peningtonj.recordcollection.network.oauth.spotify

sealed class AuthState {
    data object NotAuthenticated : AuthState()
    data object Authenticating : AuthState()
    data class Authenticated(val accessToken: AccessToken) : AuthState()

    data class Error(val message: String) : AuthState()
}