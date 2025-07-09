package io.github.peningtonj.recordcollection.db.domain

data class AlbumRating(
    val albumId: String,
    val rating: Int? = null,
)
