package io.github.peningtonj.recordcollection.db.domain

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
)
