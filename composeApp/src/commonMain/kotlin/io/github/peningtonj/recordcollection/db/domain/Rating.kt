package io.github.peningtonj.recordcollection.db.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumRating(
    @SerialName("album_id") val albumId: String,
    val rating: Int? = null,
)
