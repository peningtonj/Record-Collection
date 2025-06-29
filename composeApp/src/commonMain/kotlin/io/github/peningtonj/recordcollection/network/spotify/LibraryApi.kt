package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.network.spotify.model.Album
import io.github.peningtonj.recordcollection.network.spotify.model.Artist
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.http.URLBuilder

class LibraryApi(
    private val client: HttpClient,
) {
    suspend fun getAlbum(id: String): Result<Album> = runCatching {
        client.get("https://api.spotify.com/v1/albums/$id").body()
    }

    suspend fun getArtist(id: String): Result<Artist> = runCatching {
        client.get("https://api.spotify.com/v1/artists/$id").body()
    }

    suspend fun getUsersSavedAlbums(limit: Int = 20, offset: Int = 0): Result<List<Album>> = runCatching {
        val url = URLBuilder("https://api.spotify.com/v1/me/albums").apply {
            parameters.append("limit", limit.toString())
            parameters.append("offset", offset.toString())
        }.buildString()

        client.get(url).body()
    }
}
