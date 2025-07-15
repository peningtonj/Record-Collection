package io.github.peningtonj.recordcollection.db.domain


data class SearchResult(
    val tracks: List<Track>?,
    val albums: List<Album>?,
    val artists: List<Artist>?
)
