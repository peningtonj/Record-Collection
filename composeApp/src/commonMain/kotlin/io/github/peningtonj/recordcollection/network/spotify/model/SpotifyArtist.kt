package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,

    val followers: Followers,

    val genres: List<String>,

    val href: String,

    val id: String,

    val images: List<Image>,

    val name: String,

    val popularity: Int,

    val type: String,

    val uri: String
)
