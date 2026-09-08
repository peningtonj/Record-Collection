package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Manages per-user album state stored at:
 *   users/{spotifyUserId}/library_albums/{albumId}
 *
 * Besides the user's own data (`in_library`, `rating`, `tag_ids`, `added_at`) each entry
 * carries a **denormalised projection** of the album's stable Spotify metadata (name,
 * artist, release date, one image URL, …) so the whole library renders from this one
 * collection — no join against the shared `albums` catalogue. Volatile metadata (genres,
 * popularity, tracklist) is fetched on demand and cached with a TTL elsewhere.
 * See `docs/DATA_MODEL.md`.
 */
class UserLibraryRepository(
    private val firestore: FirebaseFirestore,
    private val userSession: UserSessionRepository
) {
    @Serializable
    data class LibraryAlbumDocument(
        @SerialName("album_id")  val albumId:   String       = "",
        @SerialName("in_library") val inLibrary: Boolean      = false,
        val rating:   Int?           = null,
        @SerialName("tag_ids")   val tagIds:    List<String> = emptyList(),
        @SerialName("added_at")  val addedAt:   String?      = null,

        // ── denormalised projection (stable fields; written on add + on sync) ──
        val name:                     String  = "",
        @SerialName("primary_artist") val primaryArtist: String = "",
        val artists:                  String  = "[]",   // JSON List<SimplifiedArtist>
        @SerialName("release_date")   val releaseDate:   String = "",
        @SerialName("album_type")     val albumType:     String = "",
        @SerialName("total_tracks")   val totalTracks:   Long   = 0,
        @SerialName("spotify_id")     val spotifyId:     String = "",
        @SerialName("spotify_uri")    val spotifyUri:    String = "",
        @SerialName("image_url")      val imageUrl:      String? = null,
        @SerialName("projection_fetched_at") val projectionFetchedAt: Long? = null,
    )

    /** Write-path collection ref — suspends until the user session is ready (new-user safe). */
    private suspend fun libraryRef() = firestore
        .collection("users")
        .document(userSession.awaitUserId())
        .collection("library_albums")

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Subscribes to all user library entries.
     * Waits for the userId to be available (new-user safe), then streams live updates.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllLibraryEntries(): Flow<List<LibraryAlbumDocument>> =
        userSession.userIdFlow.mapNotNull { it }.flatMapLatest { userId ->
            LoggingUtils.logFirebaseQuery("library_albums", "snapshots (all)")
            firestore.collection("users").document(userId).collection("library_albums")
                .snapshots.map { snapshot ->
                    LoggingUtils.logFirebaseResult("library_albums", "getAllLibraryEntries", snapshot.documents.size)
                    snapshot.documents.mapNotNull { doc ->
                        runCatching { doc.data<LibraryAlbumDocument>().copy(albumId = doc.id) }.getOrNull()
                    }
                }
        }

    /**
     * Subscribes to a single album's user library entry.
     * Waits for the userId to be available (new-user safe).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLibraryEntry(albumId: String): Flow<LibraryAlbumDocument?> =
        userSession.userIdFlow.mapNotNull { it }.flatMapLatest { userId ->
            LoggingUtils.logFirebaseQuery("library_albums", "snapshot", mapOf("albumId" to albumId))
            firestore.collection("users").document(userId).collection("library_albums")
                .document(albumId).snapshots.map { snapshot ->
                    if (snapshot.exists)
                        runCatching { snapshot.data<LibraryAlbumDocument>().copy(albumId = albumId) }.getOrNull()
                    else null
                }
        }

    /** Returns the tag IDs for a given album. Waits for userId (new-user safe). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTagIds(albumId: String): Flow<List<String>> =
        userSession.userIdFlow.mapNotNull { it }.flatMapLatest { userId ->
            firestore.collection("users").document(userId).collection("library_albums")
                .document(albumId).snapshots.map { snapshot ->
                    if (!snapshot.exists) emptyList()
                    else runCatching { snapshot.data<LibraryAlbumDocument>().tagIds }.getOrElse { emptyList() }
                }
        }

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Marks [album] as in the user's library and (re)writes its denormalised projection.
     * `added_at` is set only on the first add; `projection_fetched_at` is bumped every time.
     */
    suspend fun addToLibrary(album: Album) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge (addToLibrary)", album.id)
        val doc = libraryRef().document(album.id)
        val existingAddedAt = runCatching { doc.get().data<LibraryAlbumDocument>().addedAt }.getOrNull()
        val projection = AlbumMapper.toLibraryProjection(album).toMutableMap()
        projection["in_library"] = true
        if (existingAddedAt == null) {
            projection["added_at"] = album.addedAt?.toString() ?: Clock.System.now().toString()
        }
        doc.set(projection, merge = true)
    }

    /** Refreshes just the projection for an album already known — used by sync. */
    suspend fun refreshProjection(album: Album) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge (refreshProjection)", album.id)
        libraryRef().document(album.id).set(AlbumMapper.toLibraryProjection(album), merge = true)
    }

    suspend fun removeFromLibrary(albumId: String) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge in_library=false", albumId)
        libraryRef().document(albumId).set(mapOf("in_library" to false), merge = true)
    }

    suspend fun setRating(albumId: String, rating: Int) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge (setRating)", albumId, mapOf("rating" to rating))
        libraryRef().document(albumId).set(mapOf("rating" to rating), merge = true)
    }

    /** Atomic — `FieldValue.arrayUnion` merges server-side, so concurrent tag edits can't lose each other. */
    suspend fun addTagId(albumId: String, tagId: String) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge arrayUnion (addTagId)", albumId, mapOf("tagId" to tagId))
        libraryRef().document(albumId).set(mapOf("tag_ids" to FieldValue.arrayUnion(tagId)), merge = true)
    }

    /** Atomic — see [addTagId]. */
    suspend fun removeTagId(albumId: String, tagId: String) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge arrayRemove (removeTagId)", albumId, mapOf("tagId" to tagId))
        libraryRef().document(albumId).set(mapOf("tag_ids" to FieldValue.arrayRemove(tagId)), merge = true)
    }
}



