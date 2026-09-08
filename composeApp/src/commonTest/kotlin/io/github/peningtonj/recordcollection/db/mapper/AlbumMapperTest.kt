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
