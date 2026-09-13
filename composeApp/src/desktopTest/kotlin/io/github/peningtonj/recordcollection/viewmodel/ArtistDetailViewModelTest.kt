package io.github.peningtonj.recordcollection.viewmodel

import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.UserLibraryRepository
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Library membership/rating for an artist's albums must come from the per-user
 * library_albums collection, not the shared albums catalogue — the catalogue's
 * inLibrary/rating fields are always unset (see UserLibraryRepository doc comment), so an
 * artist's own albums were showing as "not in library" even when the user has them.
 *
 * Matched primarily by the internal id (hash of name + primary artist), not spotifyId:
 * Spotify's artist-albums endpoint can return a different catalog entry — a remaster,
 * deluxe edition, or regional variant — for what's conceptually the same album than
 * whichever one the user actually saved, so a spotifyId-only match misses it. The
 * internal id collapses that by design. spotifyId is kept only as a fallback for a
 * library entry that hasn't been re-keyed onto the current id scheme yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val artistRepository = mockk<ArtistRepository>()
    private val userLibraryRepository = mockk<UserLibraryRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val albumInLibrary = TestAlbumDataFactory.album(id = "a1", spotifyId = "sp-a1", name = "Kid A", inLibrary = false)
    private val albumDifferentEdition = TestAlbumDataFactory.album(id = "a2", spotifyId = "sp-a2-remaster", name = "Amnesiac", inLibrary = false)
    private val albumNotInLibrary = TestAlbumDataFactory.album(id = "a3", spotifyId = "sp-a3", name = "Hail to the Thief", inLibrary = false)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an artist's albums reflect the user's actual library, matched by internal id`() {
        coEvery { artistRepository.fetchAlbumArtists("artist1") } returns
            listOf(albumInLibrary, albumDifferentEdition, albumNotInLibrary)
        every { artistRepository.getArtistById("artist1") } returns flowOf(null)
        every { userLibraryRepository.getAllLibraryEntries() } returns flowOf(
            listOf(
                // Normal case: migrated entry keyed by the current internal id.
                UserLibraryRepository.LibraryAlbumDocument(albumId = "a1", spotifyId = "sp-a1", inLibrary = true, rating = 4),
                // The user saved this album under a different Spotify catalog entry
                // (e.g. the original release) than the one the artist endpoint returns
                // for "Amnesiac" here (e.g. a remaster) — same name+artist, different
                // spotifyId. Must still match by the edition-collapsing internal id.
                UserLibraryRepository.LibraryAlbumDocument(albumId = "a2", spotifyId = "sp-a2-original", inLibrary = true, rating = 5),
            ),
        )

        val viewModel = ArtistDetailViewModel(artistRepository, userLibraryRepository, "artist1")

        val albums = viewModel.uiState.value.albums.associateBy { it.album.id }
        assertTrue(albums.getValue("a1").album.inLibrary, "a1 is in the user's library")
        assertTrue(albums.getValue("a2").album.inLibrary, "a2 (different edition) is still recognised as in-library")
        assertEquals(5, albums.getValue("a2").rating)
        assertFalse(albums.getValue("a3").album.inLibrary, "a3 is not in the user's library")
    }

    @Test
    fun `falls back to matching by spotifyId when the library entry hasn't been re-keyed`() {
        coEvery { artistRepository.fetchAlbumArtists("artist1") } returns listOf(albumInLibrary)
        every { artistRepository.getArtistById("artist1") } returns flowOf(null)
        // Keyed by a legacy internal id ("old7chr") unrelated to "a1" — only spotify_id
        // ties it to albumInLibrary, matching an un-migrated library entry.
        every { userLibraryRepository.getAllLibraryEntries() } returns flowOf(
            listOf(UserLibraryRepository.LibraryAlbumDocument(albumId = "old7chr", spotifyId = "sp-a1", inLibrary = true, rating = 4)),
        )

        val viewModel = ArtistDetailViewModel(artistRepository, userLibraryRepository, "artist1")

        val albums = viewModel.uiState.value.albums.associateBy { it.album.id }
        assertTrue(albums.getValue("a1").album.inLibrary, "a1 is in the user's library")
        assertEquals(4, albums.getValue("a1").rating)
    }
}
