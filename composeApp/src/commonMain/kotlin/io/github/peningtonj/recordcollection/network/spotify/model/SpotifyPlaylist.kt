package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyUserPlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val uri: String,
    val images: List<SpotifyImage> = emptyList(),
    val collaborative: Boolean,
    val externalUrls: Map<String, String>? = null,
    val followers: Followers? = null,
    val href: String,
    val owner: SpotifyProfileDto,
    val primaryColor: String? = null,
    val public: Boolean,
    val snapshotId: String? = null,
    val tracks: PlaylistTracks,
    val type: String,
    val totalTracks: Int? = null,
)

@Serializable
data class SpotifyPlaylistDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val uri: String,
    val images: List<SpotifyImage> = emptyList(),
    val collaborative: Boolean,
    val externalUrls: Map<String, String>,
    val href: String,
    val owner: SpotifyProfileDto,
    val public: Boolean,
    val snapshotId: String,
    val tracks: PaginatedResponse<TrackDto>,
    val type: String,
)

@Serializable
data class PlaylistItemWrapper(
    @SerialName("added_at")
    val addedAt: String? = null,
    @SerialName("added_by")
    val addedBy: AddedByDto? = null,
    @SerialName("is_local")
    val isLocal: Boolean = false,
    @SerialName("track")
    val item: PlaybackItem? = null
)

@Serializable
data class AddedByDto(
    val id: String? = null,
    val type: String,
    val uri: String
)

@Serializable
data class PlaylistTracks(
    val href: String,
    val total: Int
)