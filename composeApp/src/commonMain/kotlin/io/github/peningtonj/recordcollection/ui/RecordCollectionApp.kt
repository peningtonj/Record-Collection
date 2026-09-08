package io.github.peningtonj.recordcollection.ui

import io.github.peningtonj.recordcollection.ui.screens.ArtistDetailScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.util.TrafficSource
import io.github.peningtonj.recordcollection.ui.screens.AlbumScreen
import io.github.peningtonj.recordcollection.ui.screens.CollectionScreen
import io.github.peningtonj.recordcollection.ui.screens.CollectionsListScreen
import io.github.peningtonj.recordcollection.ui.screens.LibraryScreen
import io.github.peningtonj.recordcollection.ui.screens.LoginScreen
import io.github.peningtonj.recordcollection.ui.screens.ProfileScreen
import io.github.peningtonj.recordcollection.ui.screens.SearchScreen
import io.github.peningtonj.recordcollection.ui.screens.SettingsScreen
import io.github.peningtonj.recordcollection.viewmodel.PlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.SearchViewModel

/** Platform-specific top-level app layout (desktop = side nav, android = bottom nav). */
@Composable
expect fun RecordCollectionApp(navigator: Navigator)

/** Shared screen-routing logic used by both platform layouts. */
@Composable
fun AppScreenContent(
    screen: Screen,
    playbackViewModel: PlaybackViewModel,
    searchViewModel: SearchViewModel,
) {
    // Coarse traffic attribution: tag backend calls with the visible screen.
    LaunchedEffect(screen) { TrafficSource.current = screen.toTrafficName() }

    when (screen) {
        Screen.Login -> LoginScreen()
        Screen.Profile -> ProfileScreen()
        Screen.Settings -> SettingsScreen()
        Screen.Library -> LibraryScreen(playbackViewModel = playbackViewModel)
        Screen.Collections -> CollectionsListScreen()
        Screen.Search -> SearchScreen(
            playbackViewModel = playbackViewModel,
            viewModel = searchViewModel
        )
        is Screen.Album -> AlbumScreen(
            albumId = screen.albumId,
            spotifyId = screen.spotifyId,
            playbackViewModel = playbackViewModel
        )
        is Screen.Artist -> ArtistDetailScreen(
            artistId = screen.artistId,
            playbackViewModel = playbackViewModel
        )
        is Screen.Collection -> CollectionScreen(
            collectionName = screen.collectionName,
            playbackViewModel = playbackViewModel
        )
    }
}

/** Stable, low-cardinality name for traffic attribution (`Screen.Album` → `"AlbumScreen"`). */
fun Screen.toTrafficName(): String = when (this) {
    Screen.Login -> "LoginScreen"
    Screen.Library -> "LibraryScreen"
    Screen.Search -> "SearchScreen"
    Screen.Profile -> "ProfileScreen"
    Screen.Settings -> "SettingsScreen"
    Screen.Collections -> "CollectionsListScreen"
    is Screen.Album -> "AlbumScreen"
    is Screen.Artist -> "ArtistDetailScreen"
    is Screen.Collection -> "CollectionScreen"
}

/** Human-readable title for a given screen. */
fun Screen.toTitle(): String = when (this) {
    Screen.Library -> "Library"
    Screen.Search -> "Search"
    Screen.Profile -> "Profile"
    Screen.Settings -> "Settings"
    Screen.Collections -> "Collections"
    is Screen.Album -> "Album Details"
    is Screen.Artist -> "Artist Details"
    is Screen.Collection -> collectionName
    else -> ""
}