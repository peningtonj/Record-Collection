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
 * The match must be by Spotify ID, not the internal hash id: library_albums docs
 * predating the hash-ID migration (TECH_DEBT 2.2) are still keyed by an older scheme, so
 * an artist's freshly-fetched albums (which compute the current hash id) never lined up
 * with them by id alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val artistRepository = mockk<ArtistRepository>()
    private val userLibraryRepository = mockk<UserLibraryRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val albumInLibrary = TestAlbumDataFactory.album(id = "a1", spotifyId = "sp-a1", name = "Kid A", inLibrary = false)
    private val albumNotInLibrary = TestAlbumDataFactory.album(id = "a2", spotifyId = "sp-a2", name = "Amnesiac", inLibrary = false)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `an artist's albums reflect the user's actual library, not the shared catalogue`() {
        coEvery { artistRepository.fetchAlbumArtists("artist1") } returns listOf(albumInLibrary, albumNotInLibrary)
        every { artistRepository.getArtistById("artist1") } returns flowOf(null)
        // Keyed by a legacy internal id ("old7chr") unrelated to "a1" — only spotify_id
        // ties it to albumInLibrary, matching real un-migrated production data.
        every { userLibraryRepository.getAllLibraryEntries() } returns flowOf(
            listOf(UserLibraryRepository.LibraryAlbumDocument(albumId = "old7chr", spotifyId = "sp-a1", inLibrary = true, rating = 4)),
        )

        val viewModel = ArtistDetailViewModel(artistRepository, userLibraryRepository, "artist1")

        val albums = viewModel.uiState.value.albums.associateBy { it.album.id }
        assertTrue(albums.getValue("a1").album.inLibrary, "a1 is in the user's library")
        assertFalse(albums.getValue("a2").album.inLibrary, "a2 is not in the user's library")
        assertEquals(4, albums.getValue("a1").rating)
    }
}
