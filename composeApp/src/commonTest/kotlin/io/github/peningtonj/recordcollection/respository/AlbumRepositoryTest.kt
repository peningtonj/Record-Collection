package io.github.peningtonj.recordcollection.respository

import app.cash.sqldelight.TransactionWithoutReturn
import io.github.peningtonj.recordcollection.db.AlbumsQueries
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.events.AlbumEvent
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.spotify.LibraryApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumsResponse
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AlbumRepositoryTest {

    @MockK
    private lateinit var database: RecordCollectionDatabase

    @MockK
    private lateinit var albumsQueries: AlbumsQueries

    @MockK
    private lateinit var spotifyApi: SpotifyApi

    @MockK
    private lateinit var miscApi: MiscApi

    @MockK
    private lateinit var eventDispatcher: AlbumEventDispatcher

    private lateinit var repository: AlbumRepository

    private val testAlbumDto = TestAlbumDataFactory.albumDto()
    private val testAlbum = TestAlbumDataFactory.album()

    @BeforeTest
    fun setup() {
        MockKAnnotations.init(this)

        every { database.albumsQueries } returns albumsQueries

        repository = AlbumRepository(
            database = database,
            spotifyApi = spotifyApi,
            miscApi = miscApi,
            eventDispatcher = eventDispatcher
        )
    }

    // DATABASE OPERATIONS TESTS

    @Test
    fun `saveAlbum with AlbumDto saves album to database with correct parameters`() {
        every { albumsQueries.insert(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        repository.saveAlbum(testAlbumDto, addToUsersLibrary = true)

        verify {
            albumsQueries.insert(
                id = "test-album-id",
                spotify_id = "test-album-id",
                name = "Test Album",
                primary_artist = "Test Artist",
                artists = any(),
                release_date = "2023-01-01",
                total_tracks = 10L,
                spotify_uri = "spotify:album:test-album-id",
                added_at = any(),
                album_type = any(),
                images = any(),
                updated_at = any(),
                external_ids = any(),
                in_library = 1,
                release_group_id = null
            )
        }
    }

    @Test
    fun `saveAlbum with Album domain object saves to database correctly`() {
        every { albumsQueries.insert(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        repository.saveAlbum(testAlbum)

        // Then
        verify {
            albumsQueries.insert(
                id = "test-album-id",
                spotify_id = any(),
                name = "Test Album",
                primary_artist = "Test Artist",
                artists = any(),
                release_date = "2023-01-01",
                total_tracks = 10L,
                spotify_uri = "spotify:album:test-album-id",
                added_at = any(),
                album_type = any(),
                images = any(),
                updated_at = any(),
                external_ids = any(),
                in_library = 1,
                release_group_id = null
            )
        }
    }


//    // SPOTIFY OPERATIONS TESTS
//
    @Test
    fun `fetchAlbum returns mapped album on success`() = runTest {
        val mockLibraryApi = mockk<LibraryApi>()
        every { spotifyApi.library } returns mockLibraryApi
        coEvery { mockLibraryApi.getAlbum("test-album-id") } returns Result.success(testAlbumDto)

        mockkObject(AlbumMapper)
        every { AlbumMapper.toDomain(testAlbumDto) } returns testAlbum

        val result = repository.fetchAlbum("test-album-id")
        assertEquals(testAlbum, result)
    }

    @Test
    fun `fetchAlbum throws exception on failure`() = runTest {
        val mockLibraryApi = mockk<LibraryApi>()
        val testException = RuntimeException("Network error")

        every { spotifyApi.library } returns mockLibraryApi
        coEvery { mockLibraryApi.getAlbum("test-album-id") } returns Result.failure(testException)

        assertFailsWith<RuntimeException> {
            repository.fetchAlbum("test-album-id")
        }
    }

    @Test
    fun `fetchMultipleAlbums processes albums in batches and saves to database`() = runTest {
        // Given
        val albumIds = (1..25).map { "album-$it" } // 25 albums to test batching
        val mockAlbums = albumIds.map { id ->
            testAlbumDto.copy(id = id, name = "Album $id")
        }
        val mockResponse = mockk<AlbumsResponse>()
        val mockLibraryApi = mockk<LibraryApi>()

        every { spotifyApi.library } returns mockLibraryApi
        every { mockResponse.albums } returns mockAlbums.take(20) andThen mockAlbums.drop(20)
        coEvery { mockLibraryApi.getMultipleAlbums(any()) } returns Result.success(mockResponse)
        every { albumsQueries.insert(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()) } just Runs
        every { eventDispatcher.dispatch(any()) } just Runs

        mockkObject(AlbumMapper)
        mockAlbums.forEach { albumDto ->
            every { AlbumMapper.toDomain(albumDto) } returns testAlbum.copy(id = albumDto.id)
        }

        val result = repository.fetchMultipleAlbums(albumIds, saveToDb = true)

        assertTrue(result.isSuccess)
        assertEquals(25, result.getOrNull()?.size)

        // Verify batching (should make 2 API calls for 25 albums)
        coVerify(exactly = 2) { mockLibraryApi.getMultipleAlbums(any()) }

        // Verify all albums were saved
        verify(exactly = 25) {
            albumsQueries.insert(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }

        // Verify events were dispatched
        verify(exactly = 25) { eventDispatcher.dispatch(any<AlbumEvent.AlbumAdded>()) }
    }


    @Test
    fun `fetchMultipleAlbums does not save to database when saveToDb is false`() = runTest {
        // Given
        val albumIds = listOf("album-1")
        val mockResponse = mockk<AlbumsResponse>()
        val mockLibraryApi = mockk<LibraryApi>()

        every { spotifyApi.library } returns mockLibraryApi
        every { mockResponse.albums } returns listOf(testAlbumDto)
        coEvery { mockLibraryApi.getMultipleAlbums(any()) } returns Result.success(mockResponse)
        every { eventDispatcher.dispatch(any()) } just Runs

        mockkObject(AlbumMapper)
        every { AlbumMapper.toDomain(testAlbumDto) } returns testAlbum

        // When
        repository.fetchMultipleAlbums(albumIds, saveToDb = false)

        // Then
        verify(exactly = 0) {
            albumsQueries.insert(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
    }

    // MISC API OPERATIONS TESTS


    @Test
    fun `fetchReleaseGroupId throws exception when UPC is not present`() = runTest {
        val albumWithoutUpc = testAlbum.copy(externalIds = emptyMap())

        assertFailsWith<IllegalArgumentException> {
            repository.fetchReleaseGroupId(albumWithoutUpc)
        }
    }

    // CHAINED OPERATIONS TESTS

}