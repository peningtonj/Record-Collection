package io.github.peningtonj.recordcollection.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsRepository(private val settingsStorage: Settings) {
    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    init {
        CoroutineScope(Dispatchers.Default).launch {
            _settings.value = loadFromStorage()
        }
    }

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

    private fun saveToStorage(settings: SettingsState) {
        settingsStorage.set("theme", settings.theme.name)
        settingsStorage.set("autoSync", settings.autoSync)
        settingsStorage.set("syncInterval", settings.syncInterval.name)
        settingsStorage.set("showAlbumYear", settings.showAlbumYear)
        settingsStorage.set("defaultSortOrder", settings.defaultSortOrder.name)
        settingsStorage.set("cacheSize", settings.cacheSize.name)
        settingsStorage.set("defaultOnAddToCollection", settings.defaultOnAddToCollection)
        settingsStorage.set("transitionTrack", settings.transitionTrack)
        settingsStorage.set("openAiApiKey", settings.openAiApiKey)
        settingsStorage.set("openAiApiKeyValid", settings.openAiApiKeyValid)
        settingsStorage.set("collectionAddToLibrary", Json.encodeToString(settings.collectionAddToLibrary))
        settingsStorage.set("addTracksOnMaxRating", settings.addTracksOnMaxRating)
    }

    private fun loadFromStorage(): SettingsState {
        val mapJson = settingsStorage.getStringOrNull("collectionAddToLibrary")
        val collectionAddToLibrary = mapJson?.let {
            runCatching {
                Json.decodeFromString<Map<String, OnAddToCollection>>(it)
            }.getOrElse { emptyMap() }
        } ?: emptyMap()

        return SettingsState(
            theme = Theme.valueOf(settingsStorage.get("theme", Theme.SYSTEM.name)),
            autoSync = settingsStorage.get("autoSync", true),
            syncInterval = SyncInterval.valueOf(settingsStorage.get("syncInterval", SyncInterval.DAILY.name)),
            showAlbumYear = settingsStorage.get("showAlbumYear", true),
            defaultSortOrder = SortOrder.valueOf(settingsStorage.get("defaultSortOrder", SortOrder.RELEASE_DATE.name)),
            cacheSize = CacheSize.valueOf(settingsStorage.get("cacheSize", CacheSize.MEDIUM.name)),
            defaultOnAddToCollection = settingsStorage.get("defaultOnAddToCollection", false),
            transitionTrack = settingsStorage.get("transitionTrack", true),
            openAiApiKey = settingsStorage.get("openAiApiKey", ""),
            openAiApiKeyValid = settingsStorage.get("openAiApiKeyValid", false),
            collectionAddToLibrary = collectionAddToLibrary,
            addTracksOnMaxRating = settingsStorage.get("addTracksOnMaxRating", false),
        )
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
    val collectionAddToLibrary: Map<String, OnAddToCollection> = emptyMap(),
    val transitionTrack: Boolean = true,
    val openAiApiKey: String = "",
    val openAiApiKeyValid: Boolean = false,
    val addTracksOnMaxRating: Boolean = false,
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

@Serializable
enum class OnAddToCollection(val displayName: String, val value: Boolean?) {
    DEFAULT("App Default", null),
    TRUE("On", true),
    FALSE("Off", false)
}

@Serializable
data class SerializableSettingsState(
    val collectionAddToLibrary: Map<String, OnAddToCollection> = emptyMap()
)
