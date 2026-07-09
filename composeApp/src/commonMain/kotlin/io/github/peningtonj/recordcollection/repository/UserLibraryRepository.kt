package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
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
 * Fields stored per entry:
 *   in_library : Boolean         – whether the user has added this album to their library
 *   rating     : Int?            – user rating (null = not rated)
 *   tag_ids    : List<String>    – IDs of tags applied to this album by this user
 *   added_at   : String?         – ISO-8601 timestamp when the album was added
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
        @SerialName("added_at")  val addedAt:   String?      = null
    )

    private fun libraryRef() = firestore
        .collection("users")
        .document(userSession.requireUserId())
        .collection("library_albums")

    // ── Reads ─────────────────────────────────────────────────────────────────

    /**
     * Subscribes to all user library entries.
     * Waits for the userId to be available (new-user safe), then streams live updates.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllLibraryEntries(): Flow<List<LibraryAlbumDocument>> =
        userSession.userIdFlow.mapNotNull { it }.flatMapLatest { userId ->
            LoggingUtils.logFirebaseQuery("users/$userId/library_albums", "snapshots (all)")
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
            LoggingUtils.logFirebaseQuery("users/$userId/library_albums", "snapshot", mapOf("albumId" to albumId))
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

    suspend fun setInLibrary(albumId: String, inLibrary: Boolean) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge (setInLibrary)", albumId, mapOf("inLibrary" to inLibrary))
        if (inLibrary) {
            // Only set added_at on the initial add – preserve it on subsequent calls
            val existing = libraryRef().document(albumId).get()
            val existingAddedAt = runCatching { existing.data<LibraryAlbumDocument>().addedAt }.getOrNull()
            val map = buildMap<String, Any?> {
                put("in_library", true)
                if (existingAddedAt == null) put("added_at", Clock.System.now().toString())
            }
            libraryRef().document(albumId).set(map, merge = true)
        } else {
            libraryRef().document(albumId).set(mapOf("in_library" to false), merge = true)
        }
    }

    suspend fun setRating(albumId: String, rating: Int) {
        LoggingUtils.logFirebaseWrite("library_albums", "set merge (setRating)", albumId, mapOf("rating" to rating))
        libraryRef().document(albumId).set(mapOf("rating" to rating), merge = true)
    }

    suspend fun addTagId(albumId: String, tagId: String) {
        val existing = libraryRef().document(albumId).get()
        val tagIds = runCatching { existing.data<LibraryAlbumDocument>().tagIds }.getOrElse { emptyList() }
        if (tagId !in tagIds) {
            LoggingUtils.logFirebaseWrite("library_albums", "set merge (addTagId)", albumId, mapOf("tagId" to tagId))
            libraryRef().document(albumId).set(mapOf("tag_ids" to tagIds + tagId), merge = true)
        }
    }

    suspend fun removeTagId(albumId: String, tagId: String) {
        val existing = libraryRef().document(albumId).get()
        val tagIds = runCatching { existing.data<LibraryAlbumDocument>().tagIds }.getOrElse { emptyList() }
        LoggingUtils.logFirebaseWrite("library_albums", "set merge (removeTagId)", albumId, mapOf("tagId" to tagId))
        libraryRef().document(albumId).set(mapOf("tag_ids" to tagIds.filter { it != tagId }), merge = true)
    }
}



