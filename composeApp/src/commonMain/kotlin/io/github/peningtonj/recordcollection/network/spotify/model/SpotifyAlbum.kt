package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: String,
    val name: String,
    val albumType: AlbumType,
    val artists: List<Artist>,
    val totalTracks: Int,
    val releaseDate: String,
    val releaseDatePrecision: ReleaseDatePrecision,
    val uri: String,
    val externalUrls: Map<String, String>,
    val images: List<Image>,
    val popularity: Int?,
    val label: String?,
    val copyrights: List<Copyright>
)

@Serializable
enum class AlbumType {
    @SerialName("album") ALBUM,
    @SerialName("single") SINGLE,
    @SerialName("compilation") COMPILATION
}

@Serializable
enum class ReleaseDatePrecision {
    @SerialName("year") YEAR,
    @SerialName("month") MONTH,
    @SerialName("day") DAY
}
