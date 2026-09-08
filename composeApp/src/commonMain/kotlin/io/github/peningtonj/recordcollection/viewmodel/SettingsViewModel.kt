package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.repository.CacheSize
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.repository.SettingsState
import io.github.peningtonj.recordcollection.repository.SortOrder
import io.github.peningtonj.recordcollection.repository.SyncInterval
import io.github.peningtonj.recordcollection.repository.Theme
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val openAiApi: OpenAiApi
) : ViewModel() {

    // Expose the repository's settings directly
    val settings: StateFlow<SettingsState> = settingsRepository.settings

    /** Applies [transform] to the current settings and persists them; failures are logged, not thrown. */
    private fun edit(operation: String, transform: (SettingsState) -> SettingsState) =
        launchSafely(operation) {
            settingsRepository.updateSettings(transform(settings.value))
        }

    fun updateTheme(theme: Theme) = edit("updateTheme") { it.copy(theme = theme) }

    fun toggleAutoSync() = edit("toggleAutoSync") { it.copy(autoSync = !it.autoSync) }

    fun updateSyncInterval(interval: SyncInterval) = edit("updateSyncInterval") { it.copy(syncInterval = interval) }

    fun toggleShowAlbumYear() = edit("toggleShowAlbumYear") { it.copy(showAlbumYear = !it.showAlbumYear) }

    fun toggleDefaultOnAddToCollection() =
        edit("toggleDefaultOnAddToCollection") { it.copy(defaultOnAddToCollection = !it.defaultOnAddToCollection) }

    fun toggleAddTracksOnMaxRating() =
        edit("toggleAddTracksOnMaxRating") { it.copy(addTracksOnMaxRating = !it.addTracksOnMaxRating) }

    fun toggleTransitionTrack() = edit("toggleTransitionTrack") { it.copy(transitionTrack = !it.transitionTrack) }

    fun updateDefaultSortOrder(sortOrder: SortOrder) = edit("updateDefaultSortOrder") { it.copy(defaultSortOrder = sortOrder) }

    fun updateCacheSize(cacheSize: CacheSize) = edit("updateCacheSize") { it.copy(cacheSize = cacheSize) }

    fun resetToDefaults() = edit("resetToDefaults") { SettingsState() }

    fun updateOpenAiApiKey(value: String) = edit("updateOpenAiApiKey") { it.copy(openAiApiKey = value) }

    fun updateOnAddToLibrarySetting(collectionName: String, onAddToLibrary: OnAddToCollection) =
        edit("updateOnAddToLibrarySetting") {
            it.copy(collectionAddToLibrary = it.collectionAddToLibrary + (collectionName to onAddToLibrary))
        }

    fun validateOpenAiApiKey() = launchSafely("validateOpenAiApiKey") {
        val currentSettings = settings.value
        val valid = try {
            openAiApi.isApiKeyValid(currentSettings.openAiApiKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.e(e) { "Failed to validate OpenAI API key" }
            false
        }
        settingsRepository.updateSettings(currentSettings.copy(openAiApiKeyValid = valid))
    }

    fun clearCache() = launchSafely("clearCache") { settingsRepository.clearCache() }

    fun exportLibrary() = launchSafely("exportLibrary") { settingsRepository.exportLibrary() }

    fun importLibrary() = launchSafely("importLibrary") { settingsRepository.importLibrary() }
}
