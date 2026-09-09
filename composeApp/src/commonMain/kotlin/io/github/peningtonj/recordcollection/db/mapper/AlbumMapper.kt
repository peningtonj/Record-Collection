package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumDocument
import io.github.peningtonj.recordcollection.db.domain.AlbumType
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbumEntry
import io.github.peningtonj.recordcollection.db.domain.Image
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedAlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import io.github.aakira.napier.Napier
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

    /**
     * The stable-field projection stored on `users/{uid}/library_albums/{id}` so the
     * library renders without joining the shared `albums` catalogue. See docs/DATA_MODEL.md.
     * Returns a Firestore-writable map (merged into the doc); user data is written separately.
     */
    fun toLibraryProjection(album: Album): Map<String, Any?> = mapOf(
        "name" to album.name,
        "primary_artist" to album.primaryArtist,
        "artists" to Json.encodeToString(album.artists),
        "release_date" to album.releaseDate.toString(),
        "album_type" to album.albumType.name,
        // Int / Double, never a Kotlin Long — GitLive's JS SDK rejects a boxed Long.
        // Firestore stores int64/double regardless, so the `Long` model fields read fine.
        "total_tracks" to album.totalTracks,
        "spotify_id" to album.spotifyId,
        "spotify_uri" to album.spotifyUri,
        "image_url" to album.images.firstOrNull()?.url,
        "projection_fetched_at" to kotlinx.datetime.Clock.System.now().toEpochMilliseconds().toDouble(),
    )

    /**
     * A `collections/{name}.albums[]` entry as a plain-primitive map. Built by hand
     * (not `set(CollectionAlbumEntry)`) so it's safe to write from the Kotlin/JS build —
     * see [io.github.peningtonj.recordcollection.db.isFirestoreRawMapSafe].
     */
    fun collectionEntryToFirestoreMap(entry: CollectionAlbumEntry): Map<String, Any?> = mapOf(
        "album_id" to entry.albumId,
        "position" to entry.position,
        "added_at" to entry.addedAt.toDouble(),
        "name" to entry.name,
        "primary_artist" to entry.primaryArtist,
        "artists" to entry.artists,
        "release_date" to entry.releaseDate,
        "album_type" to entry.albumType,
        "total_tracks" to entry.totalTracks.toInt(),
        "spotify_id" to entry.spotifyId,
        "spotify_uri" to entry.spotifyUri,
        "image_url" to entry.imageUrl,
    )

    /** A `CollectionAlbumEntry` carrying the stable-field projection (see [toLibraryProjection]). */
    fun toCollectionEntry(
        album: Album,
        position: Int,
        addedAtEpochSeconds: Long,
    ): CollectionAlbumEntry =
        CollectionAlbumEntry(
            albumId = album.id,
            position = position,
            addedAt = addedAtEpochSeconds,
            name = album.name,
            primaryArtist = album.primaryArtist,
            artists = Json.encodeToString(album.artists),
            releaseDate = album.releaseDate.toString(),
            albumType = album.albumType.name,
            totalTracks = album.totalTracks.toLong(),
            spotifyId = album.spotifyId,
            spotifyUri = album.spotifyUri,
            imageUrl = album.images.firstOrNull()?.url,
        )

    fun collectionEntryToDomain(
        entry: CollectionAlbumEntry,
    ): Album = libraryProjectionToDomain(
        albumId = entry.albumId, name = entry.name, primaryArtist = entry.primaryArtist,
        artistsJson = entry.artists, releaseDate = entry.releaseDate, albumType = entry.albumType,
        totalTracks = entry.totalTracks, spotifyId = entry.spotifyId, spotifyUri = entry.spotifyUri,
        imageUrl = entry.imageUrl, rating = null, addedAt = null,
    ).copy(inLibrary = false)

    /** Rebuilds an [Album] from a library projection. Volatile fields (genres, updatedAt, …) are left empty. */
    fun libraryProjectionToDomain(
        albumId: String,
        name: String,
        primaryArtist: String,
        artistsJson: String,
        releaseDate: String,
        albumType: String,
        totalTracks: Long,
        spotifyId: String,
        spotifyUri: String,
        imageUrl: String?,
        rating: Int?,
        addedAt: String?,
    ): Album = Album(
        id = albumId,
        spotifyId = spotifyId,
        name = name,
        primaryArtist = primaryArtist,
        artists = runCatching { Json.decodeFromString<List<SimplifiedArtist>>(artistsJson) }.getOrElse { emptyList() },
        releaseDate = parseReleaseDate(releaseDate),
        totalTracks = totalTracks.toInt(),
        spotifyUri = spotifyUri,
        addedAt = addedAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
        albumType = runCatching { AlbumType.fromString(albumType) }.getOrDefault(AlbumType.ALBUM),
        images = imageUrl?.let { listOf(Image(url = it, height = null, width = null)) } ?: emptyList(),
        inLibrary = true,
        rating = rating,
    )

    /**
     * `albums/{id}` as a plain-primitive map (see [collectionEntryToFirestoreMap]).
     * Callers override `added_at` / `updated_at` as needed (map `+` wins on the right).
     */
    fun toDocumentMap(album: Album): Map<String, Any?> = mapOf(
        "id" to album.id,
        "spotify_id" to album.spotifyId,
        "name" to album.name,
        "primary_artist" to album.primaryArtist,
        "artists" to Json.encodeToString(album.artists),
        "release_date" to album.releaseDate.toString(),
        "total_tracks" to album.totalTracks,
        "spotify_uri" to album.spotifyUri,
        "added_at" to album.addedAt?.toString(),
        "album_type" to album.albumType.name,
        "images" to Json.encodeToString(album.images),
        "updated_at" to album.updatedAt?.toDouble(),
        "external_ids" to album.externalIds?.let { Json.encodeToString(it) },
        "release_group_id" to album.releaseGroupId,
    )

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

    /** Fallback for a Spotify `release_date` we can't parse — see [parseReleaseDate]. */
    val UNKNOWN_RELEASE_DATE: LocalDate = LocalDate(1900, 1, 1)

    /**
     * Parses a Spotify `release_date` (`2024`, `2024-12`, or `2024-12-31`).
     * A malformed value must not abort a whole sync, so anything unparseable
     * falls back to [UNKNOWN_RELEASE_DATE] with a warning.
     */
    fun parseReleaseDate(releaseDate: String): LocalDate {
        return runCatching {
            when (releaseDate.count { it == '-' }) {
                1 -> {
                    // Year-Month: 2024-12
                    val parts = releaseDate.split('-')
                    LocalDate(parts[0].toInt(), parts[1].toInt(), 1) // Default to 1st of month
                }

                0 -> LocalDate(releaseDate.toInt(), 1, 1) // Year only: default to January 1st

                else -> LocalDate.parse(releaseDate) // ISO format
            }
        }.getOrElse {
            Napier.w("Unparseable release_date '$releaseDate' — using $UNKNOWN_RELEASE_DATE")
            UNKNOWN_RELEASE_DATE
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

