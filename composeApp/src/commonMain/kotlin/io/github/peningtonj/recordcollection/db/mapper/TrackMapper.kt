package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Track_entity
import io.github.peningtonj.recordcollection.db.Album_entity
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackTrack
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import kotlinx.serialization.json.Json

object TrackMapper {
    fun toDomain(entity: PlaybackTrack) : Track {
        return Track(
            id = entity.id,
            name = entity.name,
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            album = AlbumMapper.toDomain(entity.album),
            durationMs = entity.durationMs.toLong(),
            imageUrl = null,
            isExplicit = false,
            trackNumber = 0,
            discNumber = 0,
            albumId = entity.album.id
        )
    }

    fun toDomain(entity: Track_entity, albumEntity: Album_entity?) : Track {
        return Track(
            id = entity.id,
            name = entity.name,
            artists = Json.decodeFromString<List<SimplifiedArtistDto>>(entity.artists)
                .map { ArtistMapper.toDomain(it) },
            album = albumEntity?.let { AlbumMapper.toDomain(it) },
            durationMs = entity.duration_ms,
            trackNumber = entity.track_number,
            discNumber = entity.disc_number,
            isExplicit = entity.is_explicit == 1L,
            albumId = entity.album_id
        )
    }
}