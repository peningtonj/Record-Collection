package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Album_ratings
import io.github.peningtonj.recordcollection.db.domain.AlbumRating

object RatingMapper {
    fun toDomain(rating: Album_ratings) : AlbumRating {
        return AlbumRating(
            albumId = rating.album_id,
            rating = rating.rating?.toInt()
        )
    }
}