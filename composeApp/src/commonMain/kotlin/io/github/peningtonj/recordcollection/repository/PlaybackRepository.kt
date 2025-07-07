package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.mapper.PlaybackMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.ShuffleToggleRequest
import io.github.peningtonj.recordcollection.network.spotify.model.StartPlaybackRequest

class PlaybackRepository(
    private val spotifyApi: SpotifyApi
) {
    suspend fun startAlbumPlayback(album: Album) = spotifyApi.playback.startPlayback(
        StartPlaybackRequest(
            album.spotifyUri
        )
    )

    suspend fun getCurrentPlayback(): Playback? {
        return spotifyApi.playback.getPlaybackState()
            .getOrNull()
            ?.let { PlaybackMapper.toDomain(it) }
    }

    suspend fun turnOffShuffle() = spotifyApi.playback.toggleShuffle(
        ShuffleToggleRequest(
            state = false
        )
    )
}