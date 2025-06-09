package io.github.peningtonj.recordcollection.network.spotify

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class SpotifyApi(
    private val client: HttpClient
) {
    suspend fun getCurrentUserProfile(accessToken: String): Result<SpotifyProfile> = runCatching {
        client.get("https://api.spotify.com/v1/me") {
            headers {
                append("Authorization", "Bearer $accessToken")
            }
        }.body()
    }
}