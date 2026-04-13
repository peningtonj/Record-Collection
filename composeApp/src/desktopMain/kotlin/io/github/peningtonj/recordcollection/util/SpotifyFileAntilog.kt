package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A Napier Antilog that writes all messages tagged "Spotify" to a dedicated
 * log file (appended across runs).
 *
 * The file is created at [logDir]/spotify_queries.log.
 * Each line format: "2026-04-13 12:34:56.789 D/Spotify: → GET /albums/abc123"
 */
class SpotifyFileAntilog(logDir: String = "logs") : Antilog() {

    private val writer: PrintWriter
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    init {
        val dir = File(logDir)
        dir.mkdirs()
        val logFile = File(dir, "spotify_queries.log")
        writer = PrintWriter(FileWriter(logFile, true), true)
        writer.println("=== Session started at ${LocalDateTime.now().format(timestampFormatter)} ===")
    }

    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?
    ) {
        if (tag != LoggingUtils.Category.SPOTIFY.tag) return

        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val level = priority.name.first()
        val line = "$timestamp $level/Spotify: $message"

        synchronized(writer) {
            writer.println(line)
            throwable?.let { writer.println(it.stackTraceToString()) }
        }
    }
}

