package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.mapper.AlbumCollectionMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AlbumCollectionRepository(
    private val database: RecordCollectionDatabase
) {
    
    fun getAllCollections(): Flow<List<AlbumCollection>> = 
        database.albumCollectionsQueries
            .selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(AlbumCollectionMapper::toDomain) }
    
    fun getCollectionByName(name: String): Flow<AlbumCollection?> =
        database.albumCollectionsQueries
            .selectByName(name)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let(AlbumCollectionMapper::toDomain) }
    
    fun createCollection(
        name: String,
        description: String? = null,
    ) = database.albumCollectionsQueries.insert(
            name = name,
            description = description,
        )

    suspend fun updateCollection(
        oldName: String,
        newName: String,
        description: String? = null,
    ) {
        database.albumCollectionsQueries.update(
            name = oldName,
            description = description,
            newName
        )
    }
    
    fun deleteCollection(name: String) {
        database.albumCollectionsQueries.delete(name)
    }
    
    fun getCollectionCount(): Flow<Long> = 
        database.albumCollectionsQueries
            .getCount()
            .asFlow()
            .mapToOne(Dispatchers.IO)
}