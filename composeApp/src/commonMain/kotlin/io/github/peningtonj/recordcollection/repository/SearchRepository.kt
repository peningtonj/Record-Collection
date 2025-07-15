package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.db.mapper.SearchResultMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.SearchType
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchRequest
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifySearchResult

class SearchRepository(
    private val spotifyApi: SpotifyApi,
    ) {

    suspend fun searchSpotify(
        query: String,
        type: List<SearchType> = listOf(SearchType.ARTIST, SearchType.ALBUM, SearchType.PLAYLIST, SearchType.TRACK),
        limit: Int = 50,
        offset: Int = 0,
        market: String? = null,
        includeExternal: String? = null,
    ) : SearchResult =
        SearchResultMapper.toDomain(
        spotifyApi.search.searchSpotify(
            SpotifySearchRequest(
                q = query,
                type = type,
                limit = limit,
                offset = offset,
                market = market,
                includeExternal = includeExternal
            )
        )
    )
}