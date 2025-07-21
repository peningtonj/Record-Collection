package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionFolder
import io.github.peningtonj.recordcollection.db.mapper.AlbumCollectionMapper
import io.github.peningtonj.recordcollection.db.mapper.CollectionFolderMapper
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class AlbumCollectionRepository(
    private val database: RecordCollectionDatabase,
    private val openAiApi: OpenAiApi
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
        parent: String? = null,
    ) = database.albumCollectionsQueries.insert(
            name = name,
            description = description,
            parent_name = parent
        )

    fun updateCollectionByName(
        newCollectionDetails: AlbumCollection,
        existingName: String,
    ) {
        database.albumCollectionsQueries.update(
            existing_name = existingName,
            new_description = newCollectionDetails.description,
            new_parent = newCollectionDetails.parentName,
            new_name = newCollectionDetails.name,
        )
    }
    
    fun deleteCollection(name: String) {
        database.albumCollectionsQueries.delete(name)
        database.collectionAlbumsQueries.deleteByCollectionName(name)
    }
    
    fun getCollectionCount(): Flow<Long> = 
        database.albumCollectionsQueries
            .getCount()
            .asFlow()
            .mapToOne(Dispatchers.IO)

    fun createFolder(folder: CollectionFolder) =
        database.collectionFoldersQueries.insert(
            folder_name = folder.folderName,
            collections = Json.encodeToString(folder.collections),
            folders = Json.encodeToString(folder.folders),
            parent = folder.parentName,
        )

    fun getAllTopLevelCollections() =
        database.albumCollectionsQueries
            .selectAllTopLevelCollections()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(AlbumCollectionMapper::toDomain) }

    fun getAllTopLevelFolders() =
        database.collectionFoldersQueries
            .getTopLevelFolders()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(CollectionFolderMapper::toDomain) }

    fun getCollectionsByFolder(folderName: String): Flow<List<AlbumCollection>> =
        database.albumCollectionsQueries
            .selectByParent(folderName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(AlbumCollectionMapper::toDomain) }

    fun getFoldersByParent(parentName: String): Flow<List<CollectionFolder>> =
        database.collectionFoldersQueries
            .getFoldersByParent(parentName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map(CollectionFolderMapper::toDomain) }

    suspend fun draftCollectionFromPrompt(prompt: String, url: String): String {
        val urlText = openAiApi.getUrlContent(url)
        val promptWithArticle = """
            $prompt
            $urlText
        """.trimIndent()
        println("Drafting collection from prompt: $promptWithArticle")
        return openAiApi.prompt(promptWithArticle)
    }
}