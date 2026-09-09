package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.UserApi
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.util.AggregateException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProfileRepositoryTest {

    private val userApi = mockk<UserApi>()
    private val spotifyApi = mockk<SpotifyApi> { every { user } returns userApi }
    private val repository = ProfileRepository(spotifyApi)

    private fun albums(count: Int) = (1..count).map {
        TestAlbumDataFactory.album(id = "id-$it", name = "Album $it").copy(spotifyId = "sp-$it")
    }

    @Test
    fun `addAlbumsToSpotifyLibrary batches by 20 and succeeds`() = runTest {
        coEvery { userApi.saveAlbumsToCurrentUsersLibrary(any()) } returns Unit

        val result = repository.addAlbumsToSpotifyLibrary(albums(25))

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { userApi.saveAlbumsToCurrentUsersLibrary(any()) }
    }

    @Test
    fun `addAlbumsToSpotifyLibrary skips albums with no spotifyId`() = runTest {
        coEvery { userApi.saveAlbumsToCurrentUsersLibrary(any()) } returns Unit
        val withBlank = albums(1) + TestAlbumDataFactory.album(id = "x").copy(spotifyId = "")

        repository.addAlbumsToSpotifyLibrary(withBlank)

        coVerify(exactly = 1) { userApi.saveAlbumsToCurrentUsersLibrary(listOf("sp-1")) }
    }

    @Test
    fun `addAlbumsToSpotifyLibrary reports a partial failure instead of swallowing it`() = runTest {
        // First chunk succeeds, second chunk fails.
        coEvery { userApi.saveAlbumsToCurrentUsersLibrary(match { it.contains("sp-1") }) } returns Unit
        coEvery { userApi.saveAlbumsToCurrentUsersLibrary(match { it.contains("sp-21") }) } throws
            RuntimeException("429 Too Many Requests")

        val result = repository.addAlbumsToSpotifyLibrary(albums(25))

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertIs<AggregateException>(error)
        assertEquals(1, error.causes.size)
    }
}
