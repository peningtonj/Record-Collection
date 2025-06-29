package io.github.peningtonj.recordcollection.network.spotify.model

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
    val albums: PaginatedResponse<Album>?,
    val artists: PaginatedResponse<Artist>?,
    val tracks: PaginatedResponse<Track>?
)
