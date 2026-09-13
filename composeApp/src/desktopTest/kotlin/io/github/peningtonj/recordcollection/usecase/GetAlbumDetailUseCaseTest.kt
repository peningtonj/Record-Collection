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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A fresh "add to library" never writes an `albums/{id}` catalog doc (see
 * AlbumRepository.addAlbumToLibrary), so execute() falls into the API-fetch branch for
 * most of a user's own library. That branch must still report inLibrary/rating correctly
 * — looked up by the internal id first (it collapses remasters/deluxe/regional variants
 * onto one identity by design — TECH_DEBT 2.2), falling back to spotifyId only for a
 * library entry that hasn't been re-keyed onto the current id scheme yet.
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
    fun `an album not yet in the shared catalog shows as in-library when the migrated entry matches by id`() = runTest {
        coEvery { albumRepository.albumExists("lib1") } returns false
        coEvery { albumRepository.fetchAlbum("spotify1") } returns Result.success(album)
        coEvery { trackRepository.fetchTracksForAlbum(album) } returns emptyList()
        every { userLibraryRepository.getLibraryEntry("lib1") } returns flowOf(
            UserLibraryRepository.LibraryAlbumDocument(albumId = "lib1", spotifyId = "spotify1", inLibrary = true, rating = 5),
        )

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
