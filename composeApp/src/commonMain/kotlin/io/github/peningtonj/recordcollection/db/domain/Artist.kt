package io.github.peningtonj.recordcollection.db.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Firestore document for the "artists" collection. Document ID == artist ID.
 * Images stored as a JSON string to avoid Int/Long Firestore type issues.
 */
@Serializable
data class ArtistDocument(
    val id: String = "",
    val followers: Long = 0L,
    val genres: List<String> = emptyList(),
    val href: String = "",
    val images: String = "[]", // JSON-encoded List<Image>
    val name: String = "",
    val popularity: Long = 0L,
    val type: String = "",
    val uri: String = ""
)

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

@Serializable
data class SimplifiedArtist(
    val id: String,
    val name: String,
    val uri: String,
    @SerialName("external_urls")
    val externalUrls: Map<String, String>,
    val href: String,
    val type: String,
    )

