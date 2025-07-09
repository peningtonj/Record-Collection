package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumsResponse
import io.github.peningtonj.recordcollection.network.spotify.model.ArtistsResponse
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import io.github.peningtonj.recordcollection.network.spotify.model.PaginatedResponse
import io.github.peningtonj.recordcollection.network.spotify.model.SavedAlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedTrackDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder

class LibraryApi(
    private val client: HttpClient,
) {
    suspend fun getAlbum(id: String): Result<AlbumDto> = runCatching {
        client.get("${SpotifyApi.BASE_URL}/albums/$id").body()
    }


    suspend fun getAlbumTracks(albumId: String): Result<PaginatedResponse<SimplifiedTrackDto>> = runCatching {
        client.get("${SpotifyApi.BASE_URL}/albums/$albumId/tracks").body()
    }

    suspend fun getArtist(id: String): Result<FullArtistDto> = runCatching {
        client.get("${SpotifyApi.BASE_URL}/artists/$id").body()
    }

    suspend fun getMultipleArtists(ids: List<String>) : Result<ArtistsResponse> = runCatching {
        require(ids.size <= 50) { "Spotify API allows maximum 20 artists per request" }

        val url = URLBuilder("${SpotifyApi.BASE_URL}/artists").apply {
            parameters.append("ids", ids.joinToString(","))
        }.buildString()

        client.get(url).body()
    }

    suspend fun getMultipleAlbums(ids: List<String>): Result<AlbumsResponse> = runCatching {
        require(ids.size <= 20) { "Spotify API allows maximum 20 albums per request" }

        val url = URLBuilder("${SpotifyApi.BASE_URL}/albums").apply {
            parameters.append("ids", ids.joinToString(","))
        }.buildString()

        client.get(url).body()
    }

    suspend fun getUsersPlaylists(limit: Int = 20, offset: Int = 0): Result<PaginatedResponse<SavedAlbumDto>> = runCatching {
        val url = URLBuilder("${SpotifyApi.BASE_URL}/me/playlists").apply {
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }.buildString()

        client.get(url).body()
    }

    suspend fun getUsersSavedAlbums(limit: Int = 20, offset: Int = 0): Result<PaginatedResponse<SavedAlbumDto>> = runCatching {
        val url = URLBuilder("${SpotifyApi.BASE_URL}/me/albums").apply {
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }.buildString()

        client.get(url).body()
    }

}
