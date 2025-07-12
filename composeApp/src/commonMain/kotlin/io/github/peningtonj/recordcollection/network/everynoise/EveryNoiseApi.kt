package io.github.peningtonj.recordcollection.network.everynoise

import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.get
import kotlinx.serialization.json.Json

class EveryNoiseApi(
    private val client: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://everynoise.com/api"
    }

    suspend fun getArtistGenres(id: String): Result<List<String>> = runCatching {
        val response = client.get("$BASE_URL/$id")
        val jsonString = response.bodyAsText()
        
        // Parse the direct map format from the API
        val apiResponse = Json.decodeFromString<Map<String, List<String>>>(jsonString)
        
        // Return the genres for the requested artist ID, or empty list if not found
        apiResponse[id] ?: emptyList()
    }
}