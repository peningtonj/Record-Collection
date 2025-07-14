package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class SimplifiedArtistDto(
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val id: String,
    val name: String,
    val type: String,
    val uri: String
)

@Serializable
data class FullArtistDto(
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val followers: Followers,
    val genres: List<String>,
    val href: String,
    val id: String,
    val images: List<ImageDto>,
    val name: String,
    val popularity: Int,
    val type: String,
    val uri: String
)

@Serializable
data class ArtistsResponse(
    val artists: List<FullArtistDto>
)

@Serializable
data class AristAlbumsRequest(
    val artistId: String,
    val limit: Int? = null,
    val offset: Int? = null,
    val market: String? = null,
    val includeGroups: List<String>? = listOf("album", "single")
)