package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.AlbumCollectionInfo
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbum
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbumId
import io.github.peningtonj.recordcollection.db.mapper.CollectionAlbumMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionAlbumRepository(
    private val database: RecordCollectionDatabase
) {
    fun getAlbumsInCollection(collectionName: String): Flow<List<CollectionAlbum>> =
        database.collectionAlbumsQueries
            .selectAlbumsInCollection(collectionName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(CollectionAlbumMapper::toAlbumWithPosition) }
    
    fun addAlbumToCollection(collectionName: String, albumId: String) {
        val maxPosition = database.collectionAlbumsQueries
            .getMaxPosition(collectionName)
            .executeAsOne()
            .COALESCE

        val nextPosition = (maxPosition ?: 0) + 1

        database.collectionAlbumsQueries.insert(
            collection_name = collectionName,
            album_id = albumId,
            position = nextPosition
        )
    }
    
    fun removeAlbumFromCollection(collectionName: String, albumId: String) {
        database.collectionAlbumsQueries.delete(
            collection_name = collectionName,
            album_id = albumId
        )
    }
    
    fun reorderAlbums(collectionName: String, albumPositions: List<Pair<String, Int>>) {
        database.transaction {
            albumPositions.forEach { (albumId, position) ->
                database.collectionAlbumsQueries.updatePosition(
                    position = position.toLong(),
                    collection_name = collectionName,
                    album_id = albumId
                )
            }
        }
    }
    
    fun clearCollection(collectionName: String) {
        database.collectionAlbumsQueries.deleteByCollectionName(collectionName)
    }
    
    fun getAlbumCountInCollection(collectionName: String): Flow<Long> =
        database.collectionAlbumsQueries
            .getCount(collectionName)
            .asFlow()
            .mapToOne(Dispatchers.IO)
    
    fun isAlbumInCollection(collectionName: String, albumId: String): Flow<Boolean> =
        database.collectionAlbumsQueries
            .isAlbumInCollection(collectionName, albumId)
            .asFlow()
            .mapToOne(Dispatchers.IO)

    fun getCollectionsForAlbum(albumId: String): Flow<List<AlbumCollectionInfo>> =
        database.collectionAlbumsQueries.getCollectionsForAlbum(albumId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { CollectionAlbumMapper.toDomain(it) } }
}