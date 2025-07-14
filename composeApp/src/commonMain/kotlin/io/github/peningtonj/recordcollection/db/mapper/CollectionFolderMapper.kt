package io.github.peningtonj.recordcollection.db.mapper

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.Collection_folders
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionFolder
import kotlinx.serialization.json.Json


object CollectionFolderMapper {
    fun toDomain(entity: Collection_folders) : CollectionFolder {
        return CollectionFolder(
            folderName = entity.folder_name,
            collections = entity.collections?.let {
                Json.decodeFromString<List<AlbumCollection>>(it)
            } ?: emptyList(),
            folders = entity.folders?.let {
                Json.decodeFromString<List<CollectionFolder>>(it)
            } ?: emptyList(),
            parentName = entity.parent
        )
    }
}