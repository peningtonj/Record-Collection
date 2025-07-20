package io.github.peningtonj.recordcollection.repository

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.mapper.TrackMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackTrack
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems

class PlaylistRepository(
    private val spotifyApi: SpotifyApi
) {
    suspend fun getPlaylistTracks(playlistId: String): List<Track> {
        val playlistResult = spotifyApi.library.getPlaylistTracks(playlistId)

        return playlistResult.getOrNull()
            ?.getAllItems { nextUrl ->
                spotifyApi.library.getNextPaginated(nextUrl)
            }
            ?.getOrNull()
            ?.mapNotNull { wrapper ->
                val item = wrapper.item
                if (item is PlaybackTrack) {
                    TrackMapper.toDomain(item)
                } else {
                    null // ignore episodes or null items
                }
            }
            ?: emptyList()
    }
}
