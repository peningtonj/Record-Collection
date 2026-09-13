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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    private val artistRepository = mockk<ArtistRepository>()
    private val userLibraryRepository = mockk<UserLibraryRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val albumInLibrary = TestAlbumDataFactory.album(id = "a1", name = "Kid A", inLibrary = false)
    private val albumNotInLibrary = TestAlbumDataFactory.album(id = "a2", name = "Amnesiac", inLibrary = false)

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
        every { userLibraryRepository.getAllLibraryEntries() } returns flowOf(
            listOf(UserLibraryRepository.LibraryAlbumDocument(albumId = "a1", inLibrary = true, rating = 4)),
        )

        val viewModel = ArtistDetailViewModel(artistRepository, userLibraryRepository, "artist1")

        val albums = viewModel.uiState.value.albums.associateBy { it.album.id }
        assertTrue(albums.getValue("a1").album.inLibrary, "a1 is in the user's library")
        assertFalse(albums.getValue("a2").album.inLibrary, "a2 is not in the user's library")
        assertEquals(4, albums.getValue("a1").rating)
    }
}
