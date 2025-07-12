package io.github.peningtonj.recordcollection.network.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.spotify.model.DevicePlaybackRequest
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackDto
import io.github.peningtonj.recordcollection.network.spotify.model.ShufflePlaybackRequest
import io.github.peningtonj.recordcollection.network.spotify.model.StartPlaybackRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
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

        val response = client.get("${SpotifyApi.BASE_URL}/me/player")


        when (response.status) {
            HttpStatusCode.OK -> {
                val body = response.body<PlaybackDto>()
                body
            }
            HttpStatusCode.NoContent -> {
                throw PlaybackException.NoActivePlayback()
            }
            HttpStatusCode.NotFound -> {
                Napier.d { "No active device found" }
                throw PlaybackException.NoActiveDevice()
            }
            HttpStatusCode.Forbidden -> {
                Napier.d { "Premium required" }
                throw PlaybackException.PremiumRequired()
            }
            else -> {
                val errorBody = response.bodyAsText()
                Napier.e { "Unexpected playback state error: ${response.status} - $errorBody" }
                throw PlaybackException.UnexpectedError(response.status.value, errorBody)
            }
        }
    }


    suspend fun toggleShuffle(request: ShufflePlaybackRequest) {
        Napier.d { "Setting shuffle to ${request.state}" }

        val response = client.put("${SpotifyApi.BASE_URL}/me/player/shuffle") {
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

    suspend fun pausePlayback(request: DevicePlaybackRequest) {
        Napier.d { "Pausing playback" }
        val response = client.put("${SpotifyApi.BASE_URL}/me/player/pause") {
            url {
                request.deviceId?.let {
                    parameters.append("device_id", it)
                }
            }
        }
    }

    suspend fun skipToNextTrack(request: DevicePlaybackRequest) {
        Napier.d { "Skipping to next track" }
        val response = client.post("${SpotifyApi.BASE_URL}/me/player/next") {
            url {
                request.deviceId?.let {
                    parameters.append("device_id", it)
                }
            }
        }
    }

    suspend fun skipToPreviousTrack(request: DevicePlaybackRequest) {
        Napier.d { "Skipping to previous track" }
        val response = client.post("${SpotifyApi.BASE_URL}/me/player/previous") {
            url {
                request.deviceId?.let {
                    parameters.append("device_id", it)
                }
            }
        }
    }

    suspend fun seekToPosition(positionMs: Long) {
        Napier.d { "Seeking to $positionMs" }
        val response = client.put("${SpotifyApi.BASE_URL}/me/player/seek") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("position_ms" to positionMs))
        }
    }


}

sealed class PlaybackException(message: String) : Exception(message) {
    class NoActiveDevice : PlaybackException("No active playback device found")
    class PremiumRequired : PlaybackException("Premium subscription required for this action")
    class NoActivePlayback : PlaybackException("No playback currently active")

    data class UnexpectedError(val statusCode: Int, override val message: String) :
        PlaybackException("Unexpected error ($statusCode): $message")
}
