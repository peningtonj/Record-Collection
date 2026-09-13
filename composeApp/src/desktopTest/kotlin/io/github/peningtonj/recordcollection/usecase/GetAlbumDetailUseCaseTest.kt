package io.github.peningtonj.recordcollection.usecase

import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.repository.UserLibraryRepository
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import app.cash.turbine.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A fresh "add to library" never writes an `albums/{id}` catalog doc (see
 * AlbumRepository.addAlbumToLibrary), so execute() falls into the API-fetch branch for
 * most of a user's own library. That branch used to be a one-shot lookup: the rating and
 * add/remove-from-library buttons on this screen correctly updated Firestore, but the
 * screen itself never noticed until it was closed and re-opened. It must now be a live
 * stream, matched primarily by the internal id — it collapses remasters/deluxe/regional
 * variants onto one identity by design (TECH_DEBT 2.2) — falling back to spotifyId only
 * for a library entry that hasn't been re-keyed onto the current id scheme yet.
 */
class GetAlbumDetailUseCaseTest {

    private val albumRepository = mockk<AlbumRepository>()
    private val albumTagRepository = mockk<AlbumTagRepository>()
    private val collectionAlbumRepository = mockk<CollectionAlbumRepository>()
    private val trackRepository = mockk<TrackRepository>()
    private val userLibraryRepository = mockk<UserLibraryRepository>()

    private val useCase = GetAlbumDetailUseCase(
        albumRepository, albumTagRepository, collectionAlbumRepository, trackRepository, userLibraryRepository,
    )

    private val album = TestAlbumDataFactory.album(id = "lib1", name = "In Rainbows", spotifyId = "spotify1", inLibrary = false)

    @Test
    fun `a rating or library change is reflected live, without re-opening the screen`() = runTest {
        coEvery { albumRepository.albumExists("lib1") } returns false
        coEvery { albumRepository.fetchAlbum("spotify1") } returns Result.success(album)
        coEvery { trackRepository.fetchTracksForAlbum(album) } returns emptyList()
        every { userLibraryRepository.getLibraryEntryBySpotifyId("spotify1") } returns flowOf(null)

        val entry = MutableStateFlow<UserLibraryRepository.LibraryAlbumDocument?>(null)
        every { userLibraryRepository.getLibraryEntry("lib1") } returns entry

        useCase.execute(albumId = "lib1", spotifyId = "spotify1").test {
            assertTrue(!awaitItem().album.inLibrary, "starts out of the library")

            // Simulate the add-to-library button's write landing on the live listener —
            // no re-subscription, no re-opening the screen.
            entry.value = UserLibraryRepository.LibraryAlbumDocument(albumId = "lib1", spotifyId = "spotify1", inLibrary = true, rating = null)
            assertTrue(awaitItem().album.inLibrary, "in-library flip must be picked up live")

            // Simulate the rating button.
            entry.value = entry.value!!.copy(rating = 8)
            assertEquals(8, awaitItem().rating)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an album not yet in the shared catalog shows as in-library when the migrated entry matches by id`() = runTest {
        coEvery { albumRepository.albumExists("lib1") } returns false
        coEvery { albumRepository.fetchAlbum("spotify1") } returns Result.success(album)
        coEvery { trackRepository.fetchTracksForAlbum(album) } returns emptyList()
        every { userLibraryRepository.getLibraryEntry("lib1") } returns flowOf(
            UserLibraryRepository.LibraryAlbumDocument(albumId = "lib1", spotifyId = "spotify1", inLibrary = true, rating = 5),
        )
        every { userLibraryRepository.getLibraryEntryBySpotifyId("spotify1") } returns flowOf(null)

        val state = useCase.execute(albumId = "lib1", spotifyId = "spotify1").first()

        assertTrue(state.album.inLibrary, "album should be reported as in-library")
        assertEquals(5, state.rating)
    }

    @Test
    fun `falls back to a spotifyId match when the library entry hasn't been re-keyed`() = runTest {
        // Deliberately keyed by a legacy internal id ("old7chr") that does NOT match
        // TestAlbumDataFactory's freshly-computed id ("lib1") — only spotifyId ties them
        // together, exactly like an un-migrated (or edition-variant) library entry.
        coEvery { albumRepository.albumExists("lib1") } returns false
        coEvery { albumRepository.fetchAlbum("spotify1") } returns Result.success(album)
        coEvery { trackRepository.fetchTracksForAlbum(album) } returns emptyList()
        every { userLibraryRepository.getLibraryEntry("lib1") } returns flowOf(null)
        every { userLibraryRepository.getLibraryEntryBySpotifyId("spotify1") } returns flowOf(
            UserLibraryRepository.LibraryAlbumDocument(albumId = "old7chr", spotifyId = "spotify1", inLibrary = true, rating = 5),
        )

        val state = useCase.execute(albumId = "lib1", spotifyId = "spotify1").first()

        assertTrue(state.album.inLibrary, "album should be reported as in-library")
        assertEquals(5, state.rating)
    }

    @Test
    fun `an album not in the catalog and not in the library shows as not in-library`() = runTest {
        coEvery { albumRepository.albumExists("lib1") } returns false
        coEvery { albumRepository.fetchAlbum("spotify1") } returns Result.success(album)
        coEvery { trackRepository.fetchTracksForAlbum(album) } returns emptyList()
        every { userLibraryRepository.getLibraryEntry("lib1") } returns flowOf(null)
        every { userLibraryRepository.getLibraryEntryBySpotifyId("spotify1") } returns flowOf(null)

        val state = useCase.execute(albumId = "lib1", spotifyId = "spotify1").first()

        assertTrue(!state.album.inLibrary)
        assertEquals(null, state.rating)
    }
}
