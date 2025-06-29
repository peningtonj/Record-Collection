package io.github.peningtonj.recordcollection.network.spotify.model
import kotlinx.serialization.Serializable

@Serializable
data class Copyright(
    val text: String,
    val type: String,
)
