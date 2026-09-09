package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto

object ArtistMapper {
    /**
     * `artists/{id}` as a plain-primitive map — not `set(ArtistDocument)`, whose `Long`
     * followers/popularity the GitLive JS SDK rejects. See db/FirestoreMap.kt.
     */
    fun toDocumentMap(
        id: String,
        followers: Int,
        genres: List<String>,
        href: String,
        imagesJson: String,
        name: String,
        popularity: Int,
        type: String,
        uri: String,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "followers" to followers,
        "genres" to genres,
        "href" to href,
        "images" to imagesJson,
        "name" to name,
        "popularity" to popularity,
        "type" to type,
        "uri" to uri,
    )

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
}