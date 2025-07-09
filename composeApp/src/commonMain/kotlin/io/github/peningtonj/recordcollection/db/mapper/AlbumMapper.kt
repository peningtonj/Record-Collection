package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Albums
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumType
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json


object AlbumMapper {

    fun toDomain(entity: Albums): Album {
        return Album(
            id = entity.id,
            name = entity.name,
            primaryArtist = entity.primary_artist,
            artists = Json.decodeFromString<List<SimplifiedArtistDto>>(entity.artists)
                .map { ArtistMapper.toDomain(it) },
            releaseDate = parseReleaseDate(entity.release_date.toString()),
            totalTracks = entity.total_tracks.toInt(),
            spotifyUri = entity.spotify_uri,
            addedAt = Instant.parse(entity.added_at),
            albumType = AlbumType.fromString(entity.album_type),
            images = Json.decodeFromString<List<ImageDto>>(entity.images)
                .map { ImageMapper.toDomain(it) },
            updatedAt = entity.updated_at
        )
    }

    fun toDomain(entity: AlbumDto): Album {
        return Album(
            id = entity.id,
            name = entity.name,
            primaryArtist = entity.artists.firstOrNull()?.name ?: "Unknown Artist",
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            releaseDate = parseReleaseDate(entity.releaseDate),
            totalTracks = entity.totalTracks,
            spotifyUri = entity.uri,
            albumType = AlbumType.fromString(entity.albumType.name),
            images = entity.images
                .map { ImageMapper.toDomain(it) },
        )
    }

    fun parseReleaseDate(releaseDate: String): LocalDate {
        return when (releaseDate.count { it == '-' }) {
            1 -> {
                // Year-Month: 2024-12
                val parts = releaseDate.split('-')
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                LocalDate(year, month, 1) // Default to 1st of month
            }

            0 -> {
                // Year only: 2024
                val year = releaseDate.toInt()
                LocalDate(year, 1, 1) // Default to January 1st
            }

            else -> {
                LocalDate.parse(releaseDate) // Uses ISO format
            }
        }
    }
}