package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchResult

object SearchResultMapper {
    fun toDomain(result: SpotifySearchResult) : SearchResult {
        return SearchResult(
            tracks = result.tracks?.items?.map {
                TrackMapper.toDomain(it)
            },
            artists = result.artists?.items?.map {
                ArtistMapper.toDomain(it)
            },
            albums = result.albums?.items?.map {
                AlbumMapper.toDomain(it)
            },
        )
    }
}