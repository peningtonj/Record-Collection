package io.github.peningtonj.recordcollection.network.miscApi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MusicBrainzResponse(
    val created: String? = null,
    val count: Int? = null,
    val offset: Int? = null,
    val releases: List<Release>
)

@Serializable
data class Release(
    val id: String,
    val score: Int? = null,
    @SerialName("status-id") val statusId: String? = null,
    @SerialName("packaging-id") val packagingId: String? = null,
    @SerialName("artist-credit-id") val artistCreditId: String? = null,
    val count: Int? = null,
    val title: String,
    val status: String? = null,
    val disambiguation: String? = null,
    val packaging: String? = "",
    @SerialName("release-group") val releaseGroup: ReleaseGroup? = null,
    val date: String? = null,
    val country: String? = null,
    val barcode: String? = null,
    @SerialName("track-count") val trackCount: Int? = null,
)

@Serializable
data class ReleaseGroup(
    val id: String,
    @SerialName("type-id") val typeId: String,
    @SerialName("primary-type-id") val primaryTypeId: String,
    val title: String,
    @SerialName("primary-type") val primaryType: String
)

@Serializable
data class ReleaseGroupResponse(
    val id: String,
    val title: String,
    @SerialName("primary-type") val primaryType: String,
    val releases: List<ReleaseSummary>
)

@Serializable
data class ReleaseSummary(
    val id: String,
    val title: String,
    val status: String? = null,
    val date: String? = null
)
