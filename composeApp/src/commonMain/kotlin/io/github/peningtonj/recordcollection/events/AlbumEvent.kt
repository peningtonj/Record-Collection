package io.github.peningtonj.recordcollection.events

import io.github.peningtonj.recordcollection.db.domain.Album

sealed class AlbumEvent {
    data class AlbumAdded(val album: Album, val replaceArtist : Boolean = false) : AlbumEvent()
    data class AlbumUpdated(val album: Album) : AlbumEvent()
    data class AlbumDeleted(val albumId: String) : AlbumEvent()
}
