package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.UserLibraryRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ArtistDetailViewModel(
    private val artistRepository: ArtistRepository,
    private val userLibraryRepository: UserLibraryRepository,
    private val artistId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistDetailUiState())
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    override fun onCleared() {
        Napier.d("ArtistDetailViewModel($artistId) cleared")
        super.onCleared()
    }

    data class ArtistDetailUiState(
        val artist: Artist? = null,
        val albums: List<AlbumDetailUiState> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    init {
        loadArtistDetails()
    }

    private fun loadArtistDetails() = launchSafely(
        operation = "loadArtistDetails($artistId)",
        onError = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) },
    ) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Fetch API albums once up-front (one-shot)
                val albumsFromApi = artistRepository.fetchAlbumArtists(artistId)

                // Library membership/rating come from the per-user library_albums
                // collection, not the shared albums catalogue (whose inLibrary/rating
                // fields are always unset — see UserLibraryRepository doc comment).
                // Matched by Spotify ID, not the internal hash ID: library_albums docs
                // predating the hash-ID migration (TECH_DEBT 2.2) are still keyed by an
                // older scheme, so albumsFromApi's freshly-hashed ids wouldn't match them
                // — the Spotify ID is stable across every scheme and always present.
                val spotifyIds = albumsFromApi.map { it.spotifyId }.toSet()

                combine(
                    artistRepository.getArtistById(artistId),
                    userLibraryRepository.getAllLibraryEntries()
                ) { artist, libraryEntries -> Pair(artist, libraryEntries) }
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message
                        )
                    }
                    .collect { (artist, libraryEntries) ->
                        val savedAlbumMap = libraryEntries
                            .filter { it.spotifyId in spotifyIds }
                            .associateBy { it.spotifyId }

                        val albumDetailStates = albumsFromApi.map { album ->
                            val saved = savedAlbumMap[album.spotifyId]
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
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error.message
                )
            }
    }
}