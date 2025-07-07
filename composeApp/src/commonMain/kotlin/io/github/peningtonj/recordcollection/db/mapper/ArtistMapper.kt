package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Artist_entity
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import kotlinx.serialization.json.Json

object ArtistMapper {
    fun toDomain(entity: SimplifiedArtistDto) : SimplifiedArtist {
        return SimplifiedArtist(
            id = entity.id,
            name = entity.name,
            uri = entity.uri,
            externalUrls = entity.externalUrls
        )
    }

    fun toDomain(entity: Artist_entity) : Artist {
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