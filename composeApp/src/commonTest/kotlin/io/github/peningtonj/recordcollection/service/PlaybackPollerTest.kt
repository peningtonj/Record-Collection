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
}
