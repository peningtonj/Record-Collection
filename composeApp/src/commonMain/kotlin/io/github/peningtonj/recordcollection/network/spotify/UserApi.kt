package io.github.peningtonj.recordcollection.network.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.spotify.model.NewReleasesResponse
import io.github.peningtonj.recordcollection.network.spotify.model.PaginatedResponse
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedAlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyUserPlaylistDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

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

    suspend fun saveTracksToLikedTracks(trackIds: List<String>) {
        require(trackIds.size <= 50) { "Spotify API allows maximum 50 tracks per request" }

        val url = "${SpotifyApi.BASE_URL}/me/tracks"

        Napier.d("Saving tracks to liked tracks: ${trackIds.joinToString(",")}")

        client.put(url) {
            setBody(mapOf("ids" to trackIds)) // Send as object with "ids" field
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun removeTracksFromLikedTracks(trackIds: List<String>) {
        require(trackIds.size <= 50) { "Spotify API allows maximum 50 tracks per request" }

        val url = URLBuilder("${SpotifyApi.BASE_URL}/me/tracks").apply {
            parameters.append("ids", trackIds.joinToString(",")) // Comma-separated, not JSON array
        }.buildString()

        client.delete(url)
    }

    suspend fun getUserPlaylists(): Result<PaginatedResponse<SpotifyUserPlaylistDto>> = runCatching{
        client.get("${SpotifyApi.BASE_URL}/me/playlists").body()
    }

    suspend fun getNewReleases(): Result<NewReleasesResponse> = runCatching {
        client.get("${SpotifyApi.BASE_URL}/browse/new-releases").body()
    }

    suspend fun fetchNextNewReleases(url: String): Result<PaginatedResponse<SimplifiedAlbumDto>> {
        return runCatching {
            val wrapper: NewReleasesResponse = client.get(url).body()
            wrapper.albums
        }
    }

}