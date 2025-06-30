package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedAlbumsResponse(
    val href: String,
    val items: List<SavedAlbumDto>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?,
    val previous: String?
)

@Serializable
data class SavedAlbumDto(
    val added_at: String, // ISO 8601 timestamp
    val album: AlbumDto
)