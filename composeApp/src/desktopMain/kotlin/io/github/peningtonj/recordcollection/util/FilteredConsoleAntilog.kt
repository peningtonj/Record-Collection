package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel

/**
 * A console Antilog that suppresses Firebase- and Spotify-tagged debug messages
 * from the terminal.
 *
 * Those messages are already captured in full by [FirebaseFileAntilog] /
 * [SpotifyFileAntilog], so echoing them to the terminal only creates noise that
 * buries more critical output.
 *
 * WARNING and ERROR level messages are still shown for both categories so that
 * genuine problems (rate limits, auth failures, etc.) surface immediately.
 *
 * All other log categories are printed at every level as normal.
 */
class FilteredConsoleAntilog : Antilog() {

    private val fileOnlyTags = setOf(
        LoggingUtils.Category.FIREBASE.tag,
        LoggingUtils.Category.SPOTIFY.tag,
    )

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        // DEBUG/INFO for file-only categories are written to their dedicated log
        // files — suppress from console to keep it clean.
        // WARNING and above still surface so critical errors are not hidden.
        if (tag in fileOnlyTags && priority < LogLevel.WARNING) return

        val levelChar = when (priority) {
            LogLevel.VERBOSE -> 'V'
            LogLevel.DEBUG   -> 'D'
            LogLevel.INFO    -> 'I'
            LogLevel.WARNING -> 'W'
            LogLevel.ERROR   -> 'E'
            LogLevel.ASSERT  -> 'A'
        }
        println("$levelChar/$tag: $message")
        throwable?.printStackTrace()
    }
}

