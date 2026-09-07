package io.github.peningtonj.recordcollection.util

/**
 * SHA-256 of [input] (encoded UTF-8), returned as a lowercase hex string (64 chars).
 *
 * `expect`/`actual` so it uses each platform's vetted implementation rather than a
 * hand-rolled one. Add a new `actual` when adding a non-JVM target.
 */
expect fun sha256Hex(input: String): String
