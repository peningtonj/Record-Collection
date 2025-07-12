package io.github.peningtonj.recordcollection.ui.models

import io.github.peningtonj.recordcollection.db.Tracks
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.AlbumRating
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import io.github.peningtonj.recordcollection.db.domain.Track
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.milliseconds

data class AlbumDetailUiState(
    val album: Album,
    val tags: List<TagUiState>,
    val collections: List<AlbumCollectionUiState>,
    val tracks: List<Track>,
    val totalDuration: Long,
    val rating: AlbumRating? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class AlbumCollectionUiState(
    val collection: AlbumCollection,
    val position: Int,
    val addedAt: Instant,
    val isSelected: Boolean = false
)

data class TagUiState(
    val tag: Tag,
    val displayName: String = "${tag.key}: ${tag.value}",
    val isSelected: Boolean = false
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
