package io.github.peningtonj.recordcollection.db.domain

import kotlinx.serialization.Serializable

@Serializable
data class Image(
    val url: String,
    val height: Int?,
    val width: Int?
)

