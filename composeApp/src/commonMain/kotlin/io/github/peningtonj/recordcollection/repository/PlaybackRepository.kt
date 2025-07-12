package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.mapper.PlaybackMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.DevicePlaybackRequest
import io.github.peningtonj.recordcollection.network.spotify.model.PlaybackOffset
import io.github.peningtonj.recordcollection.network.spotify.model.ShufflePlaybackRequest
import io.github.peningtonj.recordcollection.network.spotify.model.StartPlaybackRequest

class PlaybackRepository(
    private val spotifyApi: SpotifyApi
) {
    suspend fun startAlbumPlayback(album: Album) = spotifyApi.playback.startPlayback(
        StartPlaybackRequest(
            album.spotifyUri
        )
    )

    suspend fun startAlbumPlaybackFromTrack(album: Album, track: Track) = spotifyApi.playback.startPlayback(
        StartPlaybackRequest(
            album.spotifyUri,
            offset = PlaybackOffset(uri = track.spotifyUri)
        )
    )

    suspend fun getCurrentPlayback(): Playback? {
        return spotifyApi.playback.getPlaybackState()
            .getOrNull()
            ?.let { PlaybackMapper.toDomain(it) }
    }

    suspend fun turnOffShuffle() = spotifyApi.playback.toggleShuffle(
        ShufflePlaybackRequest(
            state = false
        )
    )

    suspend fun turnOnShuffle() = spotifyApi.playback.toggleShuffle(
        ShufflePlaybackRequest(
            state = true
        )
    )

    suspend fun resumePlayback() = spotifyApi.playback.startPlayback()
    suspend fun pausePlayback(
        deviceId: String? = null,
    ) = spotifyApi.playback.pausePlayback(
        DevicePlaybackRequest(deviceId)
    )
    suspend fun skipToNext(
        deviceId: String? = null,
    ) = spotifyApi.playback.skipToNextTrack(
        DevicePlaybackRequest(deviceId)
    )
    suspend fun skipToPrevious(
        deviceId: String? = null,
    ) = spotifyApi.playback.skipToPreviousTrack(
        DevicePlaybackRequest(deviceId)
    )
    suspend fun seekToPosition(positionMs: Long) = spotifyApi.playback.seekToPosition(positionMs)
}