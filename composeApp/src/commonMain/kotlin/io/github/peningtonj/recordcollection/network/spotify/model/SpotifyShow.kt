package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ShowDto(
    val id: String,
    val name: String,
    val uri: String,
    val href: String,
    val description: String,
    @SerialName("html_description")
    val htmlDescription: String,
    @SerialName("is_externally_hosted")
    val isExternallyHosted: Boolean?,
    val images: List<ImageDto>,
    val languages: List<String>,
    @SerialName("media_type")
    val mediaType: String,
    val publisher: String,
    @SerialName("total_episodes")
    val totalEpisodes: Int,
    val explicit: Boolean,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val type: String = "show"
)
