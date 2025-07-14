package io.github.peningtonj.recordcollection.db.domain

import kotlinx.datetime.Clock

data class Playback(
    val isPlaying: Boolean,
    val progressMs: Long?,
    val track: Track,
    val device: Device?,
    val shuffleState: Boolean,
    val repeatState: String,
    val lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
)
