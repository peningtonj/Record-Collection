package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
@Serializable
data class TracksDto(
    val href: String,
    val items: List<SimplifiedTrackDto>,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

@Serializable
data class SimplifiedTrackDto(
    val artists: List<SimplifiedArtistDto>,
    @SerialName("available_markets")
    val availableMarkets: List<String>?,
    @SerialName("disc_number")
    val discNumber: Int,
    @SerialName("duration_ms")
    val durationMs: Int,
    val explicit: Boolean,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val id: String,
    val name: String,
    @SerialName("preview_url")
    val previewUrl: String?,
    @SerialName("track_number")
    val trackNumber: Int,
    val type: String,
    val uri: String,
    @SerialName("is_local")
    val isLocal: Boolean,
    @SerialName("linked_from")
    val linkedFrom: LinkedTrackDto? = null,
    val restrictions: Restrictions? = null,
    )

@Serializable
data class TrackDto(
    val album: AlbumDto?,
    val artists: List<SimplifiedArtistDto>,
    @SerialName("available_markets")
    val availableMarkets: List<String>?,
    @SerialName("disc_number")
    val discNumber: Int,
    @SerialName("duration_ms")
    val durationMs: Int,
    val explicit: Boolean,
    @SerialName("external_ids")
    val externalIds: Map<String, String>,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val id: String,
    @SerialName("is_playable")
    val isPlayable: Boolean?,
    @SerialName("linked_from")
    val linkedFrom: LinkedTrackDto?,
    val restrictions: Restrictions?,
    val name: String,
    val popularity: Int,
    @SerialName("preview_url")
    val previewUrl: String?,
    @SerialName("track_number")
    val trackNumber: Int,
    val type: String,
    val uri: String,
    @SerialName("is_local")
    val isLocal: Boolean
)

@Serializable
data class LinkedTrackDto(
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val id: String,
    val type: String,
    val uri: String
)


@Serializable
data class Restrictions(
    val reason: String
)