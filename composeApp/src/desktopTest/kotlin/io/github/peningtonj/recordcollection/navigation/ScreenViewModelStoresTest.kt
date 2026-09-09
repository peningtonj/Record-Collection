package io.github.peningtonj.recordcollection.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ScreenViewModelStoresTest {

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
    fun `ownerFor returns the same owner for an equal screen`() {
        val stores = ScreenViewModelStores()
        assertSame(
            stores.ownerFor(Screen.Album("x", "y")),
            stores.ownerFor(Screen.Album("x", "y")),
        )
    }

    @Test
    fun `retainOnly clears the stores of screens that are no longer live`() {
        val stores = ScreenViewModelStores()
        val libraryVm = stores.ownerFor(Screen.Library).trackingViewModel()
        val albumVm = stores.ownerFor(Screen.Album("a", "s")).trackingViewModel()

        stores.retainOnly(setOf(Screen.Library))

        assertFalse(libraryVm.cleared, "Library is still live")
        assertTrue(albumVm.cleared, "Album was popped — its ViewModel should be cleared")
    }

    @Test
    fun `re-navigating to a cleared screen gets a fresh store`() {
        val stores = ScreenViewModelStores()
        val first = stores.ownerFor(Screen.Library).trackingViewModel()

        stores.retainOnly(emptySet())
        val second = stores.ownerFor(Screen.Library).trackingViewModel()

        assertTrue(first.cleared)
        assertNotSame(first, second)
    }

    @Test
    fun `clearAll clears every store`() {
        val stores = ScreenViewModelStores()
        val a = stores.ownerFor(Screen.Library).trackingViewModel()
        val b = stores.ownerFor(Screen.Search).trackingViewModel()

        stores.clearAll()

        assertTrue(a.cleared)
        assertTrue(b.cleared)
    }
}
