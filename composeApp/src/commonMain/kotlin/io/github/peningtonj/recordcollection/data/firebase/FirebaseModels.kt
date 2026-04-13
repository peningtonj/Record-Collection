package io.github.peningtonj.recordcollection.data.firebase

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseAlbum(
    val id: String = "",
    val spotifyId: String = "",
    val name: String = "",
    val primaryArtist: String = "",
    val artists: String = "", // JSON encoded list
    val releaseDate: String = "",
    val totalTracks: Long = 0,
    val spotifyUri: String = "",
    val addedAt: String = "",
    val albumType: String = "",
    val images: String = "", // JSON encoded list
    val externalIds: String = "", // JSON encoded
    val inLibrary: Boolean = false,
    val releaseGroupId: String? = null,
    val updatedAt: Long = 0
)

@Serializable
data class FirebaseArtist(
    val id: String = "",
    val spotifyId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val genres: List<String> = emptyList(),
    val spotifyUri: String = "",
    val followers: Long = 0,
    val popularity: Int = 0,
    val addedAt: Long = 0
)

@Serializable
data class FirebaseTrack(
    val id: String = "",
    val spotifyId: String = "",
    val name: String = "",
    val albumId: String = "",
    val artists: String = "", // JSON encoded list
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val durationMs: Long = 0,
    val explicit: Boolean = false,
    val spotifyUri: String = "",
    val previewUrl: String? = null,
    val addedAt: Long = 0
)

@Serializable
data class FirebaseAlbumRating(
    val albumId: String = "",
    val rating: Int = 0,
    val notes: String? = null,
    val updatedAt: Long = 0
)

@Serializable
data class FirebaseCollection(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val albumIds: List<String> = emptyList() // Ordered list of album IDs
)

@Serializable
data class FirebaseTag(
    val id: String = "",
    val name: String = "",
    val color: String? = null,
    val createdAt: Long = 0
)

@Serializable
data class FirebaseAlbumTag(
    val albumId: String = "",
    val tagId: String = "",
    val addedAt: Long = 0
)

@Serializable
data class FirebaseProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val spotifyUserId: String? = null,
    val spotifyAccessToken: String? = null,
    val spotifyRefreshToken: String? = null,
    val spotifyTokenExpiry: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
