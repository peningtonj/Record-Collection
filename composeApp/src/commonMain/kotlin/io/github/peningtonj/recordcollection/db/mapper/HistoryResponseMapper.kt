package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyPlayHistoryResponse

object HistoryResponseMapper {
    fun toDomain(entity: SpotifyPlayHistoryResponse) : List<Track> {
        return entity.items.map { TrackMapper.toDomain(it.track) }
    }
}