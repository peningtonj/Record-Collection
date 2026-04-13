package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel

/**
 * A console Antilog that suppresses Firebase-tagged debug messages from the terminal.
 *
 * Firebase DEBUG / VERBOSE / INFO messages are already captured in full by
 * [FirebaseFileAntilog], so echoing them to the terminal only creates noise that
 * buries more critical output.
 *
 * WARNING and ERROR level Firebase messages are still shown so that genuine
 * problems surface immediately during development without requiring a log review.
 *
 * All other log categories are printed at every level as normal.
 */
class FilteredConsoleAntilog : Antilog() {

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        // Firebase debug/info already written to the log file — suppress from console.
        // WARNING and above still surface so critical Firebase errors are not hidden.
        if (tag == LoggingUtils.Category.FIREBASE.tag && priority < LogLevel.WARNING) return

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

