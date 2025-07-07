package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Device
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackDto
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackTrack

object PlaybackMapper {
    fun toDomain(entity: PlaybackDto): Playback? {
        return when (entity.currentlyPlayingType) {
            "track" -> {
                val track = entity.item as PlaybackTrack
                    Playback(
                        isPlaying = entity.isPlaying,
                        progressMs = entity.progressMs ?: 0,
                        track = TrackMapper.toDomain(track),
                        shuffleState = entity.shuffleState,
                        repeatState = entity.repeatState,
                        device = Device(
                            entity.device.id
                        ),
                    )
            }
            else -> null
        }
    }
}
