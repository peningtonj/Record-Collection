package io.github.peningtonj.recordcollection.respository

import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
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
    private lateinit var firestore: FirebaseFirestore

    @MockK
    private lateinit var albumsCollection: CollectionReference

    @MockK
    private lateinit var albumDocRef: DocumentReference

    @MockK
    private lateinit var albumDocSnapshot: DocumentSnapshot

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

        every { firestore.collection("albums") } returns albumsCollection
        every { albumsCollection.document(any()) } returns albumDocRef
        every { albumDocSnapshot.exists } returns false
        coEvery { albumDocRef.get() } returns albumDocSnapshot
        coEvery { albumDocRef.set(any<Any>()) } just Runs
        coEvery { albumDocRef.set(any<Map<String, Any>>(), merge = true) } just Runs
        coEvery { eventDispatcher.dispatch(any()) } just Runs

        repository = AlbumRepository(
            firestore = firestore,
            spotifyApi = spotifyApi,
            miscApi = miscApi,
            eventDispatcher = eventDispatcher
        )
    }

    // FIRESTORE OPERATIONS TESTS

    @Test
    fun `saveAlbum with AlbumDto saves album to Firestore`() = runTest {
        repository.saveAlbum(testAlbumDto, addToUsersLibrary = true)

        coVerify { albumDocRef.set(any<Any>()) }
        coVerify { eventDispatcher.dispatch(any<AlbumEvent.AlbumAdded>()) }
    }

    @Test
    fun `saveAlbum with Album domain object saves to Firestore`() = runTest {
        repository.saveAlbum(testAlbum)

        coVerify { albumDocRef.set(any<Any>()) }
        coVerify { eventDispatcher.dispatch(any<AlbumEvent.AlbumAdded>()) }
    }

    @Test
    fun `albumExists returns false when document does not exist`() = runTest {
        every { albumDocSnapshot.exists } returns false

        val result = repository.albumExists("test-album-id")

        assertEquals(false, result)
    }

    @Test
    fun `albumExists returns true when document exists`() = runTest {
        every { albumDocSnapshot.exists } returns true

        val result = repository.albumExists("test-album-id")

        assertEquals(true, result)
    }

    // SPOTIFY OPERATIONS TESTS

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
    fun `fetchMultipleAlbums processes albums in batches and saves to Firestore`() = runTest {
        val albumIds = (1..25).map { "album-$it" }
        val mockAlbums = albumIds.map { id -> testAlbumDto.copy(id = id, name = "Album $id") }
        val mockResponse = mockk<AlbumsResponse>()
        val mockLibraryApi = mockk<LibraryApi>()

        every { spotifyApi.library } returns mockLibraryApi
        every { mockResponse.albums } returns mockAlbums.take(20) andThen mockAlbums.drop(20)
        coEvery { mockLibraryApi.getMultipleAlbums(any()) } returns Result.success(mockResponse)

        mockkObject(AlbumMapper)
        mockAlbums.forEach { albumDto ->
            every { AlbumMapper.toDomain(albumDto) } returns testAlbum.copy(id = albumDto.id)
            every { AlbumMapper.toDocument(any()) } returns mockk(relaxed = true)
        }

        val result = repository.fetchMultipleAlbums(albumIds, saveToDb = true)

        assertTrue(result.isSuccess)
        assertEquals(25, result.getOrNull()?.size)

        // Verify batching (should make 2 API calls for 25 albums)
        coVerify(exactly = 2) { mockLibraryApi.getMultipleAlbums(any()) }

        // Verify events were dispatched
        coVerify(exactly = 25) { eventDispatcher.dispatch(any<AlbumEvent.AlbumAdded>()) }
    }

    @Test
    fun `fetchMultipleAlbums does not save to Firestore when saveToDb is false`() = runTest {
        val albumIds = listOf("album-1")
        val mockResponse = mockk<AlbumsResponse>()
        val mockLibraryApi = mockk<LibraryApi>()

        every { spotifyApi.library } returns mockLibraryApi
        every { mockResponse.albums } returns listOf(testAlbumDto)
        coEvery { mockLibraryApi.getMultipleAlbums(any()) } returns Result.success(mockResponse)

        mockkObject(AlbumMapper)
        every { AlbumMapper.toDomain(testAlbumDto) } returns testAlbum

        repository.fetchMultipleAlbums(albumIds, saveToDb = false)

        // document.set should not be called when saveToDb = false
        coVerify(exactly = 0) { albumDocRef.set(any<Any>()) }
    }

    // MISC API OPERATIONS TESTS

    @Test
    fun `fetchReleaseGroupId throws exception when UPC is not present`() = runTest {
        val albumWithoutUpc = testAlbum.copy(externalIds = emptyMap())

        assertFailsWith<IllegalArgumentException> {
            repository.fetchReleaseGroupId(albumWithoutUpc)
        }
    }

}