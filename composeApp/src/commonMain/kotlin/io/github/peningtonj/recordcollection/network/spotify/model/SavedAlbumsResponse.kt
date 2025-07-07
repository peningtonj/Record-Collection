package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
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
    @SerialName("added_at")
    val addedAt: String, // ISO 8601 timestamp
    val album: AlbumDto
)