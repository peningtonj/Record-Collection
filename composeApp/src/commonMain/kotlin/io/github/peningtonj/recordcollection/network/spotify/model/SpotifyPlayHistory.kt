package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyPlayHistoryRequest(
    val limit: Int? = null, // Maximum number of items to return (1-50, default 20)
    val after: Long? = null, // Unix timestamp in milliseconds. Returns all items after this cursor position
    val before: Long? = null // Unix timestamp in milliseconds. Returns all items before this cursor position
)

@Serializable
data class SpotifyPlayHistoryResponse(
    val items: List<PlayHistoryItemDto>,
    val next: String? = null,
    val cursors: CursorsDto,
    val limit: Int,
    val href: String
)

@Serializable
data class PlayHistoryItemDto(
    val track: PlaybackTrack, // Reusing your existing PlaybackTrack model
    @SerialName("played_at")
    val playedAt: String, // ISO 8601 UTC timestamp
    val context: ContextDto? = null // Context where the track was played from
)

@Serializable
data class CursorsDto(
    val after: String? = null,
    val before: String? = null
)