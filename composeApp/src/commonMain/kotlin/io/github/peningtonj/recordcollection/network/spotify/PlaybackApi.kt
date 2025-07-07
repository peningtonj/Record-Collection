package io.github.peningtonj.recordcollection.network.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackDto
import io.github.peningtonj.recordcollection.network.spotify.model.ShuffleToggleRequest
import io.github.peningtonj.recordcollection.network.spotify.model.StartPlaybackRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

class PlaybackApi(
    private val client: HttpClient,
) {
    suspend fun getPlaybackState() : Result<PlaybackDto> = runCatching {
        client.get("${SpotifyApi.BASE_URL}/me/player").body()
    }

    suspend fun toggleShuffle(request: ShuffleToggleRequest) {
        Napier.d { "Setting shuffle to ${request.state}" }

        val response = client.put("${SpotifyApi.BASE_URL}/me/player/shuffle") {
            contentType(ContentType.Application.Json) // technically not needed
            url {
                parameters.append("state", request.state.toString())
                request.deviceId?.let {
                    parameters.append("device_id", it)
                }
            }
        }

        Napier.d { "Playback response ${response.status}" }

        when (response.status) {
            HttpStatusCode.NoContent,
            HttpStatusCode.OK -> Unit // Accept both as success
            HttpStatusCode.NotFound -> throw PlaybackException.NoActiveDevice()
            HttpStatusCode.Forbidden -> throw PlaybackException.PremiumRequired()
            else -> throw PlaybackException.UnexpectedError(response.status.value, response.bodyAsText())
        }
    }

    suspend fun startPlayback(request: StartPlaybackRequest? = null): Result<Unit> = runCatching {
        Napier.d { "Starting playback of ${request?.contextUri}" }
        val response = client.put("${SpotifyApi.BASE_URL}/me/player/play") {
            contentType(ContentType.Application.Json)
            if (request != null) {
                setBody(request)
            }
        }

        Napier.d { "Playback response ${response.status}" }


        when (response.status) {
            HttpStatusCode.NoContent -> Unit // Success
            HttpStatusCode.NotFound -> throw PlaybackException.NoActiveDevice()
            HttpStatusCode.Forbidden -> throw PlaybackException.PremiumRequired()
            else -> throw PlaybackException.UnexpectedError(response.status.value, response.bodyAsText())
        }
    }
}

sealed class PlaybackException(message: String) : Exception(message) {
    class NoActiveDevice : PlaybackException("No active playback device found")
    class PremiumRequired : PlaybackException("Premium subscription required for this action")
    data class UnexpectedError(val statusCode: Int, override val message: String) :
        PlaybackException("Unexpected error ($statusCode): $message")
}