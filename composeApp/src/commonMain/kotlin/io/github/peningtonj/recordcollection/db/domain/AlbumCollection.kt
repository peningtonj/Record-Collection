package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Instant

data class AlbumCollection(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CollectionAlbum(
    val id: Long = 0,
    val collectionId: Long,
    val album: Album,
    val position: Int,
    val addedAt: Instant
)
