package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

data class Album(
    val id: String,
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
    val genres: List<String> = emptyList()
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
