package io.github.peningtonj.recordcollection.db.domain.filter

data class AlbumFilter(
    val releaseDateRange: DateRange? = null,
    val tags: MutableMap<String, List<String>> = mutableMapOf(),
    val searchQuery: String = ""
)