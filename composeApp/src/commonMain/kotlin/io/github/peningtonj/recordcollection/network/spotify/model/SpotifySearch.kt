package io.github.peningtonj.recordcollection.network.spotify.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class SpotifySearchRequest(
    val q: String,
    val type: List<SearchType>? = null,
    val market: String? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val includeExternal: String? = null
)

@Serializable
enum class SearchType {
    @SerialName("album") ALBUM,
    @SerialName("artist") ARTIST,
    @SerialName("playlist") PLAYLIST,
    @SerialName("track") TRACK,
    @SerialName("show") SHOW,
    @SerialName("episode") EPISODE,
    @SerialName("user") USER,
}


@Serializable
data class SpotifySearchResult(
    val tracks: PaginatedResponse<TrackDto>? = null,
    val albums: PaginatedResponse<SimplifiedAlbumDto>? = null,
    val artists: PaginatedResponse<FullArtistDto>? = null,
)