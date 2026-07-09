package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking

class TagRepository(
    private val firestore: FirebaseFirestore,
    private val userSession: UserSessionRepository
) {
    /** Reactive ref — waits for userId, then streams live updates (new-user safe). */
    private fun tagsFlow() = userSession.userIdFlow.mapNotNull { it }
        .map { userId -> firestore.collection("users").document(userId).collection("tags") }

    /** Synchronous ref — writes only (userId must be set). */
    private fun tagsCollection() = firestore
        .collection("users").document(userSession.requireUserId()).collection("tags")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllTags(): Flow<List<Tag>> = tagsFlow().flatMapLatest { ref ->
        ref.snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { document ->
                val key = document.get("tag_key") as? String ?: return@mapNotNull null
                val value = document.get("tag_value") as? String ?: return@mapNotNull null
                val type = TagType.fromString(document.get("tag_type") as? String ?: "") ?: TagType.USER
                Tag(id = document.id, key = key, value = value, type = type)
            }
        }
    }

    fun getTagsByType(type: String): Flow<List<Tag>> = getAllTags()
        .map { tags -> tags.filter { it.type.value == type } }

    fun getTagsByKey(key: String): Flow<List<Tag>> = getAllTags()
        .map { tags -> tags.filter { it.key == key } }

    fun insertTag(tag: Tag) = runBlocking {
        tagsCollection().document(tag.id).set(mapOf("tag_key" to tag.key, "tag_value" to tag.value, "tag_type" to tag.type.value))
    }

    fun deleteTag(id: String) = runBlocking {
        tagsCollection().document(id).delete()
    }

    fun updateTag(tag: Tag) = runBlocking {
        tagsCollection().document(tag.id).set(mapOf("tag_key" to tag.key, "tag_value" to tag.value, "tag_type" to tag.type.value))
    }
}