package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.network.spotify.model.PaginatedResponse
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyUserPlaylistDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.http.URLBuilder

class UserApi(
    private val client: HttpClient,
) {
    suspend fun getCurrentUserProfile(): Result<SpotifyProfileDto> = runCatching {
        client.get("${SpotifyApi.BASE_URL}/me").body()
    }

    suspend fun saveAlbumsToCurrentUsersLibrary(albumIds: List<String>) {
        client.put(URLBuilder("${SpotifyApi.BASE_URL}/me/albums").apply {
                parameters.append("ids", albumIds.joinToString(","))
            }.buildString()
        )
    }

    suspend fun removeAlbumsFromCurrentUsersLibrary(albumIds: List<String>) {
        client.delete(URLBuilder("${SpotifyApi.BASE_URL}/me/albums").apply {
            parameters.append("ids", albumIds.joinToString(","))
        }.buildString()
        )
    }

    suspend fun getUserPlaylists(): Result<PaginatedResponse<SpotifyUserPlaylistDto>> = runCatching{
        client.get("${SpotifyApi.BASE_URL}/me/playlists").body()
    }
}