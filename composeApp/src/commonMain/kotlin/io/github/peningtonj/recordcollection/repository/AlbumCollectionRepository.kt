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

val CACHED_RESPONSE = """
    [
      {"album": "First Works", "artist": "54 Ultra"},
      {"album": "Addison", "artist": "Addison Rae"},
      {"album": "Let Go Letters", "artist": "Avery Anna"},
      {"album": "Debí Tirar Más Fotos", "artist": "Bad Bunny"},
      {"album": "If You Asked for a Picture", "artist": "Blondshell"},
      {"album": "King of Hearts", "artist": "Brandon Lake"},
      {"album": "The Scholars", "artist": "Car Seat Headrest"},
      {"album": "Latinaje", "artist": "Cazzu"},
      {"album": "Can’t Rush Greatness", "artist": "Central Cee"},
      {"album": "Lonesome Drifter", "artist": "Charley Crockett"},
      {"album": "5ive", "artist": "Davido"},
      {"album": "DIA", "artist": "Ela Minus"},
      {"album": "Who Believes in Angels?", "artist": "Elton John & Brandi Carlile"},
      {"album": "Evangeline vs. The Machine", "artist": "Eric Church"},
      {"album": "Lifetime", "artist": "Erika de Casier"},
      {"album": "Vibras de Noche II", "artist": "Eslabon Armado"},
      {"album": "111xpantia", "artist": "Fuerza Regida"},
      {"album": "Revengeseekerz", "artist": "Jane Remover"},
      {"album": "You Are the Morning", "artist": "Jasmine.4.T"},
      {"album": "Foxes in the Snow", "artist": "Jason Isbell"},
      {"album": "Ruby", "artist": "JENNIE"},
      {"album": "Paid in Memories", "artist": "Jessie Reyez"},
      {"album": "NGL", "artist": "JoJo"},
      {"album": "Sincerely,", "artist": "Kali Uchis"},
      {"album": "Mayhem", "artist": "Lady Gaga"},
      {"album": "Forever Is a Feeling", "artist": "Lucy Dacus"},
      {"album": "Dreamsicle", "artist": "Maren Morris"},
      {"album": "Something Beautiful", "artist": "Miley Cyrus"},
      {"album": "I’m the Problem", "artist": "Morgan Wallen"},
      {"album": "Cancionera", "artist": "Natalia Lafourcade"},
      {"album": "Choke Enough", "artist": "Oklou"},
      {"album": "While the Iron Is Hot", "artist": "Ovrkast."},
      {"album": "Sinister Gift", "artist": "Panda Bear"},
      {"album": "Glory", "artist": "Perfume Genius"},
      {"album": "Fancy That", "artist": "PinkPantheress"},
      {"album": "Music", "artist": "Playboi Carti"},
      {"album": "Trainspotting", "artist": "Rome Streetz & Conductor Williams"},
      {"album": "Louder, Please", "artist": "Rose Gray"},
      {"album": "From Florida’s Finest", "artist": "SAILORR"},
      {"album": "People Watching", "artist": "Sam Fender"},
      {"album": "10", "artist": "Sault"},
      {"album": "F*CK U SKRILLEX YOU THINK UR ANDY WARHOL BUT UR NOT!! <3", "artist": "Skrillex"},
      {"album": "So Close to What", "artist": "Tate McRae"},
      {"album": "I’ve Tried Everything But Therapy (Part 2)", "artist": "Teddy Swims"},
      {"album": "Hurry Up Tomorrow", "artist": "The Weeknd"},
      {"album": "Never Enough", "artist": "Turnstile"},
      {"album": "Prove Them Wrong", "artist": "Valiant"},
      {"album": "Sinners (Original Motion Picture Soundtrack)", "artist": "Various Artists"},
      {"album": "Whatever the Weather II", "artist": "Whatever the Weather"},
      {"album": "Oh What a Beautiful World", "artist": "Willie Nelson"}
    ]
""".trimIndent()

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