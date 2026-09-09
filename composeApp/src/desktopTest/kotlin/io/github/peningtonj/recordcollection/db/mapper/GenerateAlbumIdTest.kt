package io.github.peningtonj.recordcollection.db.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class GenerateAlbumIdTest {

    @Test
    fun `is deterministic`() {
        assertEquals(
            generateAlbumId("Abbey Road", "The Beatles"),
            generateAlbumId("Abbey Road", "The Beatles"),
        )
    }

    @Test
    fun `normalizes case and surrounding whitespace`() {
        val canonical = generateAlbumId("Abbey Road", "The Beatles")
        assertEquals(canonical, generateAlbumId("  abbey road ", "THE BEATLES"))
    }

    @Test
    fun `matches the SHA-256 contract shared with migrate_album_ids py`() {
        // sha256("abbey road|the beatles").hexdigest()[:24] — cross-checked with openssl.
        assertEquals("abbf3c87d51164fd398e40db", generateAlbumId("Abbey Road", "The Beatles"))
    }

    @Test
    fun `id is 24 lowercase hex chars`() {
        val id = generateAlbumId("OK Computer", "Radiohead")
        assertEquals(24, id.length)
        assertTrue(id.all { it in "0123456789abcdef" }, "unexpected chars in '$id'")
    }

    @Test
    fun `null artist falls back to Unknown Artist`() {
        assertEquals(generateAlbumId("x", null), generateAlbumId("x", "Unknown Artist"))
    }

    @Test
    fun `different albums get different ids`() {
        val ids = listOf(
            generateAlbumId("Abbey Road", "The Beatles"),
            generateAlbumId("Let It Be", "The Beatles"),
            generateAlbumId("Abbey Road", "Booker T"),
            generateAlbumId("OK Computer", "Radiohead"),
        )
        assertEquals(ids.size, ids.toSet().size)
        assertNotEquals(ids[0], ids[2])
    }
}
