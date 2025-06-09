package io.github.peningtonj.recordcollection.network.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifyProfile(
    val country: String? = null,
    @SerialName("display_name")
    val displayName: String? = null,
    val email: String? = null,
    @SerialName("explicit_content")
    val explicitContent: ExplicitContent? = null,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val followers: Followers,
    val href: String,
    val id: String,
    val images: List<Image>,
    val product: String? = null,
    val type: String,
    val uri: String
)

@Serializable
data class ExplicitContent(
    @SerialName("filter_enabled")
    val filterEnabled: Boolean,
    @SerialName("filter_locked")
    val filterLocked: Boolean
)

@Serializable
data class Followers(
    val href: String? = null,
    val total: Int
)

@Serializable
data class Image(
    val url: String,
    val height: Int? = null,
    val width: Int? = null
)