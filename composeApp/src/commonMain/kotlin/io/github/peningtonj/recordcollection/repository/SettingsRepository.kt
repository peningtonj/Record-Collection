package io.github.peningtonj.recordcollection.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository {
    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    suspend fun updateSettings(newSettings: SettingsState) {
        _settings.value = newSettings
        saveToStorage(newSettings)
    }

    suspend fun clearCache() {
        // Implement cache clearing logic
    }

    suspend fun exportLibrary() {
        // Implement library export logic
    }

    suspend fun importLibrary() {
        // Implement library import logic
    }

    private suspend fun saveToStorage(settings: SettingsState) {
        // Implement persistence (SharedPreferences/UserDefaults/DataStore)
    }

    private suspend fun loadFromStorage(): SettingsState {
        // Implement loading from storage
        return SettingsState()
    }

    init {
        // Load initial settings from storage
        // Note: In a real app, you might want to do this in a suspend function
        _settings.value = SettingsState() // For now, use defaults
    }
}


data class SettingsState(
    val theme: Theme = Theme.SYSTEM,
    val autoSync: Boolean = true,
    val syncInterval: SyncInterval = SyncInterval.DAILY,
    val showAlbumYear: Boolean = true,
    val defaultSortOrder: SortOrder = SortOrder.RELEASE_DATE,
    val cacheSize: CacheSize = CacheSize.MEDIUM,
    val defaultOnAddToCollection: Boolean = false,
    val collectionAddToLibrary: Map<String, OnAddToCollection> = emptyMap()
)


enum class SyncInterval(val displayName: String, val hours: Int) {
    NEVER("Never", 0),
    HOURLY("Hourly", 1),
    DAILY("Daily", 24),
    WEEKLY("Weekly", 168)
}

enum class SortOrder(val displayName: String) {
    ARTIST_NAME("Artist Name"),
    ALBUM_NAME("Album Name"),
    RELEASE_DATE("Release Date"),
    DATE_ADDED("Date Added"),
    RATING("Rating")
}

enum class CacheSize(val displayName: String, val sizeInMB: Int) {
    SMALL("Small (100MB)", 100),
    MEDIUM("Medium (500MB)", 500),
    LARGE("Large (1GB)", 1000),
    UNLIMITED("Unlimited", -1)
}

enum class Theme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

enum class OnAddToCollection(val displayName: String, val value: Boolean?) {
    DEFAULT("App Default", null),
    TRUE("On", true),
    FALSE("Off", false)
}
