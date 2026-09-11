package io.github.peningtonj.recordcollection.service

import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.repository.SettingsState
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.testDataFactory.TestAlbumDataFactory
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test

private const val SFX_URI = "spotify:track:6xXAl2w0mqyxsRB8ak2S7N"

class PlaybackSessionManagerTest {

    private val playbackRepository = mockk<PlaybackRepository>(relaxed = true)
    private val trackRepository = mockk<TrackRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private val queue = PlaybackQueueService(playbackRepository, trackRepository)

    private val albumA = TestAlbumDataFactory.album(id = "A", name = "Album A").copy(spotifyUri = "spotify:album:A")
    private val albumB = TestAlbumDataFactory.album(id = "B", name = "Album B").copy(spotifyUri = "spotify:album:B")

    private fun track(id: String, uri: String, durationMs: Long) = Track(
        id = id, name = id, artists = emptyList(), albumId = "A", isExplicit = false,
        trackNumber = 1, discNumber = 1, durationMs = durationMs, spotifyUri = uri,
    )

    private val aLast = track("a-last", "spotify:track:a-last", 200_000)
    private val bFirst = track("b-first", "spotify:track:b-first", 200_000)

    private fun ui(albumId: String, tracks: List<Track>) = AlbumDetailUiState(
        album = if (albumId == "A") albumA else albumB,
        tags = emptyList(), collections = emptyList(), tracks = tracks, releaseGroup = emptyList(), totalDuration = 0,
    )

    private fun playback(track: Track, progressMs: Long, playing: Boolean = true) = Playback(
        isPlaying = playing, progressMs = progressMs, track = track, device = null,
        shuffleState = false, repeatState = "off",
        lastUpdated = Clock.System.now().toEpochMilliseconds(),
    )

    private fun manager(transitionTrack: Boolean, scope: kotlinx.coroutines.CoroutineScope): PlaybackSessionManager {
        every { settingsRepository.settings } returns MutableStateFlow(SettingsState(transitionTrack = transitionTrack))
        coEvery { playbackRepository.getCurrentPlayback(any()) } returns null
        return PlaybackSessionManager(playbackRepository, queue, settingsRepository, scope)
    }

    @Test
    fun `no-SFX - next album starts a beat before the last track ends`() = runTest {
        val m = manager(transitionTrack = false, backgroundScope)
        m.startAlbum(ui("A", listOf(aLast)), queue = listOf(ui("B", listOf(bFirst))))
        runCurrent()

        m.handleQueueTransitions(playback(aLast, progressMs = 190_000)) // 10s left
        advanceTimeBy(9_600); runCurrent()
        coVerify(exactly = 0) { playbackRepository.startAlbumPlayback(albumB) }

        advanceTimeBy(200); runCurrent() // past the 300ms lead
        coVerify(exactly = 1) { playbackRepository.startAlbumPlayback(albumB) }
    }

    @Test
    fun `SFX - queued ~4s before the end, then next album after the SFX`() = runTest {
        val m = manager(transitionTrack = true, backgroundScope)
        m.startAlbum(ui("A", listOf(aLast)), queue = listOf(ui("B", listOf(bFirst))))
        runCurrent()

        m.handleQueueTransitions(playback(aLast, progressMs = 190_000)) // 10s left
        advanceTimeBy(5_900); runCurrent()
        coVerify(exactly = 0) { playbackRepository.addTrackToQueue(SFX_URI) }
        advanceTimeBy(200); runCurrent() // fires at ~6s (10s - 4s trigger)
        coVerify(exactly = 1) { playbackRepository.addTrackToQueue(SFX_URI) }

        // Now the SFX is playing with 3s left.
        val sfx = track("sfx", SFX_URI, 8_000)
        m.handleQueueTransitions(playback(sfx, progressMs = 5_000))
        advanceTimeBy(2_600); runCurrent()
        coVerify(exactly = 0) { playbackRepository.startAlbumPlayback(albumB) }
        advanceTimeBy(200); runCurrent()
        coVerify(exactly = 1) { playbackRepository.startAlbumPlayback(albumB) }
    }

    @Test
    fun `pausing the last track cancels the pending transition`() = runTest {
        val m = manager(transitionTrack = false, backgroundScope)
        m.startAlbum(ui("A", listOf(aLast)), queue = listOf(ui("B", listOf(bFirst))))
        runCurrent()

        m.handleQueueTransitions(playback(aLast, progressMs = 195_000))          // 5s left, armed
        m.handleQueueTransitions(playback(aLast, progressMs = 195_000, playing = false)) // paused
        advanceTimeBy(30_000); runCurrent()
        coVerify(exactly = 0) { playbackRepository.startAlbumPlayback(albumB) }
    }

    @Test
    fun `a transient null poll does not cancel the pending transition`() = runTest {
        val m = manager(transitionTrack = false, backgroundScope)
        m.startAlbum(ui("A", listOf(aLast)), queue = listOf(ui("B", listOf(bFirst))))
        runCurrent()

        m.handleQueueTransitions(playback(aLast, progressMs = 195_000)) // 5s left, armed
        m.handleQueueTransitions(null)                                  // transient failure
        advanceTimeBy(4_800); runCurrent()
        coVerify(exactly = 1) { playbackRepository.startAlbumPlayback(albumB) }
    }
}
