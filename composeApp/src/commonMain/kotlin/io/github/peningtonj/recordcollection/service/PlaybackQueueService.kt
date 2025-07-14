import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Playback
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.flow.first

const val TRANSITION_TRIGGER_MS = 6000L // 6 seconds before track ends
const val NEXT_ALBUM_TRIGGER_MS = 1000L // 1 seconds before track ends

class PlaybackQueueService(
    private val playbackRepository: PlaybackRepository,
    private val albumRepository: AlbumRepository
) {
    data class QueueSession(
        val album: Album,
        val lastTrack: Track,
        val startedFromTrackIndex: Int = 0,
        val hasAddedTransitionTrack: Boolean = false,
        val queue: List<AlbumDetailUiState> = emptyList(),
        val transitionTrackUri: String = "spotify:track:6xXAl2w0mqyxsRB8ak2S7N"
    )

    suspend fun shouldAddTransitionTrack(session: QueueSession?, playback: Playback?): Boolean {
        val currentSession = session ?: return false
        if (currentSession.queue.isEmpty()) return false
        if (currentSession.hasAddedTransitionTrack) return false
        
        val currentTrack = playback?.track ?: return false
        if (currentTrack.id != currentSession.lastTrack.id) return false
        
        val progressMs = playback.progressMs ?: 0
        val remainingMs = currentTrack.durationMs - progressMs
        if (remainingMs > TRANSITION_TRIGGER_MS) return false
        
        return true
    }

    suspend fun shouldTransitionToNextAlbum(session: QueueSession?, playback: Playback?): Boolean {
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
            Result.failure(e)
        }
    }

    suspend fun ensureTracksLoaded(album: AlbumDetailUiState): AlbumDetailUiState {
        return if (album.tracks.isEmpty()) {
            albumRepository.fetchAndSaveTracks(album.album.id)
            val tracks = albumRepository.getTracksForAlbum(album.album.id).first()
            album.copy(tracks = tracks)
        } else {
            album
        }
    }
}