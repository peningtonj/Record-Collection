package io.github.peningtonj.recordcollection.navigation

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import io.github.aakira.napier.Napier

/**
 * Owns one [ViewModelStore] per back-stack [Screen] so that screen-scoped ViewModels
 * get their `onCleared()` called (and their `viewModelScope` / Firestore listeners
 * cancelled) when the screen is popped.
 *
 * Keyed by `Screen` value: navigating to the same screen (same album id, etc.) reuses
 * its store, and a screen that appears more than once on the back stack shares one
 * store. That matches how [Navigator]'s hand-rolled back stack already behaves.
 *
 * The navigator calls [retainOnly] after every navigation with the set of screens still
 * reachable (current + back stack); everything else is cleared.
 */
class ScreenViewModelStores {

    private class Owner : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    private val owners = mutableMapOf<Screen, Owner>()

    fun ownerFor(screen: Screen): ViewModelStoreOwner =
        owners.getOrPut(screen) { Owner() }

    /** Clears and drops every store whose screen is not in [liveScreens]. */
    fun retainOnly(liveScreens: Set<Screen>) {
        val gone = owners.keys - liveScreens
        if (gone.isEmpty()) return
        gone.forEach { screen ->
            owners.remove(screen)?.viewModelStore?.clear()
            Napier.d("Cleared ViewModelStore for $screen")
        }
    }

    /** Clears everything — call when the whole navigator is being torn down. */
    fun clearAll() {
        owners.values.forEach { it.viewModelStore.clear() }
        owners.clear()
    }
}
