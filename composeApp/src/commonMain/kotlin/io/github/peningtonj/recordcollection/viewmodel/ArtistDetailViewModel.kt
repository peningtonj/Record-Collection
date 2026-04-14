import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArtistDetailViewModel(
    private val artistRepository: ArtistRepository,
    private val albumRepository: AlbumRepository,
    private val artistId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    data class ArtistDetailUiState(
        val artist: Artist? = null,
        val albums: List<AlbumDetailUiState> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    init {
        loadArtistDetails()
    }

    private fun loadArtistDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Fetch API albums once up-front (one-shot)
                val albumsFromApi = artistRepository.fetchAlbumArtists(artistId)

                // Look up saved state by internal hash IDs directly — this is more reliable
                // than querying by artist name, which requires the artist document to exist
                // in Firestore and names to match exactly.
                val albumIds = albumsFromApi.map { it.id }

                combine(
                    artistRepository.getArtistById(artistId),
                    albumRepository.getAlbumsByIds(albumIds)
                ) { artist, savedAlbums -> Pair(artist, savedAlbums) }
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                    .collect { (artist, savedAlbums) ->
                        val savedAlbumMap = savedAlbums.associateBy { it.id }

                        val albumDetailStates = albumsFromApi.map { album ->
                            val saved = savedAlbumMap[album.id]
                            AlbumDetailUiState(
                                album = album.copy(
                                    inLibrary = saved?.inLibrary ?: false,
                                    rating = saved?.rating ?: album.rating
                                ),
                                tags = emptyList(),
                                collections = emptyList(),
                                tracks = emptyList(),
                                totalDuration = 0L,
                                rating = saved?.rating,
                                isLoading = false,
                                error = null,
                                releaseGroup = emptyList()
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            artist = artist,
                            albums = albumDetailStates,
                            error = null
                        )
                    }
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
        }
    }
}