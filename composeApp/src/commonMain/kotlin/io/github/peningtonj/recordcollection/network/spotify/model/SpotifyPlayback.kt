package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


@Serializable
data class PlaybackDto(
    val device: DeviceDto,
    val context: ContextDto? = null,
    val timestamp: Long,
    @SerialName("progress_ms")
    val progressMs: Long? = 0,
    @SerialName("is_playing")
    val isPlaying: Boolean,
    @SerialName("currently_playing_type")
    val currentlyPlayingType: String,
    val item: PlaybackItem? = null, // This now handles both tracks and episodes automatically
    @SerialName("shuffle_state")
    val shuffleState: Boolean,
    @SerialName("smart_shuffle")
    val smartShuffleState: Boolean,
    @SerialName("repeat_state")
    val repeatState: String,
    val actions: ActionsDto
)

@Serializable(with = PlaybackItemSerializer::class)
sealed class PlaybackItem {
    abstract val id: String
    abstract val name: String
    abstract val uri: String
    abstract val durationMs: Int
}

@Serializable
data class PlaybackTrack(
    override val id: String,
    override val name: String,
    override val uri: String,
    @SerialName("duration_ms")
    override val durationMs: Int,
    val artists: List<SimplifiedArtistDto>,
    val album: AlbumDto,
    val explicit: Boolean,
    val popularity: Int,
    @SerialName("preview_url")
    val previewUrl: String? = null,
    @SerialName("track_number")
    val trackNumber: Int
) : PlaybackItem()

@Serializable
data class PlaybackEpisode(
    override val id: String,
    override val name: String,
    override val uri: String,
    @SerialName("duration_ms")
    override val durationMs: Int,
    val description: String,
    val explicit: Boolean,
    val show: ShowDto,
    @SerialName("release_date")
    val releaseDate: String
) : PlaybackItem()

object PlaybackItemSerializer : JsonContentPolymorphicSerializer<PlaybackItem>(PlaybackItem::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PlaybackItem> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "track" -> PlaybackTrack.serializer()
            "episode" -> PlaybackEpisode.serializer()
            else -> throw SerializationException("Unknown playback item type")
        }
    }
}

@Serializable
data class DeviceDto(
    val id: String? = null,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("is_private_session")
    val isPrivateSession: Boolean,
    @SerialName("is_restricted")
    val isRestricted: Boolean,
    val name: String,
    val type: String,
    @SerialName("volume_percent")
    val volumePercent: Int? = 100,
    @SerialName("supports_volume")
    val supportsVolume: Boolean
)

@Serializable
data class ActionsDto(
    @SerialName("interrupting_playback")
    val interruptingPlayback: Boolean? = null,
    val pausing: Boolean? = null,
    val resuming: Boolean? = null,
    val seeking: Boolean? = null,
    @SerialName("skipping_next")
    val skippingNext: Boolean? = null,
    @SerialName("skipping_prev")
    val skippingPrev: Boolean? = null,
    @SerialName("toggling_repeat_context")
    val togglingRepeatContext: Boolean? = null,
    @SerialName("toggling_shuffle")
    val togglingShuffle: Boolean? = null,
    @SerialName("toggling_repeat_track")
    val togglingRepeatTrack: Boolean? = null,
    @SerialName("transferring_playback")
    val transferringPlayback: Boolean? = null
)

@Serializable
data class StartPlaybackRequest(
    @SerialName("context_uri")
    val contextUri: String? = null,
    val uris: List<String>? = null,
    val offset: PlaybackOffset? = null,
    @SerialName("position_ms")
    val positionMs: Int? = null
)

data class ShufflePlaybackRequest(
    val state: Boolean,
    val deviceId: String? = null
)

data class DevicePlaybackRequest(
    val deviceId: String? = null
)

@Serializable
data class PlaybackOffset(
    val position: Int? = null,
    val uri: String? = null
)

@Serializable
data class AddToQueueRequest(
    val uri: String,
    @SerialName("device_id")
    val deviceId: String? = null,
)
