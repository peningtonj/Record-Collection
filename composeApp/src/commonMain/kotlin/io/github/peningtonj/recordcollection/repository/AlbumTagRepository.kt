package io.github.peningtonj.recordcollection.db.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import io.github.peningtonj.recordcollection.repository.UserLibraryRepository
import io.github.peningtonj.recordcollection.repository.UserSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Reads and writes tag_ids from users/{userId}/library_albums/{albumId}  (via UserLibraryRepository).
 * Joins tag IDs against users/{userId}/tags  (via UserSessionRepository for the path).
 */
class AlbumTagRepository(
    private val firestore: FirebaseFirestore,
    private val userLibraryRepository: UserLibraryRepository,
    private val userSession: UserSessionRepository
) {
    private fun tagsRef() = firestore
        .collection("users").document(userSession.requireUserId()).collection("tags")

    /** Watches the album's tag_ids from the user library, then reactively joins against the user tags collection. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTagsForAlbum(albumId: String): Flow<List<Tag>> =
        userLibraryRepository.getTagIds(albumId)
            .flatMapLatest { tagIds ->
                if (tagIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
                tagsRef().snapshots.map { tagsSnapshot ->
                    tagsSnapshot.documents
                        .filter { it.id in tagIds }
                        .mapNotNull { doc ->
                            val key = doc.get("tag_key") as? String ?: return@mapNotNull null
                            val value = doc.get("tag_value") as? String ?: return@mapNotNull null
                            val type = TagType.fromString(doc.get("tag_type") as? String ?: "") ?: TagType.USER
                            Tag(id = doc.id, key = key, value = value, type = type)
                        }
                }
            }

    fun addTagToAlbum(albumId: String, tagId: String) = runBlocking {
        userLibraryRepository.addTagId(albumId, tagId)
    }

    fun removeTagFromAlbum(albumId: String, tagId: String) = runBlocking {
        userLibraryRepository.removeTagId(albumId, tagId)
    }
}
