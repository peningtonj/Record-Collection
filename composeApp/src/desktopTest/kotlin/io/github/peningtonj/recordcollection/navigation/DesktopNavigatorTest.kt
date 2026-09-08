package io.github.peningtonj.recordcollection.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** End-to-end check that a popped screen's ViewModels are cleared (TECH_DEBT 1.2). */
class DesktopNavigatorTest {

    private class TrackingViewModel : ViewModel() {
        var cleared = false
            private set

        override fun onCleared() {
            cleared = true
        }
    }

    private fun ViewModelStoreOwner.trackingViewModel(): TrackingViewModel =
        ViewModelProvider.create(
            this,
            viewModelFactory { initializer { TrackingViewModel() } },
        ).get(TrackingViewModel::class)

    @Test
    fun `navigating back clears the popped screen's ViewModel`() {
        val navigator = DesktopNavigator()
        navigator.navigateTo(Screen.Library)

        val album = Screen.Album("album-1", "spotify-1")
        navigator.navigateTo(album)
        val albumVm = navigator.viewModelStoreOwnerFor(album).trackingViewModel()
        val libraryVm = navigator.viewModelStoreOwnerFor(Screen.Library).trackingViewModel()

        assertFalse(albumVm.cleared)

        navigator.navigateBack()

        assertTrue(albumVm.cleared, "Album screen was popped")
        assertFalse(libraryVm.cleared, "Library is now the current screen")
    }

    @Test
    fun `popUpTo clears the back-stack entries it drops`() {
        val navigator = DesktopNavigator()
        navigator.navigateTo(Screen.Library)
        navigator.navigateTo(Screen.Search)
        navigator.navigateTo(Screen.Album("a", "s"))

        // Search is in the middle of the back stack and gets trimmed away.
        val searchVm = navigator.viewModelStoreOwnerFor(Screen.Search).trackingViewModel()

        navigator.popUpTo(Screen.Library, inclusive = false)

        assertTrue(searchVm.cleared)
    }
}
