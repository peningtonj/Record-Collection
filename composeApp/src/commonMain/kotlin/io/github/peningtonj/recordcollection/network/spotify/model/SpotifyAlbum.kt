package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlbumDto(
    val id: String,
    val name: String,
    @SerialName("album_type")
    val albumType: AlbumTypeDto,
    val artists: List<SimplifiedArtistDto>,
    @SerialName("total_tracks")
    val totalTracks: Int,
    @SerialName("release_date")
    val releaseDate: String,
    @SerialName("release_date_precision")
    val releaseDatePrecision: ReleaseDatePrecision,
    val uri: String,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    @SerialName("external_ids")
    val externalIds: Map<String, String>,
    val images: List<ImageDto>,
    val popularity: Int?,
    val label: String?,
    val copyrights: List<Copyright>,
    @SerialName("available_markets")
    val availableMarkets: List<String>,
    val href: String,
    val restrictions: Restrictions? = null,
    val type: String,
    val genres: List<String>,
    val tracks: TracksDto

)

@Serializable
enum class AlbumTypeDto {
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


@Serializable
data class AlbumsResponse(
    val albums: List<AlbumDto>
)

@Serializable
data class SavedAlbumDto(
    @SerialName("added_at")
    val addedAt: String, // ISO 8601 timestamp
    val album: AlbumDto
)