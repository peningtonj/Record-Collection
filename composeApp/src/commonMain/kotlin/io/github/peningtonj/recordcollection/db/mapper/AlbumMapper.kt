package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumDocument
import io.github.peningtonj.recordcollection.db.domain.AlbumType
import io.github.peningtonj.recordcollection.db.domain.Image
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedAlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import io.github.peningtonj.recordcollection.util.sha256Hex
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json


object AlbumMapper {

    /**
     * Generate a stable ID from album name and artist.
     * This ensures that albums with the same name and artist always get the same ID,
     * even if Spotify changes their internal IDs.
     */


    fun toDomain(entity: AlbumDto): Album {
        val primaryArtist = entity.artists.firstOrNull()?.name ?: "Unknown Artist"
        return Album(
            id = generateAlbumId(entity.name, primaryArtist),
            spotifyId = entity.id,
            name = entity.name,
            primaryArtist = primaryArtist,
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            releaseDate = parseReleaseDate(entity.releaseDate),
            totalTracks = entity.totalTracks,
            spotifyUri = entity.uri,
            albumType = AlbumType.fromString(entity.albumType.name),
            images = entity.images?.map { ImageMapper.toDomain(it) } ?: emptyList(),
            externalIds = entity.externalIds
        )
    }
    
    fun toDomain(entity: SimplifiedAlbumDto): Album {
        val primaryArtist = entity.artists.firstOrNull()?.name ?: "Unknown Artist"
        return Album(
            id = generateAlbumId(entity.name, primaryArtist),
            spotifyId = entity.id,
            name = entity.name,
            primaryArtist = primaryArtist,
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            releaseDate = parseReleaseDate(entity.releaseDate),
            totalTracks = entity.totalTracks,
            spotifyUri = entity.uri,
            images = entity.images?.map { ImageMapper.toDomain(it) } ?: emptyList(),
            albumType = AlbumType.fromString(entity.albumType.name),

        )
    }


    fun toDomain(entity: AlbumDocument): Album {
        return Album(
            id = entity.id,
            spotifyId = entity.spotifyId,
            name = entity.name,
            primaryArtist = entity.primaryArtist,
            artists = runCatching { Json.decodeFromString<List<SimplifiedArtist>>(entity.artists) }.getOrElse { emptyList() },
            releaseDate = parseReleaseDate(entity.releaseDate),
            totalTracks = entity.totalTracks.toInt(),
            spotifyUri = entity.spotifyUri,
            addedAt = entity.addedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
            albumType = AlbumType.fromString(entity.albumType),
            images = runCatching { Json.decodeFromString<List<Image>>(entity.images) }.getOrElse { emptyList() },
            updatedAt = entity.updatedAt,
            externalIds = entity.externalIds?.let { runCatching { Json.decodeFromString<Map<String, String>>(it) }.getOrNull() },
            // inLibrary and rating are now sourced from users/{userId}/library_albums
            inLibrary = false,
            releaseGroupId = entity.releaseGroupId,
            rating = null
        )
    }

    /** Writes album metadata only — inLibrary and rating are stored in the user library sub-collection. */
    fun toDocument(album: Album): AlbumDocument {
        return AlbumDocument(
            id = album.id,
            spotifyId = album.spotifyId,
            name = album.name,
            primaryArtist = album.primaryArtist,
            artists = Json.encodeToString(album.artists),
            releaseDate = album.releaseDate.toString(),
            totalTracks = album.totalTracks.toLong(),
            spotifyUri = album.spotifyUri,
            addedAt = album.addedAt?.toString(),
            albumType = album.albumType.name,
            images = Json.encodeToString(album.images),
            updatedAt = album.updatedAt,
            externalIds = album.externalIds?.let { Json.encodeToString(it) },
            releaseGroupId = album.releaseGroupId
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

/**
 * Stable Firestore document ID for an album, derived from its normalized
 * name + primary artist (the album's identity — see docs/MIGRATION_SPOTIFY_ID.md).
 *
 * SHA-256 truncated to 96 bits (24 hex chars): collision-safe for any realistic
 * collection. Existing databases must be migrated with scripts/migrate_album_ids.py —
 * the old 32-bit String.hashCode() scheme produced different IDs.
 *
 * Keep this normalization byte-for-byte in sync with migrate_album_ids.py.
 */
fun generateAlbumId(name: String, artist: String?): String {
    val normalizedArtist = (artist ?: "Unknown Artist").trim().lowercase()
    val key = "${name.trim().lowercase()}|$normalizedArtist"
    return sha256Hex(key).take(24)
}

fun generateAlbumId(album: Album): String {
    return generateAlbumId(album.name, album.primaryArtist)
}

