package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.service.ArticleImportService
import io.github.peningtonj.recordcollection.service.OpenAiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArticleImportViewModel(
    private val articleImportService: ArticleImportService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _importState = MutableStateFlow<ArticleImportData?>(null)
    val importState: StateFlow<ArticleImportData?> = _importState.asStateFlow()


    fun clearImportResult() {
        _importState.value = null
        _uiState.value = UiState.Idle
    }

    fun draftCollectionFromUrl(url: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val response = articleImportService.getResponseFromOpenAI(url)

            try {
                val albums = articleImportService.parseResponse(response)
                _importState.value = ArticleImportData(
                    openAiResponse = response,
                    albumNames = albums
                )
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to parse response: $response")
            }
        }
    }

    fun getAlbumsFromDraft() {
        viewModelScope.launch {
            Napier.d("Importing albums from draft")
            val current = _importState.value ?: return@launch

            _uiState.value = UiState.Loading

            articleImportService.streamAlbumLookups(current.albumNames) { result ->
                _importState.update { state ->
                    state?.copy(
                        lookupResults = state.lookupResults + result
                    )
                }
            }
            _uiState.value = UiState.Idle
        }
    }
}

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Error(val message: String) : UiState
}

data class ArticleImportData(
    val openAiResponse: String,
    val albumNames: List<OpenAiResponse> = emptyList(),
    val lookupResults: List<AlbumLookUpResult> = emptyList(),
)

data class AlbumLookUpResult(
    val query: OpenAiResponse,
    val album: Album?
)