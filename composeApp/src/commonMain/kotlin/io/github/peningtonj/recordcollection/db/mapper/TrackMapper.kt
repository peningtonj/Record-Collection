package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.domain.TrackDocument
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackTrack
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedTrackDto
import io.github.peningtonj.recordcollection.network.spotify.model.TrackDto
import kotlinx.serialization.json.Json

/** Represents a Firestore track document. Used for both saving and reading from Firestore. */

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
            albumId = generateAlbumId(entity.album.name, entity.album.artists.firstOrNull()?.name),
            spotifyUri = entity.uri,
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
            albumId = generateAlbumId(album),
            spotifyUri = entity.uri,
        )
    }


    fun toDocument(entity: SimplifiedTrackDto, albumId: String): TrackDocument = TrackDocument(
        name = entity.name,
        albumId = albumId,
        trackNumber = entity.trackNumber.toLong(),
        durationMs = entity.durationMs.toLong(),
        spotifyUri = entity.uri,
        artists = Json.encodeToString(entity.artists),
        previewUrl = entity.previewUrl,
        primaryArtist = entity.artists.firstOrNull()?.name ?: "Unknown Artist",
        isExplicit = entity.explicit,
        discNumber = entity.discNumber.toLong(),
        isSaved = false
    )

    fun toDomain(id: String, doc: TrackDocument): Track {
        val artists = try {
            Json.decodeFromString<List<SimplifiedArtistDto>>(doc.artists)
                .map { ArtistMapper.toDomain(it) }
        } catch (e: Exception) { emptyList() }
        return Track(
            id = id,
            name = doc.name,
            albumId = doc.albumId,
            artists = artists,
            durationMs = doc.durationMs,
            trackNumber = doc.trackNumber,
            discNumber = doc.discNumber,
            spotifyUri = doc.spotifyUri,
            isExplicit = doc.isExplicit,
            isSaved = doc.isSaved,
            imageUrl = null
        )
    }

}