package io.github.peningtonj.recordcollection.viewmodel

import Playlist
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.service.CollectionImportService
import io.github.peningtonj.recordcollection.service.AlbumNameAndArtist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CollectionImportViewModel(
    private val collectionImportService: CollectionImportService,
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _importSource  = MutableStateFlow<ImportSource?>(null)
    val importSource: StateFlow<ImportSource?> = _importSource.asStateFlow()


    private val _userPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<Playlist>> = _userPlaylists.asStateFlow()

    fun clearImportResult() {
        _uiState.value = UiState.Idle
    }

    init {
        viewModelScope.launch {
            _userPlaylists.value = profileRepository.getUserSavedPlaylist()
        }
    }

    fun setImportSource(source: ImportSource?) {
        Napier.d { "Setting import source to $source" }
        _importSource.value = source
    }

    fun draftCollectionFromUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val response = collectionImportService.getResponseFromOpenAI(url)
            try {
                val albums = collectionImportService.parseAlbumAndArtistResponse(response)
                Napier.d { "Successfully found ${albums.size} albums" }
                _uiState.value = UiState.AlbumsList(
//                    openAiResponse = response,
                    albumNames = albums
                )
//                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to parse response: $response: $e")
            }
        }
    }
    fun getAlbumsFromDraft() {
        viewModelScope.launch {
            val current = _uiState.value

            if (current !is UiState.AlbumsList) {
                Napier.w { "getAlbumsFromDraft called in invalid state: $current" }
                return@launch
            }

            Napier.d("Importing albums from draft: ${current.albumNames}")

            val lookupResults = mutableListOf<AlbumLookUpResult>()
            _uiState.value = UiState.Searching(
                albumNames = current.albumNames,
                albums = emptyList()
            )

            collectionImportService.streamAlbumLookups(current.albumNames) { result ->
                lookupResults += result
                _uiState.value = UiState.Searching(
                    albumNames = current.albumNames,
                    albums = lookupResults.toList()
                )
            }

            val successfulAlbums = lookupResults.mapNotNull { it.album }
            _uiState.value = UiState.ReadyToImport(albums = successfulAlbums)
        }
    }

    fun getAlbumsFromPlaylist(playlistInput: String) {

        //
        viewModelScope.launch {
            var playlistId = ""
            playlistId = if (playlistInput.contains("open.spotify.com")) {
                Regex("playlist/([a-zA-Z0-9]+)").find(playlistInput)?.groupValues?.get(1) ?: ""
            } else {
                playlistInput.trim()
            }
            _uiState.value = UiState.Loading
            Napier.d("Importing albums from playlist")
            _uiState.value = UiState.ReadyToImport(
                collectionImportService.getAlbumsFromPlaylist(playlistId)
            )
        }
    }
}

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class AlbumsList(
        val albumNames: List<AlbumNameAndArtist> = emptyList(),
    ): UiState
    data class Searching(
        val albumNames: List<AlbumNameAndArtist> = emptyList(),
        val albums: List<AlbumLookUpResult> = emptyList()
    ) : UiState
    data class ReadyToImport(
        val albums: List<Album> = emptyList(),
    ) : UiState
    data class Error(val message: String) : UiState
}

data class AlbumLookUpResult(
    val query: AlbumNameAndArtist,
    val album: Album?
)

enum class ImportSource {
    ARTICLE,
    PLAYLIST
}
