package io.github.peningtonj.recordcollection.db.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Artist(
    val followers: Long,
    val genres: List<String>,
    val href: String,
    val id: String,
    val images: List<Image>,
    val name: String,
    val popularity: Long,
    val type: String,
    val uri: String
)

@Serializable
data class SimplifiedArtist(
    val id: String,
    val name: String,
    val uri: String,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val type: String,
    )

