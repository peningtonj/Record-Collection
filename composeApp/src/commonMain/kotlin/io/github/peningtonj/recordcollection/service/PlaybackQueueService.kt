import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.flow.first

const val TRANSITION_TRIGGER_MS = 4000L // 4 seconds before track ends
const val NEXT_ALBUM_TRIGGER_MS = 500L // 0.5 seconds before track ends

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

    fun isLastTrackInAlbum(session: QueueSession?, playback: Playback?): Boolean {
        val currentSession = session ?: return false
        if (currentSession.queue.isEmpty()) return false

        val currentTrack = playback?.track ?: return false
        return currentTrack.id == currentSession.lastTrack.id
    }
    fun albumEnding(session: QueueSession?, playback: Playback?, transitionTriggerTime: Long = TRANSITION_TRIGGER_MS): Boolean {
        val currentSession = session ?: return false

        if (!isLastTrackInAlbum(currentSession, playback)) return false
        if (currentSession.hasAddedTransitionTrack) return false

        val currentTrack = playback?.track ?: return false

        val progressMs = playback.progressMs ?: 0
        val remainingMs = currentTrack.durationMs - progressMs
//        Napier.d("Getting close to adding transition track | remainingMs: $remainingMs, transitionTriggerTime: $transitionTriggerTime")
        return remainingMs <= transitionTriggerTime
    }

     fun shouldTransitionToNextAlbum(session: QueueSession?, playback: Playback?): Boolean {
        val currentSession = session ?: return false
        if (!currentSession.hasAddedTransitionTrack) return false
        
        val currentTrack = playback?.track ?: return false
        if (currentTrack.spotifyUri != currentSession.transitionTrackUri) return false
        
        val progressMs = playback.progressMs ?: 0
        val remainingMs = currentTrack.durationMs - progressMs
        return remainingMs <= NEXT_ALBUM_TRIGGER_MS
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
            trackRepository.fetchAndSaveTracks(album.album.id)
            val tracks = trackRepository.getTracksForAlbum(album.album.id).first()
            album.copy(tracks = tracks)
        } else {
            album
        }
    }
}