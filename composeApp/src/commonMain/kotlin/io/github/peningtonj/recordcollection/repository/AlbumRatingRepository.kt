package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.domain.AlbumRating
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Thin wrapper around UserLibraryRepository that exposes the rating sub-domain. */
class RatingRepository(
    private val userLibraryRepository: UserLibraryRepository
) {
    suspend fun addRating(albumId: String, rating: Int) =
        userLibraryRepository.setRating(albumId, rating)

    fun getAlbumRating(albumId: String): Flow<AlbumRating?> =
        userLibraryRepository.getLibraryEntry(albumId).map { entry ->
            entry?.let { AlbumRating(albumId = albumId, rating = it.rating) }
        }

    fun getAllRatings(): Flow<List<AlbumRating>> =
        userLibraryRepository.getAllLibraryEntries().map { entries ->
            entries.mapNotNull { entry ->
                entry.rating?.let { AlbumRating(albumId = entry.albumId, rating = it) }
            }
        }
}