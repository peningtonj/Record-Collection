package io.github.peningtonj.recordcollection.db.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.peningtonj.recordcollection.db.AlbumTagsQueries
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.mapper.TagMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AlbumTagRepository(
    private val database: RecordCollectionDatabase
) {
    fun getTagsForAlbum(albumId: String): Flow<List<Tag>> {
        return database.albumTagsQueries.getTagsForAlbum(albumId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { tag -> TagMapper.toDomain(tag) } }
    }

    fun addTagToAlbum(albumId: String, tagId: String) =
        database.albumTagsQueries.addTagToAlbum(albumId, tagId, System.currentTimeMillis())

    fun removeTagFromAlbum(albumId: String, tagId: String) =
        database.albumTagsQueries.removeTagFromAlbum(albumId, tagId)
}
