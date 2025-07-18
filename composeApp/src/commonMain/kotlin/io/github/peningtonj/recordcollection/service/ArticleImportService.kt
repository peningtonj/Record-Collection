package io.github.peningtonj.recordcollection.service

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.network.spotify.model.SearchType
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.github.peningtonj.recordcollection.viewmodel.AlbumLookUpResult
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ArticleImportService(
    private val albumCollectionRepository: AlbumCollectionRepository,
    private val searchRepository: SearchRepository,
) {
    suspend fun getResponseFromOpenAI(url: String): String {
        val prompt = """
            Extract album information from this article and return JSON like:
            [
              {"album": "Rumours", "artist": "Fleetwood Mac"},
              ...
            ]
            
            Only return the json and no additional text.
            
        """.trimIndent()

        return albumCollectionRepository.draftCollectionFromPrompt(prompt, url)
    }

    fun parseResponse(response: String): List<OpenAiResponse> {
        return Json.decodeFromString<List<OpenAiResponse>>(response)
    }

    suspend fun lookupAlbum(album: OpenAiResponse): Album? {
        Napier.d { "Looking up album: ${album.album} by ${album.artist}" }
        return searchRepository.searchSpotify(
            "${album.album}, ${album.artist}",
            listOf(SearchType.ALBUM)
        ).albums?.firstOrNull()
    }
    suspend fun streamAlbumLookups(
        albumNames: List<OpenAiResponse>,
        onResult: (AlbumLookUpResult) -> Unit
    ) {
        coroutineScope {
            albumNames.map { album ->
                launch {
                    val result = lookupAlbum(album)
                    onResult(AlbumLookUpResult(album, result))
                }
            }
        }
    }


}

@Serializable
data class OpenAiResponse(
    val album: String,
    val artist: String,
)