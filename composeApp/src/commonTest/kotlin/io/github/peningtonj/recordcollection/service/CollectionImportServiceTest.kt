package io.github.peningtonj.recordcollection.service

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.network.spotify.model.SearchType
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.PlaylistRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.testDataFactory.TestTrackDataFactory
import io.github.peningtonj.recordcollection.viewmodel.AlbumLookUpResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionImportServiceTest {

    private val albumCollectionRepository = mockk<AlbumCollectionRepository>()
    private val searchRepository = mockk<SearchRepository>()
    private val playlistRepository = mockk<PlaylistRepository>()

    private lateinit var service: CollectionImportService
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testAlbum = TestAlbumDataFactory.album()

    private val testAlbumNameAndArtist = AlbumNameAndArtist(
        album = "Test Album",
        artist = "Test Artist"
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        service = CollectionImportService(
            albumCollectionRepository = albumCollectionRepository,
            searchRepository = searchRepository,
            playlistRepository = playlistRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `parseAlbumAndArtistResponse correctly parses valid JSON`() {
        // Given
        val validJson = """
            [
                {"album": "Rumours", "artist": "Fleetwood Mac"},
                {"album": "Dark Side of the Moon", "artist": "Pink Floyd"}
            ]
        """.trimIndent()

        // When
        val result = service.parseAlbumAndArtistResponse(validJson)

        // Then
        assertEquals(2, result.size)
        assertEquals("Rumours", result[0].album)
        assertEquals("Fleetwood Mac", result[0].artist)
        assertEquals("Dark Side of the Moon", result[1].album)
        assertEquals("Pink Floyd", result[1].artist)
    }

    @Test
    fun `parseAlbumAndArtistResponse handles empty JSON array`() {
        // Given
        val emptyJson = "[]"

        // When
        val result = service.parseAlbumAndArtistResponse(emptyJson)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parseAlbumAndArtistResponse throws exception for invalid JSON`() {
        // Given
        val invalidJson = "invalid json"

        // When & Then
        assertFailsWith<kotlinx.serialization.SerializationException> {
            service.parseAlbumAndArtistResponse(invalidJson)
        }
    }

    @Test
    fun `lookupAlbum returns first album from search results`() = runTest {
        // Given
        val searchResults = mockk<SearchResult>()
        val albums = listOf(testAlbum, testAlbum.copy(id = "album-2"))

        every { searchResults.albums } returns albums
        coEvery { searchRepository.searchSpotify(any(), any()) } returns searchResults

        // When
        val result = service.lookupAlbum(testAlbumNameAndArtist)

        // Then
        assertEquals(testAlbum, result)
        coVerify {
            searchRepository.searchSpotify(
                "Test Album, Test Artist",
                listOf(SearchType.ALBUM)
            )
        }
    }

    @Test
    fun `lookupAlbum returns null when no albums found`() = runTest {
        // Given
        val searchResults = mockk<SearchResult>()
        every { searchResults.albums } returns null
        coEvery { searchRepository.searchSpotify(any(), any()) } returns searchResults

        // When
        val result = service.lookupAlbum(testAlbumNameAndArtist)

        // Then
        assertNull(result)
    }

    @Test
    fun `lookupAlbum returns null when albums list is empty`() = runTest {
        // Given
        val searchResults = mockk<SearchResult>()
        every { searchResults.albums } returns emptyList()
        coEvery { searchRepository.searchSpotify(any(), any()) } returns searchResults

        // When
        val result = service.lookupAlbum(testAlbumNameAndArtist)

        // Then
        assertNull(result)
    }

    @Test
    fun `streamAlbumLookups calls onResult for each album lookup`() = runTest {
        val albumNames = listOf(
            AlbumNameAndArtist("Album 1", "Artist 1"),
            AlbumNameAndArtist("Album 2", "Artist 2")
        )
        val results = mutableListOf<AlbumLookUpResult>()
        val searchResults = mockk<SearchResult>()

        every { searchResults.albums } returns listOf(testAlbum)
        coEvery { searchRepository.searchSpotify(any(), any()) } returns searchResults

        service.streamAlbumLookups(albumNames) { result ->
            results.add(result)
        }
        advanceUntilIdle()

        // Then
        assertEquals(2, results.size)
        assertEquals("Album 1", results[0].query.album)
        assertEquals("Artist 1", results[0].query.artist)
        assertEquals(testAlbum, results[0].album)

        assertEquals("Album 2", results[1].query.album)
        assertEquals("Artist 2", results[1].query.artist)
        assertEquals(testAlbum, results[1].album)
    }

    @Test
    fun `streamAlbumLookups handles null album results`() = runTest {
        // Given
        val albumNames = listOf(testAlbumNameAndArtist)
        val results = mutableListOf<AlbumLookUpResult>()
        val searchResults = mockk<SearchResult>()

        every { searchResults.albums } returns null
        coEvery { searchRepository.searchSpotify(any(), any()) } returns searchResults

        // When
        service.streamAlbumLookups(albumNames) { result ->
            results.add(result)
        }
        advanceUntilIdle()

        // Then
        assertEquals(1, results.size)
        assertEquals(testAlbumNameAndArtist, results[0].query)
        assertNull(results[0].album)
    }

    @Test
    fun `streamAlbumLookups handles empty album list`() = runTest {
        // Given
        val albumNames = emptyList<AlbumNameAndArtist>()
        val results = mutableListOf<AlbumLookUpResult>()

        // When
        service.streamAlbumLookups(albumNames) { result ->
            results.add(result)
        }
        advanceUntilIdle()

        // Then
        assertTrue(results.isEmpty())
        coVerify(exactly = 0) { searchRepository.searchSpotify(any(), any()) }
    }

    @Test
    fun `getAlbumsFromPlaylist returns distinct albums from playlist tracks`() = runTest {
        // Given
        val playlistId = "playlist-123"
        val testAlbum2 = TestAlbumDataFactory.album("test-album-2-id", "Test Album 2")
        val tracks = (1..3).map {
            TestTrackDataFactory.track(it, testAlbum)
        } + (1 .. 3).map {
            TestTrackDataFactory.track(it, testAlbum2)
        }

        coEvery { playlistRepository.getPlaylistTracks(playlistId) } returns tracks

        // When
        val result = service.getAlbumsFromPlaylist(playlistId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.contains(testAlbum))
        assertTrue(result.contains(testAlbum2))
        coVerify { playlistRepository.getPlaylistTracks(playlistId) }
    }

    @Test
    fun `getAlbumsFromPlaylist returns empty list when no tracks have albums`() = runTest {
        // Given
        val playlistId = "playlist-123"
        val tracks = (1..3).map {
            TestTrackDataFactory.track(it, testAlbum)
        }

        coEvery { playlistRepository.getPlaylistTracks(playlistId) } returns tracks

        // When
        val result = service.getAlbumsFromPlaylist(playlistId)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getAlbumsFromPlaylist returns empty list when playlist has no tracks`() = runTest {
        // Given
        val playlistId = "playlist-123"
        coEvery { playlistRepository.getPlaylistTracks(playlistId) } returns emptyList()

        // When
        val result = service.getAlbumsFromPlaylist(playlistId)

        // Then
        assertTrue(result.isEmpty())
    }

}

// Test for the data class
class AlbumNameAndArtistTest {

    @Test
    fun `AlbumNameAndArtist creates correctly`() {
        // Given
        val album = "Test Album"
        val artist = "Test Artist"

        val result = AlbumNameAndArtist(album, artist)

        assertEquals(album, result.album)
        assertEquals(artist, result.artist)
    }

    @Test
    fun `AlbumNameAndArtist serializes and deserializes correctly`() {
        // Given
        val original = AlbumNameAndArtist("Test Album", "Test Artist")
        val json = Json.encodeToString(AlbumNameAndArtist.serializer(), original)

        // When
        val deserialized = Json.decodeFromString(AlbumNameAndArtist.serializer(), json)

        // Then
        assertEquals(original, deserialized)
    }
}