package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A Napier Antilog that writes all messages tagged "Firebase" to a dedicated
 * log file, in addition to the normal console output.
 *
 * The file is created at [logDir]/firebase_queries.log (appended across runs).
 */
class FirebaseFileAntilog(logDir: String = "logs") : Antilog() {

    private val writer: PrintWriter
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    init {
        val dir = File(logDir)
        dir.mkdirs()
        val logFile = File(dir, "firebase_queries.log")
        // append = true so logs persist across restarts; autoFlush = true for immediate writes
        writer = PrintWriter(FileWriter(logFile, true), true)
        writer.println("=== Session started at ${LocalDateTime.now().format(timestampFormatter)} ===")
    }

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        if (tag != LoggingUtils.Category.FIREBASE.tag) return

        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val level = priority.name.first()
        val line = "$timestamp $level/Firebase: $message"

        synchronized(writer) {
            writer.println(line)
            throwable?.let { writer.println(it.stackTraceToString()) }
        }
    }
}

