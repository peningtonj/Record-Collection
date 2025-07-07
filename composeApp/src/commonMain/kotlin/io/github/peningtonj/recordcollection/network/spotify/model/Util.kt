package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?,
    val previous: String?
)

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