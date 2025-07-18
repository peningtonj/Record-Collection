import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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
                val artistFlow = artistRepository.getArtistById(artistId)
                val albumsFromApi = artistRepository.fetchAlbumArtists(artistId)
                val savedAlbumsFlow = albumRepository.getAlbumsByArtist(artistId)

                // Combine the flows and the API result
                combine(
                    artistFlow,
                    savedAlbumsFlow
                ) { artist, savedAlbums ->
                    Triple(artist, albumsFromApi, savedAlbums)
                }
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { (artist, apiAlbums, savedAlbums) ->
                    // Convert albums to AlbumDetailUiState or merge them as needed

                    val albumDetailStates = apiAlbums.map { album ->
                        // Create a basic AlbumDetailUiState from the API album
                        AlbumDetailUiState(
                            album = album,
                            tags = emptyList(),
                            collections = emptyList(),
                            tracks = emptyList(),
                            totalDuration = 0L,
                            rating = null,
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