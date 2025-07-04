package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Album_entity
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import kotlinx.serialization.json.Json
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object AlbumMapper {

    fun toDomain(entity: Album_entity): Album {
        return Album(
            id = entity.id,
            name = entity.name,
            primaryArtist = entity.primary_artist,
            artists = Json.decodeFromString<List<SimplifiedArtistDto>>(entity.artists)
                .map { ArtistMapper.toDomain(it) },
            releaseDate = parseReleaseDate(entity.release_date.toString()),
            totalTracks = entity.total_tracks.toInt(),
            spotifyUri = entity.spotify_uri,
            addedAt = entity.added_at,
            albumType = entity.album_type,
            images = Json.decodeFromString<List<ImageDto>>(entity.images)
                .map { ImageMapper.toDomain(it) },
            updatedAt = entity.updated_at
        )
    }

    fun toDomain(entity: AlbumDto): Album {
        return Album(
            id = entity.id,
            name = entity.name,
            primaryArtist = entity.artists.firstOrNull()?.name,
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            releaseDate = parseReleaseDate(entity.releaseDate),
            totalTracks = entity.totalTracks,
            spotifyUri = entity.uri,
            albumType = entity.albumType.name,
            images = entity.images
                .map { ImageMapper.toDomain(it) },
        )
    }

    private fun parseReleaseDate(releaseDate: String): Date {
        val formats = listOf(
            "yyyy-MM-dd",
            "yyyy-MM",
            "yyyy"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                return sdf.parse(releaseDate)!!
            } catch (e: ParseException) {
                // Try the next format
            }
        }

        throw IllegalArgumentException("Unrecognized date format: $releaseDate")
    }
}