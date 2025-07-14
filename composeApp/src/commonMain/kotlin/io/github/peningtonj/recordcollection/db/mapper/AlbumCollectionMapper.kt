package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Album_collections
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import kotlinx.datetime.Instant

object AlbumCollectionMapper {
    fun toDomain(albumCollection: Album_collections) : AlbumCollection {
        return AlbumCollection(
            name = albumCollection.name,
            description = albumCollection.description,
            createdAt = Instant.fromEpochSeconds(albumCollection.created_at),
            updatedAt = Instant.fromEpochSeconds(albumCollection.updated_at),
            parentName = albumCollection.parent_name
        )
    }
}