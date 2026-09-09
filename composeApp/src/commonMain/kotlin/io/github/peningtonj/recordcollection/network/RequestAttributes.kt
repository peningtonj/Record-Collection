package io.github.peningtonj.recordcollection.network

import io.ktor.util.AttributeKey

/**
 * Marks a request that must not be retried by [HttpRequestRetry] (the playback poller —
 * a stale poll is worthless, and retrying it just wastes rate-limit budget).
 *
 * A Ktor attribute rather than a header: a custom request header triggers a CORS
 * preflight in the browser, and Spotify's `/me/player` doesn't answer the preflight,
 * so `X-No-Retry` used to make every poll fail with "Fail to fetch" on web.
 */
val NoRetryAttribute: AttributeKey<Boolean> = AttributeKey("RecordCollection-NoRetry")
