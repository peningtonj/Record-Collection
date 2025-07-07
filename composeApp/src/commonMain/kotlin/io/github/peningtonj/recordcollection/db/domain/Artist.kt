package io.github.peningtonj.recordcollection.db.domain

data class Artist(
    val followers: Long,
    val genres: List<String>,
    val href: String,
    val id: String,
    val images: List<Image>,
    val name: String,
    val popularity: Long,
    val type: String,
    val uri: String
)
data class SimplifiedArtist(
    val id: String,
    val name: String,
    val uri: String,
    val externalUrls: Map<String, String>
)

