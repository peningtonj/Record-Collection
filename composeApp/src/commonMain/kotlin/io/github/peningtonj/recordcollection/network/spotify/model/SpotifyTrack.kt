package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val album: Album,
    val artists: List<Artist>,
    @SerialName("available_markets")
    val availableMarkets: List<String>,
    @SerialName("disc_number")
    val discNumber: Int,
    @SerialName("duration_ms")
    val durationMs: Long,
    val explicit: Boolean,
    @SerialName("external_ids")
    val externalIds: Map<String, String>,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val id: String,
    @SerialName("is_playable")
    val isPlayable: Boolean? = null,
    @SerialName("linked_from")
    val linkedFrom: TrackLink? = null,
    val restrictions: Restrictions? = null,
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
data class TrackLink(
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