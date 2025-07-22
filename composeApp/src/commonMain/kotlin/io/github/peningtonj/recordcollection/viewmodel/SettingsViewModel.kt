package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.peningtonj.recordcollection.repository.CacheSize
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.repository.SettingsState
import io.github.peningtonj.recordcollection.repository.SortOrder
import io.github.peningtonj.recordcollection.repository.SyncInterval
import io.github.peningtonj.recordcollection.repository.Theme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Expose the repository's settings directly
    val settings: StateFlow<SettingsState> = settingsRepository.settings

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(theme = theme))
        }
    }

    fun toggleAutoSync() {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(autoSync = !currentSettings.autoSync))
        }
    }

    fun updateSyncInterval(interval: SyncInterval) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(syncInterval = interval))
        }
    }

    fun toggleShowAlbumYear() {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(showAlbumYear = !currentSettings.showAlbumYear))
        }
    }
    fun toggleDefaultOnAddToCollection() {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(defaultOnAddToCollection = !currentSettings.defaultOnAddToCollection))
        }
    }


    fun toggleTransitionTrack() {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(transitionTrack = !currentSettings.transitionTrack))
        }
    }

    fun updateDefaultSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(defaultSortOrder = sortOrder))
        }
    }

    fun updateCacheSize(cacheSize: CacheSize) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(cacheSize = cacheSize))
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.updateSettings(SettingsState())
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            // Implementation would clear the actual cache
            settingsRepository.clearCache()
        }
    }

    fun updateOnAddToLibrarySetting(collectionName: String, onAddToLibrary: OnAddToCollection) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(collectionAddToLibrary = currentSettings.collectionAddToLibrary.toMutableMap().apply {
                put(collectionName, onAddToLibrary)
            }))
        }
    }

    fun exportLibrary() {
        viewModelScope.launch {
            // Implementation would export library data
            settingsRepository.exportLibrary()
        }
    }

    fun importLibrary() {
        viewModelScope.launch {
            // Implementation would import library data
            settingsRepository.importLibrary()
        }
    }
}