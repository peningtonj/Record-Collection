package io.github.peningtonj.recordcollection.db.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class AlbumTagRepository(
    private val firestore: FirebaseFirestore
) {
    private val albumsRef = firestore.collection("albums")
    private val tagsRef = firestore.collection("tags")

    @Serializable
    private data class TagIdsField(
        @SerialName("tag_ids") val tagIds: List<String> = emptyList()
    )

    // Watches the album's tag_ids array, then reactively joins against the tags collection.
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTagsForAlbum(albumId: String): Flow<List<Tag>> =
        albumsRef.document(albumId).snapshots
            .flatMapLatest { snapshot ->
                if (!snapshot.exists) return@flatMapLatest flowOf(emptyList())
                val tagIds = runCatching { snapshot.data<TagIdsField>().tagIds }.getOrElse { emptyList() }
                if (tagIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
                tagsRef.snapshots.map { tagsSnapshot ->
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

    // Kept non-suspend (runBlocking) so TagService callers need no changes.
    fun addTagToAlbum(albumId: String, tagId: String) = runBlocking {
        val existing = albumsRef.document(albumId).get()
        val tagIds = runCatching { existing.data<TagIdsField>().tagIds }.getOrElse { emptyList() }
        if (tagId !in tagIds) {
            albumsRef.document(albumId).set(mapOf("tag_ids" to tagIds + tagId), merge = true)
        }
    }

    fun removeTagFromAlbum(albumId: String, tagId: String) = runBlocking {
        val existing = albumsRef.document(albumId).get()
        val tagIds = runCatching { existing.data<TagIdsField>().tagIds }.getOrElse { emptyList() }
        albumsRef.document(albumId).set(mapOf("tag_ids" to tagIds.filter { it != tagId }), merge = true)
    }
}
