package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionDocument
import io.github.peningtonj.recordcollection.db.domain.CollectionFolder
import io.github.peningtonj.recordcollection.db.domain.CollectionFolderDocument
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class AlbumCollectionRepository(
    private val firestore: FirebaseFirestore,
    private val openAiApi: OpenAiApi
) {
    private val collectionsRef = firestore.collection("collections")
    private val foldersRef = firestore.collection("collection_folders")

    // ── Collection CRUD (Firestore) ───────────────────────────────────────────

    fun getAllCollections(): Flow<List<AlbumCollection>> {
        LoggingUtils.logFirebaseQuery("collections", "snapshots (all collections)")
        return collectionsRef.snapshots.map { snapshot ->
            LoggingUtils.logFirebaseResult("collections", "getAllCollections", snapshot.documents.size)
            snapshot.documents.mapNotNull { it.data<CollectionDocument?>()?.toAlbumCollection() }
        }
    }

    fun getCollectionByName(name: String): Flow<AlbumCollection?> {
        LoggingUtils.logFirebaseQuery("collection", "snapshot by name", mapOf("name" to name))
        return collectionsRef.document(name).snapshots.map { snapshot ->
            LoggingUtils.logFirebaseResult("collection", "getCollectionByName(name=$name)", if (snapshot.exists) 1 else 0)
            if (snapshot.exists) snapshot.data<CollectionDocument?>()?.toAlbumCollection() else null
        }
    }

    suspend fun createCollection(name: String, description: String? = null, parent: String? = null) {
        val now = Clock.System.now().epochSeconds
        LoggingUtils.logFirebaseWrite("collection", "set (create)", name, mapOf("description" to (description ?: ""), "parent" to (parent ?: "")))
        collectionsRef.document(name).set(
            CollectionDocument(
                name = name,
                description = description,
                createdAt = now,
                updatedAt = now,
                parentName = parent,
                albums = emptyList()
            )
        )
    }

    suspend fun updateCollectionByName(newCollectionDetails: AlbumCollection, existingName: String) {
        val now = Clock.System.now().epochSeconds
        if (newCollectionDetails.name != existingName) {
            // Rename: copy document (preserving albums array) then delete old one
            LoggingUtils.logFirebaseQuery("collection", "get for rename", mapOf("from" to existingName, "to" to newCollectionDetails.name))
            val existing = collectionsRef.document(existingName).get().data<CollectionDocument?>()
            LoggingUtils.logFirebaseWrite("collection", "set (rename – new doc)", newCollectionDetails.name)
            collectionsRef.document(newCollectionDetails.name).set(
                CollectionDocument(
                    name = newCollectionDetails.name,
                    description = newCollectionDetails.description,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    parentName = newCollectionDetails.parentName,
                    albums = existing?.albums ?: emptyList()
                )
            )
            LoggingUtils.logFirebaseWrite("collection", "delete (rename – old doc)", existingName)
            collectionsRef.document(existingName).delete()
        } else {
            LoggingUtils.logFirebaseWrite("collection", "set merge (update fields)", existingName)
            collectionsRef.document(existingName).set(
                mapOf(
                    "description" to newCollectionDetails.description,
                    "parent_name" to newCollectionDetails.parentName,
                    "updated_at" to now
                ),
                merge = true
            )
        }
    }

    suspend fun deleteCollection(name: String) {
        LoggingUtils.logFirebaseWrite("collection", "delete", name)
        collectionsRef.document(name).delete()
    }

    fun getCollectionCount(): Flow<Long> {
        LoggingUtils.logFirebaseQuery("collection", "snapshots (count)")
        return collectionsRef.snapshots.map { it.documents.size.toLong() }
    }

    fun getAllTopLevelCollections(): Flow<List<AlbumCollection>> =
        getAllCollections().map { collections -> collections.filter { it.parentName == null } }

    fun getCollectionsByFolder(folderName: String): Flow<List<AlbumCollection>> {
        LoggingUtils.logFirebaseQuery("collection", "snapshots where parent_name ==", mapOf("folderName" to folderName))
        return collectionsRef
            .where { "parent_name" equalTo folderName }
            .snapshots
            .map { snapshot ->
                LoggingUtils.logFirebaseResult("collection", "getCollectionsByFolder(folder=$folderName)", snapshot.documents.size)
                snapshot.documents.mapNotNull { it.data<CollectionDocument?>()?.toAlbumCollection() }
            }
    }

    // ── Folder operations (Firestore) ─────────────────────────────────────────

    suspend fun createFolder(folder: CollectionFolder) {
        LoggingUtils.logFirebaseWrite("collection_folders", "set (create)", folder.folderName)
        foldersRef.document(folder.folderName).set(
            CollectionFolderDocument(
                folderName = folder.folderName,
                parent = folder.parentName
            )
        )
    }

    private fun getAllFolders(): Flow<List<CollectionFolder>> {
        LoggingUtils.logFirebaseQuery("collection_folders", "snapshots (all)")
        return foldersRef.snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { it.data<CollectionFolderDocument?>()?.toDomain() }
        }
    }

    fun getAllTopLevelFolders(): Flow<List<CollectionFolder>> =
        getAllFolders().map { folders -> folders.filter { it.parentName == null } }

    fun getFoldersByParent(parentName: String): Flow<List<CollectionFolder>> =
        getAllFolders().map { folders -> folders.filter { it.parentName == parentName } }

    // ── AI helper ────────────────────────────────────────────────────────────

    suspend fun draftCollectionFromPrompt(prompt: String, url: String, openAiApiKey: String): String {
        val urlText = openAiApi.getUrlContent(url)
        val promptWithArticle = """
            $prompt
            $urlText
        """.trimIndent()
        println("Drafting collection from prompt: $promptWithArticle")
        return openAiApi.prompt(promptWithArticle, openAiApiKey)
    }
}