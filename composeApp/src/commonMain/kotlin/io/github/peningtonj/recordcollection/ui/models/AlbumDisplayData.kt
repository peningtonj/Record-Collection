package io.github.peningtonj.recordcollection.ui.models

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbum
import kotlin.time.Duration.Companion.milliseconds

data class AlbumDisplayData(
    val album: Album,
    val totalDuration: Long,
    val rating: Int
)

data class CollectionAlbumDisplayData(
    val album: CollectionAlbum,
    val rating: Int
)

