package io.github.peningtonj.recordcollection.util

import kotlinx.coroutines.CancellationException

/**
 * Like `runCatching`, but re-throws [CancellationException] so it never swallows
 * coroutine cancellation. Use this instead of `runCatching` in suspend code.
 */
inline fun <T> resultOf(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        Result.failure(t)
    }

/**
 * Collapses a list of [Result]s: success (with the unwrapped values) if every element
 * succeeded, otherwise a single [Result.failure] carrying an [AggregateException] with
 * every error. Nothing short-circuits — a partial failure still reports all the failures.
 */
fun <T> List<Result<T>>.aggregate(): Result<List<T>> {
    val failures = mapNotNull { it.exceptionOrNull() }
    return if (failures.isEmpty()) {
        Result.success(map { it.getOrThrow() })
    } else {
        Result.failure(AggregateException(failures))
    }
}

/** Multiple operations failed; [causes] holds each individual failure. */
class AggregateException(val causes: List<Throwable>) : Exception(
    "${causes.size} operation(s) failed: " +
        causes.joinToString("; ") { it.message ?: it::class.simpleName.orEmpty() },
    causes.firstOrNull(),
)
