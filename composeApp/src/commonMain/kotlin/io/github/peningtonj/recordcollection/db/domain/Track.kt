package io.github.peningtonj.recordcollection.db.domain

data class Track(
    val id: String,
    val name: String,
    val artists: List<SimplifiedArtist>,
    val album: Album,
    val durationMs: Long,
    val imageUrl: String?
)
