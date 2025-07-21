package io.github.peningtonj.recordcollection.viewmodel

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.network.miscApi.model.Release
import io.github.peningtonj.recordcollection.network.miscApi.model.ReleaseGroup
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.service.TagService
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.usecase.ReleaseGroupUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    private val albumRepository = mockk<AlbumRepository>()
    private val ratingRepository = mockk<RatingRepository>()
    private val collectionAlbumRepository = mockk<CollectionAlbumRepository>()
    private val tagService = mockk<TagService>()
    private val releaseGroupUseCase = mockk<ReleaseGroupUseCase>()

    private lateinit var viewModel: AlbumViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testAlbum = TestAlbumDataFactory.album()
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AlbumViewModel(
            albumRepository = albumRepository,
            ratingRepository = ratingRepository,
            collectionAlbumRepository = collectionAlbumRepository,
            tagService = tagService,
            releaseGroupUseCase = releaseGroupUseCase
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun `setRating calls ratingRepository with correct parameters`() {
        val albumId = "test-album-id"
        val rating = 5
        every { ratingRepository.addRating(any(), any()) } just Runs

        viewModel.setRating(albumId, rating)

        verify { ratingRepository.addRating(albumId, 5L) }
    }

    @Test
    fun `addTagToAlbum calls tagService with correct parameters`() {
        val albumId = "test-album-id"
        val tagKey = "genre"
        val tagValue = "rock"
        every { tagService.addTagToAlbum(any(), any(), any()) } just Runs

        viewModel.addTagToAlbum(albumId, tagKey, tagValue)

        verify { tagService.addTagToAlbum(albumId, tagKey, tagValue) }
    }

    @Test
    fun `removeTagFromAlbum calls tagService with correct parameters`() {
        val albumId = "test-album-id"
        val tagId = "tag-id"
        every { tagService.removeTagFromAlbum(any(), any()) } just Runs

        viewModel.removeTagFromAlbum(albumId, tagId)

        verify { tagService.removeTagFromAlbum(albumId, tagId) }
    }

    @Test
    fun `updateReleaseGroup updates status to Updating then back to Idle`() = runTest {
        val mockRelease = mockk<Release>()
        val mockReleaseGroup = mockk<ReleaseGroup>()
        val releaseGroupId = "release-group-id"
        val releases = listOf(mockRelease)
        val albums = listOf(testAlbum)
        val releaseTitle = "test-release-title"
        val disambiguation = "test-disambiguation"

        every { mockRelease.releaseGroup } returns mockReleaseGroup
        every { mockReleaseGroup.id } returns releaseGroupId
        every { mockRelease.title } returns releaseTitle
        every { mockRelease.disambiguation } returns disambiguation

        coEvery { releaseGroupUseCase.getReleaseFromAlbum(testAlbum) } returns mockRelease
        coEvery { albumRepository.updateReleaseGroupId(any(), any()) } just Runs
        coEvery { releaseGroupUseCase.getReleases(releaseGroupId) } returns releases
        coEvery { releaseGroupUseCase.searchAlbumsFromReleaseGroup(any(), any()) } returns albums
        coEvery { releaseGroupUseCase.updateAlbums(any(), any()) } just Runs

        viewModel.updateReleaseGroup(testAlbum)
        advanceUntilIdle()

        assertEquals(ReleaseGroupStatus.Idle, viewModel.releaseGroupStatus.value)

        coVerify { releaseGroupUseCase.getReleaseFromAlbum(testAlbum) }
        coVerify { albumRepository.updateReleaseGroupId(testAlbum.id, releaseGroupId) }
        coVerify { releaseGroupUseCase.getReleases(releaseGroupId) }
        coVerify { releaseGroupUseCase.searchAlbumsFromReleaseGroup(any(), testAlbum.primaryArtist) }
        coVerify { releaseGroupUseCase.updateAlbums(releaseGroupId, albums) }
    }

    @Test
    fun `updateReleaseGroup returns early when releaseGroupId is null`() = runTest {
        val mockRelease = mockk<Release>()

        every { mockRelease.releaseGroup } returns null
        coEvery { releaseGroupUseCase.getReleaseFromAlbum(testAlbum) } returns mockRelease

        viewModel.updateReleaseGroup(testAlbum)
        advanceUntilIdle()

        coVerify { releaseGroupUseCase.getReleaseFromAlbum(testAlbum) }
        coVerify(exactly = 0) { albumRepository.updateReleaseGroupId(any(), any()) }
        coVerify(exactly = 0) { releaseGroupUseCase.getReleases(any()) }
    }

    @Test
    fun `updateReleaseGroup returns early when release is null`() = runTest {
        coEvery { releaseGroupUseCase.getReleaseFromAlbum(testAlbum) } returns null

        viewModel.updateReleaseGroup(testAlbum)
        advanceUntilIdle()

        coVerify { releaseGroupUseCase.getReleaseFromAlbum(testAlbum) }
        coVerify(exactly = 0) { albumRepository.updateReleaseGroupId(any(), any()) }
    }

    @Test
    fun `addAlbumToCollection saves new album when not found in repository`() = runTest {
        val collectionName = "test-collection"
        val addToLibraryOverride = true

        coEvery { albumRepository.getAlbumByNameAndArtistIfPresent(any(), any()) } returns flowOf(null)
        coEvery { albumRepository.saveAlbum(any<Album>(), any()) } just Runs
        coEvery { collectionAlbumRepository.addAlbumToCollection(any(), any()) } just Runs

        viewModel.addAlbumToCollection(testAlbum, collectionName, addToLibraryOverride)
        advanceUntilIdle()

        coVerify { albumRepository.getAlbumByNameAndArtistIfPresent(testAlbum.name, testAlbum.primaryArtist) }
        coVerify { albumRepository.saveAlbum(testAlbum, addToLibraryOverride) }
        coVerify { collectionAlbumRepository.addAlbumToCollection(collectionName, testAlbum.id) }
    }

    @Test
    fun `addAlbumToCollection uses existing album when found in repository`() = runTest {
        val collectionName = "test-collection"
        val addToLibraryOverride = true
        val existingAlbum = testAlbum.copy(id = "existing-album-id")

        coEvery { albumRepository.getAlbumByNameAndArtistIfPresent(any(), any()) } returns flowOf(existingAlbum)
        coEvery { albumRepository.addAlbumToLibrary(any()) } just Runs
        coEvery { collectionAlbumRepository.addAlbumToCollection(any(), any()) } just Runs

        viewModel.addAlbumToCollection(testAlbum, collectionName, addToLibraryOverride)
        advanceUntilIdle()

        // Then
        coVerify { albumRepository.getAlbumByNameAndArtistIfPresent(testAlbum.name, testAlbum.primaryArtist) }
        coVerify { albumRepository.addAlbumToLibrary(testAlbum.id) }
        coVerify { collectionAlbumRepository.addAlbumToCollection(collectionName, existingAlbum.id) }
        coVerify(exactly = 0) { albumRepository.saveAlbum(any<Album>(), any()) }
    }

    @Test
    fun `addAlbumToCollection with existing album but no addToLibrary override`() = runTest {
        val collectionName = "test-collection"
        val existingAlbum = testAlbum.copy(id = "existing-album-id")

        coEvery { albumRepository.getAlbumByNameAndArtistIfPresent(any(), any()) } returns flowOf(existingAlbum)
        coEvery { collectionAlbumRepository.addAlbumToCollection(any(), any()) } just Runs

        viewModel.addAlbumToCollection(testAlbum, collectionName, addToLibraryOverrideValue = false)
        advanceUntilIdle()

        coVerify { collectionAlbumRepository.addAlbumToCollection(collectionName, existingAlbum.id) }
        coVerify(exactly = 0) { albumRepository.addAlbumToLibrary(any()) }
    }

    @Test
    fun `removeAlbumFromCollection calls repository with correct parameters`() = runTest {
        val collectionName = "test-collection"
        coEvery { collectionAlbumRepository.removeAlbumFromCollection(any(), any()) } just Runs

        viewModel.removeAlbumFromCollection(testAlbum, collectionName)
        advanceUntilIdle()

        coVerify { collectionAlbumRepository.removeAlbumFromCollection(collectionName, testAlbum.id) }
    }

    @Test
    fun `releaseGroupStatus initial state is Idle`() {
        assertEquals(ReleaseGroupStatus.Idle, viewModel.releaseGroupStatus.value)
    }
}