package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Artists
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import kotlinx.serialization.json.Json

object ArtistMapper {
    fun toDomain(entity: SimplifiedArtistDto) : SimplifiedArtist {
        return SimplifiedArtist(
            id = entity.id,
            name = entity.name,
            uri = entity.uri,
            externalUrls = entity.externalUrls ?: emptyMap(),
            href = entity.href,
            type = entity.type,
        )
    }

    fun toDomain(entity: FullArtistDto) : Artist {
        return Artist(
            followers = entity.followers.total.toLong(),
            genres = entity.genres,
            href = entity.href,
            id = entity.id,
            images = entity.images.map { ImageMapper.toDomain(it) },
            name = entity.name,
            popularity = entity.popularity.toLong(),
            type = entity.type,
            uri = entity.uri,
        )
    }

    fun toDomain(entity: Artists) : Artist {
        return Artist(
            followers = entity.followers,
            genres = entity.genres,
            href = entity.href,
            id = entity.id,
            images = Json.decodeFromString<List<ImageDto>>(entity.images)
                .map { ImageMapper.toDomain(it) },
            name = entity.name,
            popularity = entity.popularity,
            type = entity.type,
            uri = entity.uri
        )
    }
}