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
 * — looked up by Spotify ID, since a library_albums doc predating the hash-ID migration
 * (TECH_DEBT 2.2) won't be keyed by the internal id a fresh API fetch computes.
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

    // The library doc is deliberately keyed by a legacy internal id ("old7chr") that does
    // NOT match TestAlbumDataFactory's freshly-computed id ("lib1") — only spotifyId ties
    // them together, exactly like the un-migrated production data that surfaced this bug.
    private val album = TestAlbumDataFactory.album(id = "lib1", name = "In Rainbows", spotifyId = "spotify1", inLibrary = false)

    @Test
    fun `an album not yet in the shared catalog still shows as in-library when it's in the user's library`() = runTest {
        coEvery { albumRepository.albumExists("lib1") } returns false
        coEvery { albumRepository.fetchAlbum("spotify1") } returns Result.success(album)
        coEvery { trackRepository.fetchTracksForAlbum(album) } returns emptyList()
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
        every { userLibraryRepository.getLibraryEntryBySpotifyId("spotify1") } returns flowOf(null)

        val state = useCase.execute(albumId = "lib1", spotifyId = "spotify1").first()

        assertTrue(!state.album.inLibrary)
        assertEquals(null, state.rating)
    }
}
