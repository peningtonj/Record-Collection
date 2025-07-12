package io.github.peningtonj.recordcollection.events.handlers

import io.github.peningtonj.recordcollection.events.AlbumEvent

interface AlbumEventHandler {
    suspend fun handle(event: AlbumEvent)
}
