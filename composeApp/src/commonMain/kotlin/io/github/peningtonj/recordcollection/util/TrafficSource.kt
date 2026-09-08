package io.github.peningtonj.recordcollection.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The screen / background worker currently believed to be generating backend traffic.
 * Used by [TrafficMetrics] to attribute Firestore + Spotify calls.
 *
 * v1 attribution is coarse: the UI sets [current] from the navigation host, so a call
 * made shortly after navigating away can be misattributed. Known background workers
 * (the playback poller, sync) are attributed by passing an explicit source instead.
 */
object TrafficSource {
    /** Convention: screen names ("LibraryScreen") or worker names ("PlaybackPoller"). */
    const val STARTUP = "startup"
    const val PLAYBACK_POLLER = "PlaybackPoller"
    const val LIBRARY_SYNC = "LibrarySync"
    const val ALBUM_PROCESSING = "AlbumProcessingHandler"
    const val UNKNOWN = "unknown"

    private val _current = MutableStateFlow(STARTUP)
    val currentFlow: StateFlow<String> = _current

    var current: String
        get() = _current.value
        set(value) { _current.value = value }
}
