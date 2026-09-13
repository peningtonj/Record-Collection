package io.github.peningtonj.recordcollection.viewmodel

import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.testDataFactory.TestTrackDataFactory
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * "Add All to Saved Songs" wasn't reflected on the open album screen: the shared action
 * only writes to Spotify/Firestore, unlike the per-track heart button which also calls
 * setTrackSaved() to optimistically flip that track in this screen's own state. The fix
 * (AlbumScreen.kt) calls setTrackSaved() for every track after the bulk write — this locks
 * that setTrackSaved() correctly updates multiple tracks in one state, independently.
 */
class AlbumDetailViewModelTest {

    private val useCase = mockk<GetAlbumDetailUseCase>()
    private val album = TestAlbumDataFactory.album(id = "alb1")
    private val t1 = TestTrackDataFactory.track(trackNumber = 1, album = album)
    private val t2 = TestTrackDataFactory.track(trackNumber = 2, album = album)
    private val t3 = TestTrackDataFactory.track(trackNumber = 3, album = album)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): AlbumDetailViewModel {
        coEvery { useCase.execute("alb1", "sp1", any()) } returns flowOf(
            AlbumDetailUiState(
                album = album, tags = emptyList(), collections = emptyList(),
                tracks = listOf(t1, t2, t3), releaseGroup = emptyList(), totalDuration = 0L,
            ),
        )
        return AlbumDetailViewModel("alb1", "sp1", useCase).also { it.loadAlbum() }
    }

    @Test
    fun `setTrackSaved for every track (the bulk-add path) flips all of them, not just one`() {
        val vm = viewModel()

        listOf(t1, t2, t3).forEach { vm.setTrackSaved(it.id, true) }

        val tracks = (vm.uiState.value as AlbumScreenUiState.Success).albumDetail.tracks
        assertTrue(tracks.all { it.isSaved }, "every track should be saved: $tracks")
    }

    @Test
    fun `setTrackSaved only touches the requested track`() {
        val vm = viewModel()

        vm.setTrackSaved(t1.id, true)

        val tracks = (vm.uiState.value as AlbumScreenUiState.Success).albumDetail.tracks.associateBy { it.id }
        assertTrue(tracks.getValue(t1.id).isSaved)
        assertTrue(!tracks.getValue(t2.id).isSaved)
        assertTrue(!tracks.getValue(t3.id).isSaved)
    }
}
