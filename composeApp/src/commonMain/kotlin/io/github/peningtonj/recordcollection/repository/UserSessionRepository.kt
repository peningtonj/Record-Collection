package io.github.peningtonj.recordcollection.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Holds the current Spotify user ID and persists it to local Settings so it survives restarts.
 *
 * Lifecycle:
 *  - On startup: loaded from Settings (non-null for any previously-authenticated user).
 *  - After first Spotify login: [setUserId] is called by [LibraryService.initUserSession].
 *  - Logout: call [clearUserId] to remove the cached ID.
 */
class UserSessionRepository(private val settings: Settings) {

    private val _userId = MutableStateFlow<String?>(loadUserId())
    val userIdFlow: StateFlow<String?> = _userId.asStateFlow()

    /** Returns the current user ID, or null if not yet initialised. */
    fun getUserId(): String? = _userId.value

    /**
     * Suspends until the user ID is available, then returns it.
     *
     * Use this on every write path (`users/{id}/…`). Reads that build a `Flow` should
     * instead observe [userIdFlow] so they re-emit if the account changes. For a user
     * who has previously authenticated the ID is already loaded from Settings, so this
     * returns immediately; on a fresh first login it waits for
     * `LibraryService.initUserSession()`.
     */
    suspend fun awaitUserId(): String = _userId.filterNotNull().first()

    /** Persists the Spotify user ID (called once after a successful profile fetch). */
    suspend fun setUserId(userId: String) {
        settings[KEY_USER_ID] = userId
        _userId.value = userId
    }

    /** Clears the persisted user ID (called on logout). */
    fun clearUserId() {
        settings.remove(KEY_USER_ID)
        _userId.value = null
    }

    private fun loadUserId(): String? =
        settings.getStringOrNull(KEY_USER_ID)?.ifEmpty { null }

    companion object {
        const val KEY_USER_ID = "spotify_user_id"
    }
}

