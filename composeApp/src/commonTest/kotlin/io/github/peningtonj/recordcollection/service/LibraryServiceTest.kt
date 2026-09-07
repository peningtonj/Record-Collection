package io.github.peningtonj.recordcollection.service

import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.repository.*
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.viewmodel.LibraryDifferences
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class LibraryServiceTest {

    private val albumRepository = mockk<AlbumRepository>()
    private val artistRepository = mockk<ArtistRepository>()
    private val profileRepository = mockk<ProfileRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val trackRepository = mockk<TrackRepository>()
    private val userSessionRepository = mockk<UserSessionRepository>(relaxed = true)

    private lateinit var service: LibraryService

    val uniqueLocalAlbums = (1..2).map { TestAlbumDataFactory.album("unique-album-test-id-$it", "Local Unique Album $it") }
    val uniqueRemoteAlbumsDto = (1..2).map {
        TestAlbumDataFactory.savedAlbumDto(
        TestAlbumDataFactory.albumDto("remote-album-test-id-$it", "Remote Test Album $it")
        )
    }
    val sharedAlbumsDto = (1..2).map {
        TestAlbumDataFactory.savedAlbumDto(
        TestAlbumDataFactory.albumDto("shared-album-test-id-$it", "Shared Test Album $it")
        )
    }

    // Domain albums are derived from the same DTOs so their ids match what the service
    // computes via AlbumMapper.toDomain (SHA-256 of name+artist — see TECH_DEBT 2.2).
    val uniqueRemoteAlbums = uniqueRemoteAlbumsDto.map { AlbumMapper.toDomain(it.album) }
    val sharedAlbums = sharedAlbumsDto.map { AlbumMapper.toDomain(it.album) }

    val localAlbums = uniqueLocalAlbums + sharedAlbums
    val remoteAlbumsDtos = uniqueRemoteAlbumsDto + sharedAlbumsDto
    val remoteAlbums = uniqueRemoteAlbums + sharedAlbums
    val duplicateAlbum = TestAlbumDataFactory.album("unique-album-test-id-1", "Local Unique Album 1")

    @BeforeTest
    fun setup() {
        coEvery { artistRepository.fetchArtistsWithEnhancedGenres(any(), any()) } returns emptyList()
        service = LibraryService(
            albumRepository,
            artistRepository,
            profileRepository,
            settingsRepository,
            trackRepository,
            userSessionRepository
        )
    }

    @Test
    fun `test identify duplicates returns older duplicate`() {
        val result = service.identifyDuplicates(listOf(localAlbums[0], duplicateAlbum))

        assertEquals(listOf(localAlbums[0]), result)
    }

    @Test
    fun `test get library differences`() = runTest {
        coEvery { profileRepository.fetchUserSavedAlbums() } returns remoteAlbumsDtos

        coEvery { albumRepository.getAllAlbumsInLibrary() } returns flowOf(localAlbums)

        val result: LibraryDifferences = service.getLibraryDifferences()

        assertEquals(0, result.localDuplicates.size)
        assertEquals(0, result.userSavedAlbumsDuplicates.size)
        assertEquals(2, result.onlyInLocal)
        assertEquals(2, result.onlyInSpotify)
        assertEquals(2, result.inBoth)
    }

    @Test
    fun `test apply sync, combine and deduplicate`() = runTest {
        val differences = LibraryDifferences(
            localCount = 2,
            spotifyCount = 2,
            onlyInLocal = 1,
            onlyInSpotify = 1,
            inBoth = 1,
            localLibrary = localAlbums,
            userSavedAlbums = remoteAlbums,
            localDuplicates = listOf(duplicateAlbum),
            userSavedAlbumsDuplicates = listOf(duplicateAlbum)
        )

        coEvery { albumRepository.removeAlbumFromLibrary(any()) } just Runs
        coEvery { profileRepository.removeAlbumsFromSpotifyLibrary(any()) } just Runs
        coEvery { profileRepository.addAlbumsToSpotifyLibrary(any()) } just Runs
        coEvery { albumRepository.saveAlbumIfNotPresent(any()) } just Runs
        coEvery { albumRepository.addAlbumToLibrary(any()) } just Runs

        service.applySync(differences, SyncAction.Combine)

        coVerify { albumRepository.saveAlbumIfNotPresent(uniqueRemoteAlbums[0]) }
        coVerify { albumRepository.saveAlbumIfNotPresent(uniqueRemoteAlbums[1]) }
        coVerify { albumRepository.addAlbumToLibrary(uniqueRemoteAlbums[0].id) }
        coVerify { albumRepository.addAlbumToLibrary(uniqueRemoteAlbums[1].id) }
        coVerify { profileRepository.addAlbumsToSpotifyLibrary(match { it.any { it.id == uniqueLocalAlbums[0].id } }) }
        coVerify { profileRepository.addAlbumsToSpotifyLibrary(match { it.any { it.id == uniqueLocalAlbums[1].id } }) }
    }

    @Test
    fun `test apply sync, local only`() = runTest {
        val differences = LibraryDifferences(
            localCount = 2,
            spotifyCount = 2,
            onlyInLocal = 1,
            onlyInSpotify = 1,
            inBoth = 1,
            localLibrary = localAlbums,
            userSavedAlbums = remoteAlbums,
            localDuplicates = emptyList(),
            userSavedAlbumsDuplicates = emptyList()
        )

        coEvery { albumRepository.removeAlbumFromLibrary(any()) } just Runs
        coEvery { profileRepository.removeAlbumsFromSpotifyLibrary(any()) } just Runs
        coEvery { profileRepository.addAlbumsToSpotifyLibrary(any()) } just Runs
        coEvery { albumRepository.saveAlbumIfNotPresent(any()) } just Runs
        coEvery { albumRepository.addAlbumToLibrary(any()) } just Runs

        service.applySync(differences, SyncAction.UseLocal)

        coVerify { profileRepository.removeAlbumsFromSpotifyLibrary(uniqueRemoteAlbums) }
    }

    @Test
    fun `test apply sync, use spotify removes local-only albums`() = runTest {
        val differences = LibraryDifferences(
            localCount = 2,
            spotifyCount = 2,
            onlyInLocal = 1,
            onlyInSpotify = 1,
            inBoth = 1,
            localLibrary = localAlbums,
            userSavedAlbums = remoteAlbums,
            localDuplicates = emptyList(),
            userSavedAlbumsDuplicates = emptyList()
        )

        coEvery { albumRepository.removeAlbumFromLibrary(any()) } just Runs
        coEvery { profileRepository.removeAlbumsFromSpotifyLibrary(any()) } just Runs
        coEvery { profileRepository.addAlbumsToSpotifyLibrary(any()) } just Runs
        coEvery { albumRepository.saveAlbumIfNotPresent(any()) } just Runs
        coEvery { albumRepository.addAlbumToLibrary(any()) } just Runs

        service.applySync(differences, SyncAction.UseSpotify)

        coVerify { albumRepository.removeAlbumFromLibrary(uniqueLocalAlbums[0].id) }
        coVerify { albumRepository.removeAlbumFromLibrary(uniqueLocalAlbums[1].id) }
    }

    @Test
    fun `test apply sync, intersection only`() = runTest {
        val differences = LibraryDifferences(
            localCount = 2,
            spotifyCount = 2,
            onlyInLocal = 1,
            onlyInSpotify = 1,
            inBoth = 1,
            localLibrary = localAlbums,
            userSavedAlbums = remoteAlbums,
            localDuplicates = emptyList(),
            userSavedAlbumsDuplicates = emptyList()
        )

        coEvery { albumRepository.removeAlbumFromLibrary(any()) } just Runs
        coEvery { profileRepository.removeAlbumsFromSpotifyLibrary(any()) } just Runs
        coEvery { profileRepository.addAlbumsToSpotifyLibrary(any()) } just Runs
        coEvery { albumRepository.saveAlbumIfNotPresent(any()) } just Runs
        coEvery { albumRepository.addAlbumToLibrary(any()) } just Runs

        service.applySync(differences, SyncAction.Intersection)

        coVerify { albumRepository.removeAlbumFromLibrary(uniqueLocalAlbums[0].id) }
        coVerify { albumRepository.removeAlbumFromLibrary(uniqueLocalAlbums[1].id) }
        coVerify { profileRepository.removeAlbumsFromSpotifyLibrary(uniqueRemoteAlbums) }

    }
}
