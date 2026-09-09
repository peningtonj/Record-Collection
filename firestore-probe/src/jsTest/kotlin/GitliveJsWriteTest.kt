import dev.gitlive.firebase.internal.decode
import dev.gitlive.firebase.internal.encodeAsObject
import dev.gitlive.firebase.internal.js
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins how GitLive's Kotlin/JS serializer treats the write shapes `:composeApp` sends to
 * Firestore. `encodeAsObject(x).js` is exactly the JS value handed to `setDoc()`.
 */
class GitliveJsWriteTest {

    private fun stringify(v: Any?): String = JSON.stringify(v)

    // Faithful copy of composeApp's model (the shape is what matters here).
    @Serializable
    private data class CollectionEntry(
        @SerialName("album_id") val albumId: String = "",
        val position: Int = 0,
        @SerialName("added_at") val addedAt: Long = 0L,
        @SerialName("total_tracks") val totalTracks: Long = 0L,
        val name: String = "",
    )

    @Serializable
    private data class CollectionDoc(
        val name: String,
        @SerialName("created_at") val createdAt: Long = 0L,
        val albums: List<CollectionEntry> = emptyList(),
    )

    // ---- why the app does NOT use set(model) ---------------------------------

    @Test
    fun `set(model) writes an empty list field as [null] - the prod bug`() {
        val out = stringify(encodeAsObject(CollectionDoc(name = "Empty")).js)
        // This is the documented failure: albums becomes [null], not [].
        assertTrue("[null]" in out, "expected the known bug signature; got $out")
    }

    // ---- what the app DOES use: raw primitive maps ---------------------------

    private fun entryMap(id: String, pos: Int) = mapOf(
        "album_id" to id, "position" to pos, "added_at" to 1_700_000_000.0,
        "total_tracks" to 11, "name" to "Kid A",
    )

    @Test
    fun `raw map with a list of primitive maps round-trips intact`() {
        val encoded = encodeAsObject(
            mapOf(
                "name" to "Faves",
                "created_at" to 1_700_000_000.0,
                "albums" to listOf(entryMap("a1", 1), entryMap("a2", 2)),
            ),
        ).js
        val out = stringify(encoded)
        assertTrue("\"a1\"" in out && "\"a2\"" in out, out)
        assertTrue("\"position\":1" in out && "\"position\":2" in out, out)
        assertFalse("null" in out, "no nulls expected: $out")
        assertFalse("low" in out && "high" in out, "no boxed Long expected: $out")
    }

    @Test
    fun `raw map with an empty list writes []`() {
        val out = stringify(encodeAsObject(mapOf("albums" to emptyList<Any?>())).js)
        assertEquals("""{"albums":[]}""", out)
    }

    @Test
    fun `raw map primitives pass straight through`() {
        val out = stringify(
            encodeAsObject(mapOf("total_tracks" to 10, "ratio" to 1.5, "flag" to true, "opt" to null)).js,
        )
        assertTrue("\"total_tracks\":10" in out, out)
        assertTrue("\"ratio\":1.5" in out, out)
    }

    @Test
    fun `a doc written as a primitive map reads back into the Long model`() {
        // added_at / total_tracks are Double on the wire but Long on the model — this is
        // the read-path claim the composeApp fix depends on.
        val wire = encodeAsObject(
            mapOf(
                "name" to "Faves",
                "created_at" to 1_700_000_000.0,
                "albums" to listOf(entryMap("a1", 1)),
            ),
        ).js
        val doc: CollectionDoc = decode(CollectionDoc.serializer(), wire)
        assertEquals("Faves", doc.name)
        assertEquals(1_700_000_000L, doc.createdAt)
        assertEquals(1, doc.albums.size)
        assertEquals("a1", doc.albums[0].albumId)
        assertEquals(1_700_000_000L, doc.albums[0].addedAt)
        assertEquals(11L, doc.albums[0].totalTracks)
    }

    @Test
    fun `an int64 on the wire also reads into the Long model (existing desktop data)`() {
        val doc: CollectionDoc = decode(
            CollectionDoc.serializer(),
            encodeAsObject(mapOf("name" to "X", "created_at" to 1_700_000_000, "albums" to emptyList<Any?>())).js,
        )
        assertEquals(1_700_000_000L, doc.createdAt)
    }

    @Test
    fun `a raw Long is the thing we must never send`() {
        // Either it throws, or it serialises to a boxed object — both break setDoc().
        val result = runCatching { stringify(encodeAsObject(mapOf("n" to 10L)).js) }
        val ok = result.isFailure || result.getOrNull()?.let { "\"n\":10" in it } == false
        assertTrue(ok, "expected Long to fail or box; got ${result.getOrNull()}")
    }
}
