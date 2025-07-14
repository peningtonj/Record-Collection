package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AlbumCollection(
    val name: String,
    val description: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val parentName: String? = null
)

data class CollectionAlbum(
    val collectionName: String,
    val album: Album,
    val position: Int,
    val addedAt: Instant
)

@Serializable
data class CollectionFolder(
    val folderName: String,
    val collections: List<AlbumCollection>,
    val folders: List<CollectionFolder>,
    val parentName: String? = null
)

data class AlbumCollectionInfo(
    val collection: AlbumCollection,
    val position: Int,
    val addedAt: Instant
)
