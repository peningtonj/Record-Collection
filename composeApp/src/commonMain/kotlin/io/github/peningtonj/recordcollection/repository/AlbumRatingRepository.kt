package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.domain.AlbumRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

class RatingRepository(
    private val firestore: FirebaseFirestore
) {
    private val albumsRef = firestore.collection("albums")

    @Serializable
    private data class RatingField(val rating: Int? = null)

    suspend fun addRating(albumId: String, rating: Int) {
        albumsRef.document(albumId).set(mapOf("rating" to rating), merge = true)
    }

    fun getAlbumRating(albumId: String): Flow<AlbumRating?> =
        albumsRef.document(albumId).snapshots.map { snapshot ->
            if (snapshot.exists) {
                val rating = runCatching { snapshot.data<RatingField>().rating }.getOrNull()
                AlbumRating(albumId = albumId, rating = rating)
            } else null
        }

    fun getAllRatings(): Flow<List<AlbumRating>> =
        albumsRef.snapshots.map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val rating = runCatching { doc.data<RatingField>().rating }.getOrNull()
                if (rating != null) AlbumRating(albumId = doc.id, rating = rating) else null
            }
        }
}