package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollectionInfo
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbum
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbumEntry
import io.github.peningtonj.recordcollection.db.domain.CollectionDocument
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class CollectionAlbumRepository(
    private val firestore: FirebaseFirestore,
    private val albumRepository: AlbumRepository,
    private val userSession: UserSessionRepository,
    private val userLibraryRepository: UserLibraryRepository
) {
    /** Reactive — waits for userId; safe for new users. */
    private fun collectionsFlow() = userSession.userIdFlow.mapNotNull { it }
        .map { userId -> firestore.collection("users").document(userId).collection("collections") }

    /** Write-path ref — suspends until the user session is ready (new-user safe). */
    private suspend fun collectionsRef() = firestore
        .collection("users").document(userSession.awaitUserId()).collection("collections")

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Renders a collection from the denormalised projection on each entry — no join
     * against shared `albums` (see docs/DATA_MODEL.md). Rating / in-library come from the
     * one cheap per-user `library_albums` listener. Entries written before the projection
     * existed (blank `name`) fall back to `albums` until `backfill_library_projection.py`
     * runs.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAlbumsInCollection(collectionName: String): Flow<List<CollectionAlbum>> {
        LoggingUtils.logFirebaseQuery("collections", "snapshot (albums array)", mapOf("collectionName" to collectionName))
        return collectionsFlow().flatMapLatest { ref ->
            ref.document(collectionName).snapshots.flatMapLatest { snapshot ->
                val entries = (snapshot.data<CollectionDocument?>()?.albums ?: emptyList()).sortedBy { it.position }
                LoggingUtils.logFirebaseResult("collections", "getAlbumsInCollection(collection=$collectionName)", entries.size)
                if (entries.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val staleIds = entries.filter { it.name.isBlank() }.map { it.albumId }
                val staleAlbumsFlow =
                    if (staleIds.isEmpty()) flowOf(emptyMap())
                    else albumRepository.getAlbumsByIds(staleIds).map { it.associateBy(Album::id) }

                combine(staleAlbumsFlow, userLibraryRepository.getAllLibraryEntries()) { staleAlbums, libraryEntries ->
                    val libraryMap = libraryEntries.associateBy { it.albumId }
                    entries.mapNotNull { entry ->
                        val album = if (entry.name.isNotBlank()) AlbumMapper.collectionEntryToDomain(entry)
                                    else staleAlbums[entry.albumId]
                        if (album == null) {
                            LoggingUtils.w(LoggingUtils.Category.REPOSITORY, "Album '${entry.albumId}' in collection '$collectionName' not found")
                            null
                        } else {
                            val lib = libraryMap[entry.albumId]
                            CollectionAlbum(
                                collectionName = collectionName,
                                album = album.copy(rating = lib?.rating, inLibrary = lib?.inLibrary ?: false),
                                position = entry.position,
                                addedAt = Instant.fromEpochSeconds(entry.addedAt)
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAlbumCountInCollection(collectionName: String): Flow<Long> {
        LoggingUtils.logFirebaseQuery("collection", "snapshot (count)", mapOf("collectionName" to collectionName))
        return collectionsFlow().flatMapLatest { ref ->
            ref.document(collectionName).snapshots.map { snapshot ->
                val count = snapshot.data<CollectionDocument?>()?.albums?.size?.toLong() ?: 0L
                LoggingUtils.logFirebaseResult("collection", "getAlbumCountInCollection(collection=$collectionName)", count.toInt())
                count
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun isAlbumInCollection(collectionName: String, albumId: String): Flow<Boolean> {
        LoggingUtils.logFirebaseQuery("collection", "snapshot (isAlbumInCollection)", mapOf("collectionName" to collectionName, "albumId" to albumId))
        return collectionsFlow().flatMapLatest { ref ->
            ref.document(collectionName).snapshots.map { snapshot ->
                val exists = snapshot.data<CollectionDocument?>()?.albums?.any { it.albumId == albumId } ?: false
                LoggingUtils.logFirebaseResult("collection", "isAlbumInCollection(album=$albumId)", if (exists) 1 else 0)
                exists
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCollectionsForAlbum(albumId: String): Flow<List<AlbumCollectionInfo>> {
        LoggingUtils.logFirebaseQuery("collection", "snapshots (getCollectionsForAlbum)", mapOf("albumId" to albumId))
        return collectionsFlow().flatMapLatest { ref ->
            ref.snapshots.map { snapshot ->
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
    }

    // ── Writes ────────────────────────────────────────────────────────────────
    //
    // The `albums` array is written as List<Map<String, Any?>> of plain primitives —
    // NOT set(mapOf("albums" to List<CollectionAlbumEntry>)). GitLive's Kotlin/JS
    // serializer boxes the entries' `Long` fields (rejected by the JS SDK) and can turn
    // an empty list into `[null]`. See db/FirestoreMap.kt.

    private suspend fun mutateAlbums(
        collectionName: String,
        op: String,
        transform: (List<CollectionAlbumEntry>) -> List<CollectionAlbumEntry>,
    ) {
        val docRef = collectionsRef().document(collectionName)
        val current = docRef.get().data<CollectionDocument?>()?.albums ?: emptyList()
        val updated = transform(current)
        if (updated == current) return
        LoggingUtils.logFirebaseWrite("collection", "set merge ($op)", collectionName, mapOf("count" to updated.size))
        docRef.set(mapOf("albums" to updated.map(AlbumMapper::collectionEntryToFirestoreMap)), merge = true)
    }

    suspend fun addAlbumToCollection(collectionName: String, album: Album) =
        mutateAlbums(collectionName, "add album") { current ->
            if (current.any { it.albumId == album.id }) return@mutateAlbums current
            val nextPosition = (current.maxOfOrNull { it.position } ?: 0) + 1
            current + AlbumMapper.toCollectionEntry(album, nextPosition, Clock.System.now().epochSeconds)
        }

    suspend fun removeAlbumFromCollection(collectionName: String, albumId: String) =
        mutateAlbums(collectionName, "remove album") { current -> current.filter { it.albumId != albumId } }

    suspend fun reorderAlbums(collectionName: String, albumPositions: List<Pair<String, Int>>) {
        val positionMap = albumPositions.toMap()
        mutateAlbums(collectionName, "reorder") { current ->
            current.map { entry -> positionMap[entry.albumId]?.let { entry.copy(position = it) } ?: entry }
        }
    }

    suspend fun clearCollection(collectionName: String) {
        LoggingUtils.logFirebaseWrite("collection", "set merge (clear albums)", collectionName)
        collectionsRef().document(collectionName).set(mapOf("albums" to emptyList<Any?>()), merge = true)
    }
}