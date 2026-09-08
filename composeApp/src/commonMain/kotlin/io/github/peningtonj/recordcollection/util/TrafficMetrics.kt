package io.github.peningtonj.recordcollection.util

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Counts Firestore + Spotify traffic, attributed to the [TrafficSource] that caused it,
 * so we can see which screens / workers are expensive and whether we're being rate
 * limited — before and after the data-model migration (see docs/DATA_MODEL.md).
 *
 * Thread-safety: `record*` are fire-and-forget `Channel.trySend`s from any thread; a
 * single consumer coroutine (started by [start]) folds them into the maps and produces
 * the report, so there are no locks and no races. Events sent before [start] buffer.
 *
 * Counts are diagnostic, not billing — a dropped event under extreme backpressure is fine.
 */
object TrafficMetrics {

    // ── Public API (call from anywhere) ──────────────────────────────────────

    /** A Firestore read: [docs] documents returned (`.get()` / a snapshot emission). */
    fun recordFirestoreRead(collection: String, docs: Int, source: String = TrafficSource.current) {
        channel.trySend(Event.Firestore(source, collection, FsKind.READ, docs))
    }

    /** A realtime listener attaching — its first emission is a full read of the query. */
    fun recordFirestoreListen(collection: String, source: String = TrafficSource.current) {
        channel.trySend(Event.Firestore(source, collection, FsKind.LISTEN, 0))
    }

    fun recordFirestoreWrite(collection: String, source: String = TrafficSource.current) {
        channel.trySend(Event.Firestore(source, collection, FsKind.WRITE, 0))
    }

    fun recordFirestoreDelete(collection: String, source: String = TrafficSource.current) {
        channel.trySend(Event.Firestore(source, collection, FsKind.DELETE, 0))
    }

