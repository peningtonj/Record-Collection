package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Entry stored inside a CollectionDocument's albums array */
@Serializable
data class CollectionAlbumEntry(
    @SerialName("album_id") val albumId: String = "",
    val position: Int = 0,
    @SerialName("added_at") val addedAt: Long = 0L
)

/**
 * Firestore document model for the "collection" collection.
 * Document ID == collection name.
 * Albums are embedded as an array rather than a separate flat collection.
 */
@Serializable
data class CollectionDocument(
    val name: String,
    val description: String? = null,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L,
    @SerialName("parent_name") val parentName: String? = null,
    val albums: List<CollectionAlbumEntry> = emptyList()
) {
    fun toAlbumCollection() = AlbumCollection(
        name = name,
        description = description,
        createdAt = Instant.fromEpochSeconds(createdAt),
        updatedAt = Instant.fromEpochSeconds(updatedAt),
        parentName = parentName
    )
}

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

/** Firestore document model for the "collection_folders" collection. Document ID == folder name. */
@Serializable
data class CollectionFolderDocument(
    @SerialName("folder_name") val folderName: String = "",
    val parent: String? = null
) {
    fun toDomain() = CollectionFolder(
        folderName = folderName,
        collections = emptyList(),
        folders = emptyList(),
        parentName = parent
    )
}

