package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UserApi(
    private val client: HttpClient,
) {
    suspend fun getCurrentUserProfile(): Result<SpotifyProfileDto> = runCatching {
        client.get("https://api.spotify.com/v1/me").body()
    }
}