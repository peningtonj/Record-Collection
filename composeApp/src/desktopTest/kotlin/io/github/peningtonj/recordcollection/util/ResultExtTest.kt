package io.github.peningtonj.recordcollection.util

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ResultExtTest {

    @Test
    fun `resultOf wraps success and failure`() {
        assertEquals(3, resultOf { 1 + 2 }.getOrNull())

        val boom = IllegalStateException("boom")
        assertEquals(boom, resultOf { throw boom }.exceptionOrNull())
    }

    @Test
    fun `resultOf re-throws CancellationException`() {
        assertFailsWith<CancellationException> {
            resultOf { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `aggregate is success when every result succeeded`() {
        val result = listOf(Result.success(1), Result.success(2), Result.success(3)).aggregate()
        assertEquals(listOf(1, 2, 3), result.getOrNull())
    }

    @Test
    fun `aggregate reports every failure, not just the first`() {
        val results = listOf(
            Result.success(1),
            Result.failure<Int>(RuntimeException("a")),
            Result.failure<Int>(RuntimeException("b")),
        )

        val error = results.aggregate().exceptionOrNull()

        assertIs<AggregateException>(error)
        assertEquals(2, error.causes.size)
        assertTrue(error.message!!.contains("a") && error.message!!.contains("b"))
    }

    @Test
    fun `aggregate of empty list is success`() {
        assertEquals(emptyList(), emptyList<Result<Int>>().aggregate().getOrNull())
    }
}
