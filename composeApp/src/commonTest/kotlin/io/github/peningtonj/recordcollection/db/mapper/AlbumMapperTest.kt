package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AlbumMapperTest {

    // --- parseReleaseDate (TECH_DEBT 4.3) ---

    @Test
    fun `parseReleaseDate handles full ISO date`() {
        assertEquals(LocalDate(2024, 12, 31), AlbumMapper.parseReleaseDate("2024-12-31"))
    }

    @Test
    fun `parseReleaseDate handles year-month`() {
        assertEquals(LocalDate(2024, 12, 1), AlbumMapper.parseReleaseDate("2024-12"))
    }

    @Test
    fun `parseReleaseDate handles year only`() {
        assertEquals(LocalDate(2024, 1, 1), AlbumMapper.parseReleaseDate("2024"))
    }

    @Test
    fun `parseReleaseDate falls back instead of throwing on garbage`() {
        assertEquals(AlbumMapper.UNKNOWN_RELEASE_DATE, AlbumMapper.parseReleaseDate("not-a-date"))
        assertEquals(AlbumMapper.UNKNOWN_RELEASE_DATE, AlbumMapper.parseReleaseDate(""))
        assertEquals(AlbumMapper.UNKNOWN_RELEASE_DATE, AlbumMapper.parseReleaseDate("2024-13-40"))
    }

    @Test
    fun `toDomain does not throw on a malformed release_date`() {
        val dto = TestAlbumDataFactory.albumDto(releaseDate = "garbage")
        assertEquals(AlbumMapper.UNKNOWN_RELEASE_DATE, AlbumMapper.toDomain(dto).releaseDate)
    }

    // --- toDocument / toDomain round trip ---

    @Test
    fun `library projection round-trips the stable fields`() {
        val original = TestAlbumDataFactory.album(id = "lib1", name = "In Rainbows")
        val p = AlbumMapper.toLibraryProjection(original)

        val back = AlbumMapper.libraryProjectionToDomain(
            albumId = "lib1",
            name = p["name"] as String,
            primaryArtist = p["primary_artist"] as String,
            artistsJson = p["artists"] as String,
            releaseDate = p["release_date"] as String,
            albumType = p["album_type"] as String,
            totalTracks = p["total_tracks"] as Long,
            spotifyId = p["spotify_id"] as String,
            spotifyUri = p["spotify_uri"] as String,
            imageUrl = p["image_url"] as String?,
            rating = 4,
            addedAt = null,
        )

        assertEquals(original.id, back.id)
        assertEquals(original.name, back.name)
        assertEquals(original.primaryArtist, back.primaryArtist)
        assertEquals(original.artists, back.artists)
        assertEquals(original.releaseDate, back.releaseDate)
        assertEquals(original.totalTracks, back.totalTracks)
        assertEquals(original.albumType, back.albumType)
        assertEquals(original.spotifyId, back.spotifyId)
        assertEquals(original.images.first().url, back.images.first().url)
        assertEquals(4, back.rating)
        assertEquals(true, back.inLibrary)
    }

    @Test
    fun `libraryProjectionToDomain tolerates a blank album_type`() {
        val album = AlbumMapper.libraryProjectionToDomain(
            albumId = "x", name = "N", primaryArtist = "A", artistsJson = "[]",
            releaseDate = "2020", albumType = "", totalTracks = 0, spotifyId = "", spotifyUri = "",
            imageUrl = null, rating = null, addedAt = null,
        )
        assertEquals(io.github.peningtonj.recordcollection.db.domain.AlbumType.ALBUM, album.albumType)
    }

    @Test
    fun `toDocument then toDomain preserves core metadata`() {
        val original = TestAlbumDataFactory.album(id = "abc123", name = "Kid A")
        val roundTripped = AlbumMapper.toDomain(AlbumMapper.toDocument(original).copy(id = original.id))

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.name, roundTripped.name)
        assertEquals(original.primaryArtist, roundTripped.primaryArtist)
        assertEquals(original.spotifyId, roundTripped.spotifyId)
        assertEquals(original.releaseDate, roundTripped.releaseDate)
        assertEquals(original.totalTracks, roundTripped.totalTracks)
        assertEquals(original.albumType, roundTripped.albumType)
        assertEquals(original.artists, roundTripped.artists)
        assertEquals(original.externalIds, roundTripped.externalIds)
    }
}
