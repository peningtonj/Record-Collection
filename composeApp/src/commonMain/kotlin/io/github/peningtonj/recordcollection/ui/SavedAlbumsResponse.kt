package io.github.peningtonj.recordcollection.ui

import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import kotlinx.serialization.Serializable

@Serializable
data class SavedAlbumsResponse(
    val items: List<SavedAlbum>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String?,
    val previous: String?
)

@Serializable
data class SavedAlbum(
    val added_at: String, // ISO 8601 timestamp
    val album: AlbumDto
)