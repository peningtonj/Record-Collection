package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumsResponse
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import io.github.peningtonj.recordcollection.network.spotify.model.SavedAlbumsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder

class LibraryApi(
    private val client: HttpClient,
) {
    suspend fun getAlbum(id: String): Result<AlbumDto> = runCatching {
        client.get("https://api.spotify.com/v1/albums/$id").body()
    }

    suspend fun getArtist(id: String): Result<FullArtistDto> = runCatching {
        client.get("https://api.spotify.com/v1/artists/$id").body()
    }

    suspend fun getMultipleAlbums(ids: List<String>): Result<AlbumsResponse> = runCatching {
        require(ids.size <= 20) { "Spotify API allows maximum 20 albums per request" }

        val url = URLBuilder("https://api.spotify.com/v1/albums").apply {
            parameters.append("ids", ids.joinToString(","))
        }.buildString()

        client.get(url).body()
    }


    suspend fun getUsersSavedAlbums(limit: Int = 20, offset: Int = 0): Result<SavedAlbumsResponse> = runCatching {
        val url = URLBuilder("https://api.spotify.com/v1/me/albums").apply {
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }.buildString()

        client.get(url).body()
    }

}
