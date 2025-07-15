package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchRequest
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json

class SearchApi(
    private val client: HttpClient,
) {
    private val searchJson = Json {
        ignoreUnknownKeys = true
    }

    suspend fun searchSpotify(query: SpotifySearchRequest): SpotifySearchResult {
        val url = URLBuilder("${SpotifyApi.BASE_URL}/search").apply {
            parameters.append("q", query.q)
            query.type?.let { parameters.append("type", query.type.joinToString(",").lowercase()) }
            query.market?.let { parameters.append("market", it) }
            query.limit?.let { parameters.append("limit", it.toString()) }
            query.offset?.let { parameters.append("offset", it.toString()) }
            query.includeExternal?.let { parameters.append("include_external", it) }
        }.buildString()

        val response = client.get(url)

        return searchJson.decodeFromString<SpotifySearchResult>(response.body())
    }


}