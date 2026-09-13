package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.domain.CollectionAlbumEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A collection entry embeds a *copy* of the album's id at add-time — if that copy ever
 * goes stale relative to library_albums (an id migration that missed it, an entry added
 * mid-migration), rating/in-library must still resolve via the stable spotifyId.
 * Confirmed in prod: a "Spring 26" entry stuck on a pre-migration id reported as
 * not-in-library despite being rated and saved.
 */
class LibraryEntryLookupTest {

    private fun entry(albumId: String, spotifyId: String) = CollectionAlbumEntry(
        albumId = albumId, spotifyId = spotifyId, name = "N", primaryArtist = "A",
    )

    private fun libraryEntry(albumId: String, spotifyId: String, inLibrary: Boolean, rating: Int?) =
        UserLibraryRepository.LibraryAlbumDocument(albumId = albumId, spotifyId = spotifyId, inLibrary = inLibrary, rating = rating)

    @Test
    fun `resolves by the current internal id when the entry is not stale`() {
        val lookup = LibraryEntryLookup(listOf(libraryEntry("id1", "sp1", inLibrary = true, rating = 8)))

        val result = lookup.forEntry(entry(albumId = "id1", spotifyId = "sp1"))

        assertEquals(true, result?.inLibrary)
        assertEquals(8, result?.rating)
    }

    @Test
    fun `falls back to spotifyId when the entry's own id is stale`() {
        // The exact "Spring 26" shape: the entry carries a pre-migration id ("09u6g90")
        // that no longer exists anywhere; only spotifyId ties it to the real entry.
        val lookup = LibraryEntryLookup(listOf(libraryEntry("4d15f9a98db99e557d76c75f", "sp-rodrigo", inLibrary = true, rating = 7)))

        val result = lookup.forEntry(entry(albumId = "09u6g90", spotifyId = "sp-rodrigo"))

        assertEquals(true, result?.inLibrary)
        assertEquals(7, result?.rating)
    }

    @Test
    fun `neither id matching returns no entry, not a false positive`() {
        val lookup = LibraryEntryLookup(listOf(libraryEntry("other-id", "other-spotify", inLibrary = true, rating = 9)))

        val result = lookup.forEntry(entry(albumId = "id1", spotifyId = "sp1"))

        assertNull(result)
    }
}
