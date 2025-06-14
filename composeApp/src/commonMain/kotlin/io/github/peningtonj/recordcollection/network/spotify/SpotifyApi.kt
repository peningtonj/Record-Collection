package io.github.peningtonj.recordcollection.network.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.repository.BaseSpotifyAuthRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class SpotifyApi(
    private val client: HttpClient,
    private val authRepository: BaseSpotifyAuthRepository
) {
    suspend fun getCurrentUserProfile(): Result<SpotifyProfile> = runCatching {
        val token = authRepository.getAccessToken()
            ?: throw IllegalStateException("No valid access token found")
        Napier.d { "Retrieving current user profile with $token" }

        client.get("https://api.spotify.com/v1/me") {
            headers {
                append("Authorization", "Bearer ${token.access_token}")
            }
        }.body()
    }
    
    private fun HttpRequestBuilder.authorize() {
        authRepository.getRefreshToken()?.let { token ->
            headers {
                append("Authorization", "Bearer ${token}")
            }
        }
    }
}