package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackTrack

object TrackMapper {
    fun toDomain(entity: PlaybackTrack) : Track {
        return Track(
            id = entity.id,
            name = entity.name,
            artists = entity.artists.map { ArtistMapper.toDomain(it) },
            album = AlbumMapper.toDomain(entity.album),
            durationMs = entity.durationMs.toLong(),
            imageUrl = null
        )
    }

}