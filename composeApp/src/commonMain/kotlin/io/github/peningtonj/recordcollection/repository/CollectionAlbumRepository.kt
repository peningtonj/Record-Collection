package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.AlbumCollectionInfo
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbum
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbumEntry
import io.github.peningtonj.recordcollection.db.domain.CollectionDocument
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class CollectionAlbumRepository(
    private val firestore: FirebaseFirestore,
    private val albumRepository: AlbumRepository,
) {
    private val collectionsRef = firestore.collection("collections")

    // ── Reads ─────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAlbumsInCollection(collectionName: String): Flow<List<CollectionAlbum>> {
        LoggingUtils.logFirebaseQuery("collections", "snapshot (albums array)", mapOf("collectionName" to collectionName))
        return collectionsRef.document(collectionName).snapshots
            .flatMapLatest { snapshot ->
                val entries = snapshot.data<CollectionDocument?>()?.albums ?: emptyList()
                LoggingUtils.logFirebaseResult("collections", "getAlbumsInCollection(collection=$collectionName)", entries.size)
                if (entries.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val sorted = entries.sortedBy { it.position }
                val albumIds = sorted.map { it.albumId }

                albumRepository.getAlbumsByIds(albumIds).map { albums ->
                    val albumMap = albums.associateBy { it.id }
                    sorted.mapNotNull { entry ->
                        val album = albumMap[entry.albumId]
                        if (album == null) {
                            LoggingUtils.w(
                                LoggingUtils.Category.REPOSITORY,
                                "Album '${entry.albumId}' in collection '$collectionName' not found"
                            )
                            null
                        } else {
                            CollectionAlbum(
                                collectionName = collectionName,
                                album = album,
                                position = entry.position,
                                addedAt = Instant.fromEpochSeconds(entry.addedAt)
                            )
                        }
                    }
                }
            }
    }

    fun getAlbumCountInCollection(collectionName: String): Flow<Long> {
        LoggingUtils.logFirebaseQuery("collection", "snapshot (count)", mapOf("collectionName" to collectionName))
        return collectionsRef.document(collectionName).snapshots
            .map { snapshot ->
                val count = snapshot.data<CollectionDocument?>()?.albums?.size?.toLong() ?: 0L
                LoggingUtils.logFirebaseResult("collection", "getAlbumCountInCollection(collection=$collectionName)", count.toInt())
                count
            }
    }

    fun isAlbumInCollection(collectionName: String, albumId: String): Flow<Boolean> {
        LoggingUtils.logFirebaseQuery("collection", "snapshot (isAlbumInCollection)", mapOf("collectionName" to collectionName, "albumId" to albumId))
        return collectionsRef.document(collectionName).snapshots
            .map { snapshot ->
                val exists = snapshot.data<CollectionDocument?>()?.albums?.any { it.albumId == albumId } ?: false
                LoggingUtils.logFirebaseResult("collection", "isAlbumInCollection(album=$albumId)", if (exists) 1 else 0)
                exists
            }
    }

    /** Subscribes to all collection documents and returns those containing the given albumId. */
    fun getCollectionsForAlbum(albumId: String): Flow<List<AlbumCollectionInfo>> {
        LoggingUtils.logFirebaseQuery("collection", "snapshots (getCollectionsForAlbum)", mapOf("albumId" to albumId))
        return collectionsRef.snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val document = doc.data<CollectionDocument?>() ?: return@mapNotNull null
                    val entry = document.albums.find { it.albumId == albumId } ?: return@mapNotNull null
                    AlbumCollectionInfo(
                        collection = document.toAlbumCollection(),
                        position = entry.position,
                        addedAt = Instant.fromEpochSeconds(entry.addedAt)
                    )
                }.also {
                    LoggingUtils.logFirebaseResult("collection", "getCollectionsForAlbum(album=$albumId)", it.size)
                }
            }
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    suspend fun addAlbumToCollection(collectionName: String, albumId: String) {
        LoggingUtils.logFirebaseQuery("collection", "get for addAlbumToCollection", mapOf("collectionName" to collectionName))
        val docRef = collectionsRef.document(collectionName)
        val currentAlbums = docRef.get().data<CollectionDocument?>()?.albums ?: emptyList()

        if (currentAlbums.any { it.albumId == albumId }) {
            LoggingUtils.logFirebaseQuery("collection", "addAlbumToCollection – already present, skipping", mapOf("albumId" to albumId))
            return
        }

        val nextPosition = (currentAlbums.maxOfOrNull { it.position } ?: 0) + 1
        val updatedAlbums = currentAlbums + CollectionAlbumEntry(
            albumId = albumId,
            position = nextPosition,
            addedAt = Clock.System.now().epochSeconds
        )
        LoggingUtils.logFirebaseWrite("collection", "set merge (add album)", collectionName, mapOf("albumId" to albumId, "position" to nextPosition))
        docRef.set(mapOf("albums" to updatedAlbums), merge = true)
    }

    suspend fun removeAlbumFromCollection(collectionName: String, albumId: String) {
        LoggingUtils.logFirebaseQuery("collection", "get for removeAlbumFromCollection", mapOf("collectionName" to collectionName))
        val docRef = collectionsRef.document(collectionName)
        val currentAlbums = docRef.get().data<CollectionDocument?>()?.albums ?: emptyList()
        val updatedAlbums = currentAlbums.filter { it.albumId != albumId }
        LoggingUtils.logFirebaseWrite("collection", "set merge (remove album)", collectionName, mapOf("albumId" to albumId))
        docRef.set(mapOf("albums" to updatedAlbums), merge = true)
    }

    suspend fun reorderAlbums(collectionName: String, albumPositions: List<Pair<String, Int>>) {
        LoggingUtils.logFirebaseQuery("collection", "get for reorderAlbums", mapOf("collectionName" to collectionName))
        val docRef = collectionsRef.document(collectionName)
        val currentAlbums = docRef.get().data<CollectionDocument?>()?.albums ?: emptyList()
        val positionMap = albumPositions.toMap()
        val updatedAlbums = currentAlbums.map { entry ->
            positionMap[entry.albumId]?.let { entry.copy(position = it) } ?: entry
        }
        LoggingUtils.logFirebaseWrite("collection", "set merge (reorder)", collectionName, mapOf("count" to albumPositions.size))
        docRef.set(mapOf("albums" to updatedAlbums), merge = true)
    }

    suspend fun clearCollection(collectionName: String) {
        LoggingUtils.logFirebaseWrite("collection", "set merge (clear albums)", collectionName)
        collectionsRef.document(collectionName).set(mapOf("albums" to emptyList<CollectionAlbumEntry>()), merge = true)
    }
}