package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Tracks
import io.github.peningtonj.recordcollection.db.Albums
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackTrack
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedTrackDto
import io.github.peningtonj.recordcollection.network.spotify.model.TrackDto
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
            albumId = entity.album.id,
            spotifyUri = entity.uri
        )
    }

    fun toDomain(entity: TrackDto) : Track {
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
            albumId = entity.album.id,
            spotifyUri = entity.uri
        )
    }

    fun toDomain(entity: SimplifiedTrackDto, album: Album) : Track {
        return Track(
            id = entity.id,
            name = entity.name,
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            album = album,
            durationMs = entity.durationMs.toLong(),
            imageUrl = null,
            isExplicit = entity.explicit,
            trackNumber = entity.trackNumber.toLong(),
            discNumber = entity.discNumber.toLong(),
            albumId = album.id,
            spotifyUri = entity.uri
        )
    }

    fun toDomain(entity: Tracks, albumEntity: Albums?) : Track {
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
            albumId = entity.album_id,
            spotifyUri = entity.spotify_uri
        )
    }
}