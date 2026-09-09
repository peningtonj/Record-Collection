package io.github.peningtonj.recordcollection.db

import dev.gitlive.firebase.firestore.FieldValue
import io.github.peningtonj.recordcollection.db.domain.CollectionDocument
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.db.mapper.ArtistMapper
import io.github.peningtonj.recordcollection.db.mapper.TrackMapper
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the "every shared-code Firestore write is a plain-primitive map" invariant that
 * keeps the Kotlin/JS build able to write (see [isFirestoreRawValueSafe] docs).
 */
class FirestoreMapTest {

    @Test
    fun `primitive values are safe`() {
        listOf(null, "s", 1, 2.5, true, FieldValue.serverTimestamp).forEach {
            assertTrue(isFirestoreRawValueSafe(it), "$it should be safe")
        }
        assertTrue(isFirestoreRawValueSafe(listOf(1, "a", mapOf("k" to 2.0))))
        assertTrue(isFirestoreRawMapSafe(mapOf("n" to "x", "c" to 3, "t" to 1.0, "flag" to false, "opt" to null)))
    }

    @Test
    fun `a Kotlin Long is not safe, anywhere`() {
        assertFalse(isFirestoreRawValueSafe(1L))
        assertFalse(isFirestoreRawValueSafe(listOf(1L)))
        assertFalse(isFirestoreRawValueSafe(mapOf("x" to 2L)))
        assertFalse(isFirestoreRawValueSafe(listOf(mapOf("added_at" to 1_700_000_000L))))
        assertFalse(isFirestoreRawValueSafe(3.0f)) // Float has the same JS-boxing problem
    }

    @Test
    fun `a serializable model is not safe`() {
        assertFalse(isFirestoreRawValueSafe(CollectionDocument(name = "x")))
    }

    @Test
    fun `toLibraryProjection is a safe write map`() {
        val p = AlbumMapper.toLibraryProjection(TestAlbumDataFactory.album())
        assertTrue(isFirestoreRawMapSafe(p), "offending: ${p.filterValues { it is Long || it is Float }}")
    }

    @Test
    fun `collectionEntryToFirestoreMap is a safe write map and keeps the entry's data`() {
        val album = TestAlbumDataFactory.album(id = "e1", name = "Amnesiac")
        val entry = AlbumMapper.toCollectionEntry(album, position = 4, addedAtEpochSeconds = 1_700_000_000L)
        val m = AlbumMapper.collectionEntryToFirestoreMap(entry)

        assertTrue(isFirestoreRawMapSafe(m), "offending: ${m.filterValues { it is Long || it is Float }}")
        assertEquals("e1", m["album_id"])
        assertEquals(4, m["position"])
        assertEquals(1_700_000_000.0, m["added_at"])
        assertEquals(album.totalTracks, m["total_tracks"])
        assertEquals("Amnesiac", m["name"])
    }

    @Test
    fun `album, artist and track document maps are safe writes`() {
        assertTrue(isFirestoreRawMapSafe(AlbumMapper.toDocumentMap(TestAlbumDataFactory.album())))

        assertTrue(
            isFirestoreRawMapSafe(
                ArtistMapper.toDocumentMap(
                    id = "a", followers = 5_000_000, genres = listOf("rock"), href = "h",
                    imagesJson = "[]", name = "Radiohead", popularity = 82, type = "artist", uri = "u",
                ),
            ),
        )

        val track = Track(
            id = "t", name = "Idioteque", artists = listOf(SimplifiedArtist("a", "Radiohead", "u", emptyMap(), "h", "artist")),
            albumId = "kid-a", isExplicit = false, trackNumber = 8L, discNumber = 1L, durationMs = 189_000L,
            spotifyUri = "spotify:track:t",
        )
        assertTrue(isFirestoreRawMapSafe(TrackMapper.toDocumentMap(track)))
    }

    @Test
    fun `an empty album list maps to an empty list, not a list with a null`() {
        val albums = emptyList<io.github.peningtonj.recordcollection.db.domain.CollectionAlbumEntry>()
        val written = albums.map(AlbumMapper::collectionEntryToFirestoreMap)
        assertEquals(emptyList(), written)
    }
}
