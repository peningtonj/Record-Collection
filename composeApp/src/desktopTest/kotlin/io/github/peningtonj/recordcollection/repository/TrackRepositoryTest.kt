package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.network.spotify.LibraryApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.UserApi
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.testDataFactory.TestTrackDataFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackRepositoryTest {

    private val libraryApi = mockk<LibraryApi>()
    private val userApi = mockk<UserApi>(relaxed = true)
    private val spotifyApi = mockk<SpotifyApi> { every { library } returns libraryApi; every { user } returns userApi }
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)

    private val album = TestAlbumDataFactory.album(id = "alb1").copy(spotifyId = "sp1")
    private val t1 = TestTrackDataFactory.track(trackNumber = 1, album = album)
    private val t2 = TestTrackDataFactory.track(trackNumber = 2, album = album)

    private fun repo() = spyk(TrackRepository(firestore, spotifyApi)).also {
        coEvery { it.fetchTracksForAlbum(any()) } returns listOf(t1, t2)
    }

    @Test
    fun `getAlbumTracks marks isSaved from the Spotify contains check`() = runTest {
        val repo = repo()
        coEvery { libraryApi.checkSavedTracks(listOf(t1.id, t2.id)) } returns Result.success(listOf(true, false))

        val tracks = repo.getAlbumTracks(album)

        assertEquals(true, tracks.single { it.id == t1.id }.isSaved)
        assertEquals(false, tracks.single { it.id == t2.id }.isSaved)
    }

    @Test
    fun `a failed contains check leaves tracks unsaved rather than throwing`() = runTest {
        val repo = repo()
        coEvery { libraryApi.checkSavedTracks(any()) } returns Result.failure(RuntimeException("boom"))

        val tracks = repo.getAlbumTracks(album)

        assertEquals(listOf(false, false), tracks.map { it.isSaved })
    }

    @Test
    fun `saveTracksLocalAndRemote patches the cache so an open tracklist shows the hearts filled`() = runTest {
        // "Add All to Saved Songs": the write to Spotify/Firestore succeeded but the
        // tracklist screen's cached copy never learned about it, so the hearts stayed
        // empty until the cache's TTL expired and re-checked Spotify.
        val repo = repo()
        coEvery { libraryApi.checkSavedTracks(any()) } returns Result.success(listOf(false, false))

        repo.getAlbumTracks(album) // populate cache, unsaved
        repo.saveTracksLocalAndRemote(listOf(t1.id, t2.id))

        val tracks = repo.getAlbumTracks(album) // cache hit — no re-fetch
        assertEquals(listOf(true, true), tracks.map { it.isSaved })
    }

    @Test
    fun `setTrackSaved override wins over the contains check and patches the cache`() = runTest {
        val repo = repo()
        coEvery { libraryApi.checkSavedTracks(any()) } returns Result.success(listOf(false, false))

        repo.getAlbumTracks(album) // populate cache
        repo.setTrackSaved(t1.id, true)

        // cache hit — no re-fetch — must reflect the override
        val tracks = repo.getAlbumTracks(album)
        assertEquals(true, tracks.single { it.id == t1.id }.isSaved)
        assertEquals(false, tracks.single { it.id == t2.id }.isSaved)
    }
}
