package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Instant

data class AlbumCollection(
    val name: String,
    val description: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CollectionAlbum(
    val collectionName: String,
    val album: Album,
    val position: Int,
    val addedAt: Instant
)

data class CollectionAlbumId(
    val collectionName: String,
    val albumId: String,
    val position: Int,
    val addedAt: Instant
)

data class AlbumCollectionInfo(
    val collection: AlbumCollection,
    val position: Int,
    val addedAt: Instant
)
