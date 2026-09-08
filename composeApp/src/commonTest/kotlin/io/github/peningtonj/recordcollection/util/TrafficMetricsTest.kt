package io.github.peningtonj.recordcollection.util

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TrafficMetricsTest {

    @Test
    fun `groupSpotifyPath collapses ids and me subpaths`() {
        assertEquals("/albums", TrafficMetrics.groupSpotifyPath("/albums/6akEvsycLGftJxYudPjmqK"))
        assertEquals("/albums", TrafficMetrics.groupSpotifyPath("/albums?ids=a,b,c"))
        assertEquals("/me/player", TrafficMetrics.groupSpotifyPath("/me/player/devices"))
        assertEquals("/me/tracks", TrafficMetrics.groupSpotifyPath("/me/tracks"))
        assertEquals("/search", TrafficMetrics.groupSpotifyPath("/search?q=kid+a&type=album"))
        assertEquals("/", TrafficMetrics.groupSpotifyPath("/"))
    }

    @Test
    fun `folds events into a per-source report`() = runTest {
        TrafficMetrics.start(backgroundScope, reportEverySeconds = 0)
        TrafficMetrics.requestReset()

        TrafficMetrics.recordFirestoreListen("library_albums", source = "LibraryScreen")
        TrafficMetrics.recordFirestoreRead("library_albums", 400, source = "LibraryScreen")
        TrafficMetrics.recordFirestoreRead("albums", 380, source = "LibraryScreen")
        TrafficMetrics.recordFirestoreWrite("tags", source = "AlbumScreen")
        TrafficMetrics.recordSpotify("/me/player", "GET", 200, rateLimitRemaining = 12, source = "PlaybackPoller")
        TrafficMetrics.recordSpotify("/albums", "GET", 429, retryAfterSeconds = 3, source = "LibraryScreen")

        val report = TrafficMetrics.reportNow()

        assertContains(report, "readDocs=780")          // 400 + 380
        assertContains(report, "listeners=1")
        assertContains(report, "writes=1")
        assertContains(report, "LibraryScreen: readDocs=780")
        assertContains(report, "library_albums=2")       // listen + read on that collection
        assertContains(report, "429=1")
        assertContains(report, "throttled=3s")
        assertContains(report, "minRemaining=12")
        assertContains(report, "PlaybackPoller: requests=1")
    }

    @Test
    fun `reset zeroes the counters`() = runTest {
        TrafficMetrics.start(backgroundScope, reportEverySeconds = 0)
        TrafficMetrics.recordSpotify("/albums", "GET", 200, source = "X")
        TrafficMetrics.requestReset()

        val report = TrafficMetrics.reportNow()

        assertContains(report, "requests=0")
    }
}
