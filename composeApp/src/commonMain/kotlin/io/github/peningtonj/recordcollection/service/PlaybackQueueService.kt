package io.github.peningtonj.recordcollection.service

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.flow.first

/** How long before the album's last track ends the transition SFX is queued. */
const val TRANSITION_TRIGGER_MS = 4000L

class PlaybackQueueService(
    private val playbackRepository: PlaybackRepository,
    private val trackRepository: TrackRepository,
) {
    data class QueueSession(
        val album: Album,
        val lastTrack: Track,
        val startedFromTrackIndex: Int = 0,
        val hasAddedTransitionTrack: Boolean = false,
        val queue: List<AlbumDetailUiState> = emptyList(),
        val transitionTrackUri: String = "spotify:track:6xXAl2w0mqyxsRB8ak2S7N",
        val playingFrom: AlbumCollection? = null,
        val isShuffled: Boolean,
        val playbackDifferenceCount: Int = 0,
    )

    /** True when [playback] is on the track [session] recorded as the album's last one. */
    fun isLastTrackInAlbum(session: QueueSession?, playback: Playback?): Boolean {
        val currentSession = session ?: return false
        if (currentSession.queue.isEmpty()) return false

        val currentTrack = playback?.track ?: return false
        return currentTrack.id == currentSession.lastTrack.id
    }

    suspend fun addTransitionTrack(session: QueueSession): Result<Unit> {
        return try {
            playbackRepository.addTrackToQueue(session.transitionTrackUri)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to add transition track", e)
            Result.failure(e)
        }
    }

    suspend fun ensureTracksLoaded(album: AlbumDetailUiState): AlbumDetailUiState {
        return if (album.tracks.isEmpty()) {
            album.copy(tracks = trackRepository.getAlbumTracks(album.album, checkSaved = false))
        } else {
            album
        }
    }
}