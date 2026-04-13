package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class Album(
    val id: String,
    val spotifyId: String,
    val name: String,
    val primaryArtist: String,
    val artists: List<SimplifiedArtist>,
    val releaseDate: LocalDate,
    val totalTracks: Int,
    val spotifyUri: String,
    val addedAt: Instant? = null,
    val albumType: AlbumType,
    val images: List<Image>,
    val updatedAt: Long? = null,
    val genres: List<String> = emptyList(),
    val externalIds: Map<String, String>? = emptyMap(),
    val inLibrary: Boolean = false,
    val releaseGroupId: String? = null
)

enum class AlbumType {
    ALBUM,
    SINGLE,
    COMPILATION,
    EP;

    companion object {
        fun fromString(value: String): AlbumType =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown album type: $value")
    }
}

/**
 * Firestore document model for the "albums" collection.
 * Document ID == internal album ID (hash of name + primary artist).
 * Complex fields (artists, images, externalIds) are stored as JSON strings
 * to avoid Firestore Int/Long type issues, consistent with ArtistDocument.
 */
@Serializable
data class AlbumDocument(
    val id: String = "",
    @SerialName("spotify_id") val spotifyId: String = "",
    val name: String = "",
    @SerialName("primary_artist") val primaryArtist: String = "",
    val artists: String = "[]",              // JSON-encoded List<SimplifiedArtist>
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("total_tracks") val totalTracks: Long = 0L,
    @SerialName("spotify_uri") val spotifyUri: String = "",
    @SerialName("added_at") val addedAt: String? = null,
    @SerialName("album_type") val albumType: String = "",
    val images: String = "[]",               // JSON-encoded List<Image>
    @SerialName("updated_at") val updatedAt: Long? = null,
    @SerialName("external_ids") val externalIds: String? = null, // JSON-encoded Map<String, String>
    @SerialName("in_library") val inLibrary: Boolean = false,
    @SerialName("release_group_id") val releaseGroupId: String? = null
)
