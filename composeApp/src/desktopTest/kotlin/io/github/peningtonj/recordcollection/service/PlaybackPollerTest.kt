package io.github.peningtonj.recordcollection.service

import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PlaybackPollerTest {

    /** TECH_DEBT 2.8 — an idle poller must ramp its delay up, not poll forever at 8 s. */
    @Test
    fun `poller backs off progressively while nothing is playing`() = runTest {
        val repo = mockk<PlaybackRepository>(relaxed = true)
        coEvery { repo.getCurrentPlayback(any()) } returns null

        val poller = PlaybackPoller(repo, onPlaybackUpdate = {}, onError = {})
        poller.start(this)

        runCurrent()
        coVerify(exactly = 1) { repo.getCurrentPlayback(any()) } // t=0

        advanceTimeBy(PLAYBACK_IDLE_BACKOFF_STEPS[0] + 1); runCurrent()
        coVerify(exactly = 2) { repo.getCurrentPlayback(any()) } // +8s

        advanceTimeBy(PLAYBACK_IDLE_BACKOFF_STEPS[1] + 1); runCurrent()
        coVerify(exactly = 3) { repo.getCurrentPlayback(any()) } // +20s

        // The next step is 45 s — 20 s more is not enough to fire again.
        advanceTimeBy(PLAYBACK_IDLE_BACKOFF_STEPS[1]); runCurrent()
        coVerify(exactly = 3) { repo.getCurrentPlayback(any()) }

        poller.stop()
    }

    /** A transport command must cut the idle wait short, not sit out a 45–60 s back-off. */
    @Test
    fun `pollNowActive re-polls immediately mid back-off`() = runTest {
        val repo = mockk<PlaybackRepository>(relaxed = true)
        coEvery { repo.getCurrentPlayback(any()) } returns null

        val poller = PlaybackPoller(repo, onPlaybackUpdate = {}, onError = {})
        poller.start(this)

        runCurrent()
        advanceTimeBy(PLAYBACK_IDLE_BACKOFF_STEPS[0] + 1); runCurrent()
        advanceTimeBy(PLAYBACK_IDLE_BACKOFF_STEPS[1] + 1); runCurrent()
        coVerify(exactly = 3) { repo.getCurrentPlayback(any()) } // now waiting ~45 s

        poller.pollNowActive()
        runCurrent()
        coVerify(exactly = 4) { repo.getCurrentPlayback(any()) } // fired without advancing time

        // The next poll is held at the active cadence even though nothing is playing yet,
        // so a play command Spotify hasn't registered is still caught ~2.5 s later.
        advanceTimeBy(PLAYBACK_ACTIVE_POLLING_DELAY + 1); runCurrent()
        coVerify(exactly = 5) { repo.getCurrentPlayback(any()) }

        // Still nothing playing → the idle back-off resumes from the start (8 s).
        advanceTimeBy(PLAYBACK_ACTIVE_POLLING_DELAY); runCurrent()
        coVerify(exactly = 5) { repo.getCurrentPlayback(any()) }
        advanceTimeBy(PLAYBACK_IDLE_BACKOFF_STEPS[0]); runCurrent()
        coVerify(exactly = 6) { repo.getCurrentPlayback(any()) }

        poller.stop()
    }
}
