package io.github.peningtonj.recordcollection.db.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Track(
    val id: String,
    val name: String,
    val artists: List<SimplifiedArtist>,
    val album: Album? = null,
    val albumId: String,
    val isExplicit: Boolean,
    val trackNumber: Long,
    val discNumber: Long,
    val durationMs: Long,
    val imageUrl: String? = null,
    val spotifyUri: String,
    val isSaved: Boolean = false,
)

@Serializable
data class TrackDocument(
    val name: String,
    @SerialName("album_id") val albumId: String,
    @SerialName("track_number") val trackNumber: Long,
    @SerialName("duration_ms") val durationMs: Long,
    @SerialName("spotify_uri") val spotifyUri: String,
    val artists: String, // JSON-encoded List<SimplifiedArtistDto>
    @SerialName("preview_url") val previewUrl: String? = null,
    @SerialName("primary_artist") val primaryArtist: String,
    @SerialName("is_explicit") val isExplicit: Boolean,
    @SerialName("disc_number") val discNumber: Long,
    @SerialName("is_saved") val isSaved: Boolean = false
)
