package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Napier
import io.github.aakira.napier.Antilog

object Logger {
    fun initialize(isDebug: Boolean) {
        if (isDebug) {
            Napier.base(DebugAntilog())
        } else {
            Napier.base(ReleaseAntilog())
        }
    }

    private class DebugAntilog : Antilog() {
        override fun performLog(
            priority: io.github.aakira.napier.LogLevel,
            tag: String?,
            throwable: Throwable?,
            message: String?
        ) {
            val priorityChar = when (priority) {
                io.github.aakira.napier.LogLevel.VERBOSE -> 'V'
                io.github.aakira.napier.LogLevel.DEBUG -> 'D'
                io.github.aakira.napier.LogLevel.INFO -> 'I'
                io.github.aakira.napier.LogLevel.WARNING -> 'W'
                io.github.aakira.napier.LogLevel.ERROR -> 'E'
                io.github.aakira.napier.LogLevel.ASSERT -> 'A'
            }
            println("$priorityChar/$tag: $message")
            throwable?.printStackTrace()
        }
    }

    private class ReleaseAntilog : Antilog() {
        override fun performLog(
            priority: io.github.aakira.napier.LogLevel,
            tag: String?,
            throwable: Throwable?,
            message: String?
        ) {
            // In production, you might want to send critical logs to a service
        }
    }
}
