package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.AlbumRating
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.db.mapper.ArtistMapper
import io.github.peningtonj.recordcollection.db.mapper.ArtistMapper.toDomain
import io.github.peningtonj.recordcollection.db.mapper.RatingMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.map

class RatingRepository(
    private val database: RecordCollectionDatabase,
    ) {

    fun addRating(albumId: String, rating: Long) {
        // Add rating and update aggregate
        database.ratingsQueries.insertOrUpdateRating(
            album_id = albumId,
            rating = rating,
        )
    }

    fun getAlbumRating(albumId: String): Flow<AlbumRating?> {
        return database.ratingsQueries
            .getRatingByAlbumId(albumId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.let { RatingMapper.toDomain(it) } }
    }

    fun getAllRatings(): Flow<List<AlbumRating>> {
        return database.ratingsQueries
            .getAllRatings()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { ratings -> ratings.map { RatingMapper.toDomain(it) } }
    }

}