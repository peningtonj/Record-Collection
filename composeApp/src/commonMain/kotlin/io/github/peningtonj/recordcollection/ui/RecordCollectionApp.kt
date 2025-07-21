package io.github.peningtonj.recordcollection.ui

import ArtistDetailScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.navigation.*
import io.github.peningtonj.recordcollection.ui.components.navigation.NavigationPanel
import io.github.peningtonj.recordcollection.ui.components.playback.PlaybackBar
import io.github.peningtonj.recordcollection.ui.screens.AlbumScreen
import io.github.peningtonj.recordcollection.ui.screens.CollectionScreen
import io.github.peningtonj.recordcollection.ui.screens.LibraryScreen
import io.github.peningtonj.recordcollection.ui.screens.LoginScreen
import io.github.peningtonj.recordcollection.ui.screens.ProfileScreen
import io.github.peningtonj.recordcollection.ui.screens.SearchScreen
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel

@Composable
fun RecordCollectionApp(
    navigator: Navigator,
) {
    Napier.d("RecordCollectionApp composable started")

    val playbackViewModel = rememberPlaybackViewModel()
    val searchViewModel = rememberSearchViewModel()

    CompositionLocalProvider(LocalNavigator provides navigator) {
        AuthNavigationWrapper {
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }
            
            Column(modifier = Modifier.fillMaxSize()) {
                // Main content area with navigation
                Row(modifier = Modifier.weight(1f)) {
                    if (currentScreen != Screen.Login) {
                        NavigationPanel(
                            navigator = navigator,
                            currentScreen = currentScreen,
                            modifier = Modifier
                                .widthIn(min = 100.dp, max = 300.dp)
                        )

                        // Vertical divider
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                        )
                    }

                    // Main content area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        NavigationHost(
                            startScreen = Screen.Login,
                            navigator = navigator
                        ) { screen ->
                            Napier.d("NavigationHost rendering screen: $screen")
                            currentScreen = screen // Update current screen state

                            when (screen) {
                                Screen.Login -> LoginScreen()
                                Screen.Profile -> ProfileScreen()
                                Screen.Library -> LibraryScreen(
                                    playbackViewModel = playbackViewModel
                                )
                                Screen.Search -> SearchScreen(
                                    playbackViewModel = playbackViewModel,
                                    viewModel = searchViewModel
                                )
                                is Screen.Album -> AlbumScreen(
                                    albumId = screen.albumId,
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
                    }
                }

                // Playback bar at the bottom spanning full width
                if (currentScreen != Screen.Login) {
                    PlaybackBar(playbackViewModel)
                }
            }
        }
    }
}