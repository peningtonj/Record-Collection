package io.github.peningtonj.recordcollection.network.miscApi

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.miscApi.model.MusicBrainzResponse
import io.github.peningtonj.recordcollection.network.miscApi.model.Release
import io.github.peningtonj.recordcollection.network.miscApi.model.ReleaseGroup
import io.github.peningtonj.recordcollection.network.miscApi.model.ReleaseGroupResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.serialization.json.Json

class MiscApi(
    private val client: HttpClient,
) {

    private val miscJson = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    companion object Companion {
        const val EVERYNOISE_BASE_URL = "https://everynoise.com/api"
        const val MUSICBRAINZ_BASE_URL = "https://musicbrainz.org/ws/2"
    }

    suspend fun getArtistGenres(id: String): Result<List<String>> = runCatching {
        val response = client.get("$EVERYNOISE_BASE_URL/$id")
        val jsonString = response.bodyAsText()
        
        // Parse the direct map format from the API
        val apiResponse = Json.decodeFromString<Map<String, List<String>>>(jsonString)
        
        // Return the genres for the requested artist ID, or empty list if not found
        apiResponse[id] ?: emptyList()
    }

    suspend fun getAlbumReleaseDetailsByUPC(upc: String) = runCatching {
        Napier.d {"Fetching UPC $upc"}
        val response = client.get("$MUSICBRAINZ_BASE_URL/release?query=barcode:$upc&fmt=json") {
            header("User-Agent", "RecordCollection/1.0 (peningtonj@gmail.com)")
        }
        val jsonString = response.bodyAsText()

        Napier.d {"UPC response: $jsonString"}

        // Parse the direct map format from the API
        try {
            miscJson.decodeFromString<MusicBrainzResponse>(jsonString)
        } catch (e: Exception) {
            Napier.e(e) { "Failed to parse UPC response" }
            throw e
        }
    }

    suspend fun getReleasesForGroup(releaseGroupId: String) = runCatching {
        Napier.d {"Fetching release group $releaseGroupId"}
        val response: String = client.get("$MUSICBRAINZ_BASE_URL/release") {
            parameter("release-group", releaseGroupId)
            parameter("fmt", "json")
            parameter("limit", 100)
            header("User-Agent", "RecordCollection/1.0 (peningtonj@gmail.com)")
        }.body()

        try {
            miscJson
                .decodeFromString<MusicBrainzResponse>(response)
                .releases
        } catch (e: Exception) {
            Napier.e(e) { "Failed to parse Release Group response" }
            throw e
        }

    }
}
