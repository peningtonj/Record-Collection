package io.github.peningtonj.recordcollection.events


import io.github.peningtonj.recordcollection.events.handlers.AlbumEventHandler
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AlbumEventDispatcher(
    private val handlers: List<AlbumEventHandler>,
    private val scope: CoroutineScope
) {

    fun dispatch(event: AlbumEvent) {
        scope.launch {
            handlers.forEach { handler ->
                try {
                    handler.handle(event)
                } catch (e: Exception) {
                    Napier.e("Failed to handle album event: $event", e)
                }
            }
        }
    }

    suspend fun dispatchAndWait(event: AlbumEvent) {
        handlers.forEach { handler ->
            try {
                handler.handle(event)
            } catch (e: Exception) {
                Napier.e("Failed to handle album event: $event", e)
            }
        }
    }
}
