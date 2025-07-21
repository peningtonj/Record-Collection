package io.github.peningtonj.recordcollection.testDataFactory

import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto

object TestSimplifiedArtistFactory {
    fun simplifiedAristDto(
        name: String = "Test Artist",
        id: String = "artist-id"
    ): SimplifiedArtistDto {
        return SimplifiedArtistDto(
            id = id,
            name = name,
            type = "artist",
            uri = "spotify:artist:$id",
            href = "http://spotify.api/artist/$id"
        )
    }

    fun simplifiedArtist(
        name: String = "Test Artist",
        id: String = "artist-id"
    ): SimplifiedArtist {
        return SimplifiedArtist(
            id = id,
            name = name,
            type = "artist",
            uri = "spotify:artist:$id",
            href = "http://spotify.api/artist/$id",
            externalUrls = emptyMap()
        )
    }
}