    /**
     * A Spotify Web API response.
     * @param endpoint  grouped path — use [groupSpotifyPath]
     * @param retryAfterSeconds  the `Retry-After` header on a 429, if any
     * @param rateLimitRemaining the `X-RateLimit-Remaining` header, if any
     */
    fun recordSpotify(
        endpoint: String,
        method: String,
        status: Int,
        retryAfterSeconds: Long? = null,
        rateLimitRemaining: Int? = null,
        source: String = TrafficSource.current,
    ) {
        channel.trySend(Event.Spotify(source, endpoint, method, status, retryAfterSeconds, rateLimitRemaining))
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    /** Start (or restart) the single consumer + periodic reporter. Call once from DI init. */
    fun start(scope: CoroutineScope, reportEverySeconds: Long = 120) {
        consumerJob?.cancel()
        reporterJob?.cancel()
        consumerJob = scope.launch {
            for (event in channel) handle(event)
        }
        reporterJob = if (reportEverySeconds > 0) scope.launch {
            while (isActive) {
                delay(reportEverySeconds * 1_000)
                channel.trySend(Event.Report)
            }
        } else null
    }

    /** Ask the consumer to log a report now (e.g. a debug action). */
    fun requestReport() { channel.trySend(Event.Report) }

    /** Ask the consumer to zero the counters (tests / a fresh measurement window). */
    fun requestReset() { channel.trySend(Event.Reset) }

    /** Formatted report, drained through the single consumer, so it's race-free. */
    suspend fun reportNow(): String {
        val into = CompletableDeferred<String>()
        channel.send(Event.ReportTo(into))
        return into.await()
    }

    // ── Grouping helper ──────────────────────────────────────────────────────

    /** Collapse a Spotify path so counts aggregate: `/albums/abc` → `/albums`, `/me/player/x` → `/me/player`. */
    fun groupSpotifyPath(path: String): String {
        val clean = path.substringBefore('?').trim('/')
        if (clean.isEmpty()) return "/"
        val parts = clean.split('/')
        return if (parts[0] == "me" && parts.size > 1) "/me/${parts[1]}" else "/${parts[0]}"
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private const val TAG = "Traffic"
    private val channel = Channel<Event>(Channel.UNLIMITED)
    private var consumerJob: Job? = null
    private var reporterJob: Job? = null
    private var windowStartedAt = now()

    private enum class FsKind { READ, LISTEN, WRITE, DELETE }

    private sealed interface Event {
        data object Report : Event
        data class ReportTo(val into: CompletableDeferred<String>) : Event
        data object Reset : Event
        data class Firestore(val source: String, val collection: String, val kind: FsKind, val docs: Int) : Event
        data class Spotify(
            val source: String, val endpoint: String, val method: String, val status: Int,
            val retryAfterSeconds: Long?, val rateLimitRemaining: Int?,
        ) : Event
    }

    private class FsCounters {
        var readDocs = 0L; var listens = 0L; var writes = 0L; var deletes = 0L
        val byCollection = mutableMapOf<String, Long>()
    }
    private class SpCounters {
        var requests = 0L; var rateLimited = 0L
        val byEndpoint = mutableMapOf<String, Long>()
    }

    private val fsBySource = mutableMapOf<String, FsCounters>()
    private val spBySource = mutableMapOf<String, SpCounters>()

    private var fsReadDocsTotal = 0L; private var fsListensTotal = 0L
    private var fsWritesTotal = 0L; private var fsDeletesTotal = 0L
    private var spReqTotal = 0L; private var sp4xx = 0L; private var sp5xx = 0L; private var sp429 = 0L
    private var spThrottledSeconds = 0L
    private var spMinRemaining: Int? = null

    private fun handle(e: Event) {
        when (e) {
            is Event.Report -> if (hasActivity()) Napier.i(buildReport(), tag = TAG)
            is Event.ReportTo -> e.into.complete(buildReport())
            is Event.Reset -> resetInternal()
            is Event.Firestore -> {
                val c = fsBySource.getOrPut(e.source) { FsCounters() }
                c.byCollection[e.collection] = (c.byCollection[e.collection] ?: 0) + 1
                when (e.kind) {
                    FsKind.READ -> { c.readDocs += e.docs; fsReadDocsTotal += e.docs }
                    FsKind.LISTEN -> { c.listens++; fsListensTotal++ }
                    FsKind.WRITE -> { c.writes++; fsWritesTotal++ }
                    FsKind.DELETE -> { c.deletes++; fsDeletesTotal++ }
                }
            }
            is Event.Spotify -> {
                val c = spBySource.getOrPut(e.source) { SpCounters() }
                c.requests++; spReqTotal++
                c.byEndpoint[e.endpoint] = (c.byEndpoint[e.endpoint] ?: 0) + 1
                when {
                    e.status == 429 -> { sp429++; c.rateLimited++ }
                    e.status in 500..599 -> sp5xx++
                    e.status in 400..499 -> sp4xx++
                }
                e.retryAfterSeconds?.let { spThrottledSeconds += it }
                e.rateLimitRemaining?.let { r -> spMinRemaining = spMinRemaining?.let { minOf(it, r) } ?: r }
            }
        }
    }

    private fun resetInternal() {
        fsBySource.clear(); spBySource.clear()
        fsReadDocsTotal = 0; fsListensTotal = 0; fsWritesTotal = 0; fsDeletesTotal = 0
        spReqTotal = 0; sp4xx = 0; sp5xx = 0; sp429 = 0; spThrottledSeconds = 0; spMinRemaining = null
        windowStartedAt = now()
    }

    private fun hasActivity() = fsReadDocsTotal + fsListensTotal + fsWritesTotal + spReqTotal > 0

    private fun buildReport(): String = buildString {
        val minutes = (now() - windowStartedAt) / 60_000
        appendLine("──── Traffic (window ≈ ${minutes}m) ────")
        appendLine("Firestore  readDocs=$fsReadDocsTotal  listeners=$fsListensTotal  writes=$fsWritesTotal  deletes=$fsDeletesTotal")
        fsBySource.entries.sortedByDescending { it.value.readDocs + it.value.writes }.forEach { (src, c) ->
            appendLine("  $src: readDocs=${c.readDocs} listen=${c.listens} write=${c.writes} delete=${c.deletes}   " +
                c.byCollection.entries.sortedByDescending { it.value }.joinToString(" ") { "${it.key}=${it.value}" })
        }
        appendLine("Spotify    requests=$spReqTotal  4xx=$sp4xx  5xx=$sp5xx  429=$sp429  throttled=${spThrottledSeconds}s  minRemaining=${spMinRemaining ?: "?"}")
        spBySource.entries.sortedByDescending { it.value.requests }.forEach { (src, c) ->
            appendLine("  $src: requests=${c.requests} 429=${c.rateLimited}   " +
                c.byEndpoint.entries.sortedByDescending { it.value }.joinToString(" ") { "${it.key}=${it.value}" })
        }
        append("────────────────────────────")
    }

    private fun now() = Clock.System.now().toEpochMilliseconds()
}
