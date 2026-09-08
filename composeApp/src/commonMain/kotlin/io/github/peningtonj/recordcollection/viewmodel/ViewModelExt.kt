package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Launches [block] on [viewModelScope] with a uniform failure policy: coroutine
 * cancellation propagates, anything else is logged (tagged with [operation]) and passed
 * to [onError] — it never escapes to crash the app.
 *
 * Use this for fire-and-forget commands. When the failure needs to be *shown*, pass an
 * [onError] that writes to a UI-state flow (or handle it inline with a `try`/`Result`).
 */
fun ViewModel.launchSafely(
    operation: String,
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job = viewModelScope.launch {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Napier.e("$operation failed", e)
        onError(e)
    }
}
