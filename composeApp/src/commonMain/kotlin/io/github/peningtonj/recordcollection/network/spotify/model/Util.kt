package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val href: String,
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?,
    val previous: String?
)


suspend fun <T> PaginatedResponse<T>.getAllItems(
    fetchNext: suspend (String) -> Result<PaginatedResponse<T>>
): Result<List<T>> {
    val allItems = mutableListOf<T>()
    var currentPage: PaginatedResponse<T>? = this

    while (currentPage != null) {
        allItems.addAll(currentPage.items)
        currentPage = currentPage.next?.let { nextUrl ->
            val nextResult = fetchNext(nextUrl)
            if (nextResult.isFailure) return Result.failure(nextResult.exceptionOrNull()!!)
            nextResult.getOrNull()
        }
    }

    return Result.success(allItems)
}


@Serializable
data class SearchResponse(
    val albums: PaginatedResponse<AlbumDto>?,
    val artists: PaginatedResponse<FullArtistDto>?,
    val tracks: PaginatedResponse<TrackDto>?
)

@Serializable
data class SpotifyImage(
    val url: String,
    val height: Int? = null,
    val width: Int? = null
)

@Serializable
data class ContextDto(
    val type: String,
    val href: String,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val uri: String
    )