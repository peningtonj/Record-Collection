package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class TagRepository(
    private val firestore: FirebaseFirestore,
    private val userSession: UserSessionRepository
) {
    /** Reactive ref — waits for userId, then streams live updates (new-user safe). */
    private fun tagsFlow() = userSession.userIdFlow.mapNotNull { it }
        .map { userId -> firestore.collection("users").document(userId).collection("tags") }

    /** Write-path ref — suspends until the user session is ready (new-user safe). */
    private suspend fun tagsCollection() = firestore
        .collection("users").document(userSession.awaitUserId()).collection("tags")

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllTags(): Flow<List<Tag>> = tagsFlow().flatMapLatest { ref ->
        ref.snapshots.map { snapshot ->
            LoggingUtils.logFirebaseResult("tags", "snapshots (all)", snapshot.documents.size)
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

    suspend fun insertTag(tag: Tag) {
        LoggingUtils.logFirebaseWrite("tags", "set (insertTag)", tag.id)
        tagsCollection().document(tag.id).set(mapOf("tag_key" to tag.key, "tag_value" to tag.value, "tag_type" to tag.type.value))
    }

    suspend fun deleteTag(id: String) {
        LoggingUtils.logFirebaseWrite("tags", "delete", id)
        tagsCollection().document(id).delete()
    }

    suspend fun updateTag(tag: Tag) {
        LoggingUtils.logFirebaseWrite("tags", "set (updateTag)", tag.id)
        tagsCollection().document(tag.id).set(mapOf("tag_key" to tag.key, "tag_value" to tag.value, "tag_type" to tag.type.value))
    }
}