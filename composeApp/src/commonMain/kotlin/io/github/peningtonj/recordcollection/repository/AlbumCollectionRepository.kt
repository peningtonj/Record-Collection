package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.CollectionDocument
import io.github.peningtonj.recordcollection.db.domain.CollectionFolder
import io.github.peningtonj.recordcollection.db.domain.CollectionFolderDocument
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock

class AlbumCollectionRepository(
    private val firestore: FirebaseFirestore,
    private val openAiApi: OpenAiApi,
    private val userSession: UserSessionRepository
) {
    // ── Scoped collection helpers ─────────────────────────────────────────────

    /** Emits a CollectionReference each time the user ID becomes available (or changes). */
    private fun collectionsFlow() = userSession.userIdFlow.mapNotNull { it }
        .map { userId -> firestore.collection("users").document(userId).collection("collections") }

    private fun foldersFlow() = userSession.userIdFlow.mapNotNull { it }
        .map { userId -> firestore.collection("users").document(userId).collection("collection_folders") }

    /** Returns the collections CollectionReference synchronously — for writes only (userId must be set). */
    private fun collectionsRef() = firestore
        .collection("users").document(userSession.requireUserId()).collection("collections")

    private fun foldersRef() = firestore
        .collection("users").document(userSession.requireUserId()).collection("collection_folders")

    // ── Collection CRUD (Firestore) ───────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllCollections(): Flow<List<AlbumCollection>> {
        LoggingUtils.logFirebaseQuery("collections", "snapshots (all collections)")
        return collectionsFlow().flatMapLatest { ref ->
            ref.snapshots.map { snapshot ->
                LoggingUtils.logFirebaseResult("collections", "getAllCollections", snapshot.documents.size)
                snapshot.documents.mapNotNull { it.data<CollectionDocument?>()?.toAlbumCollection() }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCollectionByName(name: String): Flow<AlbumCollection?> {
        LoggingUtils.logFirebaseQuery("collection", "snapshot by name", mapOf("name" to name))
        return collectionsFlow().flatMapLatest { ref ->
            ref.document(name).snapshots.map { snapshot ->
                LoggingUtils.logFirebaseResult("collection", "getCollectionByName(name=$name)", if (snapshot.exists) 1 else 0)
                if (snapshot.exists) snapshot.data<CollectionDocument?>()?.toAlbumCollection() else null
            }
        }
    }

    suspend fun createCollection(name: String, description: String? = null, parent: String? = null) {
        val now = Clock.System.now().epochSeconds
        LoggingUtils.logFirebaseWrite("collection", "set (create)", name)
        collectionsRef().document(name).set(
            CollectionDocument(name = name, description = description, createdAt = now, updatedAt = now, parentName = parent, albums = emptyList())
        )
    }

    suspend fun updateCollectionByName(newCollectionDetails: AlbumCollection, existingName: String) {
        val now = Clock.System.now().epochSeconds
        if (newCollectionDetails.name != existingName) {
            val existing = collectionsRef().document(existingName).get().data<CollectionDocument?>()
            collectionsRef().document(newCollectionDetails.name).set(
                CollectionDocument(
                    name = newCollectionDetails.name, description = newCollectionDetails.description,
                    createdAt = existing?.createdAt ?: now, updatedAt = now,
                    parentName = newCollectionDetails.parentName, albums = existing?.albums ?: emptyList()
                )
            )
            collectionsRef().document(existingName).delete()
        } else {
            collectionsRef().document(existingName).set(
                mapOf("description" to newCollectionDetails.description, "parent_name" to newCollectionDetails.parentName, "updated_at" to now),
                merge = true
            )
        }
    }

    suspend fun deleteCollection(name: String) {
        LoggingUtils.logFirebaseWrite("collection", "delete", name)
        collectionsRef().document(name).delete()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCollectionCount(): Flow<Long> =
        collectionsFlow().flatMapLatest { ref -> ref.snapshots.map { it.documents.size.toLong() } }

    fun getAllTopLevelCollections(): Flow<List<AlbumCollection>> =
        getAllCollections().map { it.filter { c -> c.parentName == null } }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCollectionsByFolder(folderName: String): Flow<List<AlbumCollection>> =
        collectionsFlow().flatMapLatest { ref ->
            ref.where { "parent_name" equalTo folderName }.snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { it.data<CollectionDocument?>()?.toAlbumCollection() }
            }
        }

    // ── Folder operations (Firestore) ─────────────────────────────────────────

    suspend fun createFolder(folder: CollectionFolder) {
        foldersRef().document(folder.folderName).set(CollectionFolderDocument(folderName = folder.folderName, parent = folder.parentName))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getAllFolders(): Flow<List<CollectionFolder>> =
        foldersFlow().flatMapLatest { ref ->
            ref.snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { it.data<CollectionFolderDocument?>()?.toDomain() }
            }
        }

    fun getAllTopLevelFolders(): Flow<List<CollectionFolder>> =
        getAllFolders().map { it.filter { f -> f.parentName == null } }

    fun getFoldersByParent(parentName: String): Flow<List<CollectionFolder>> =
        getAllFolders().map { it.filter { f -> f.parentName == parentName } }

    // ── AI helper ────────────────────────────────────────────────────────────

    suspend fun draftCollectionFromPrompt(prompt: String, url: String, openAiApiKey: String): String {
        val urlText = openAiApi.getUrlContent(url)
        return openAiApi.prompt("$prompt\n$urlText".trimIndent(), openAiApiKey)
    }
}