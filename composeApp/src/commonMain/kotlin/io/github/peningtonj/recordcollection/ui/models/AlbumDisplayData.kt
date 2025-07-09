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


fun formattedTotalDuration(totalDuration: Long): String {
    val duration = totalDuration.milliseconds
    val totalMinutes = duration.inWholeMinutes
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val seconds = (duration.inWholeSeconds % 60)

    return if (hours > 0) {
        "$hours h $minutes min"
    } else {
        "$minutes min $seconds sec"
    }
}
