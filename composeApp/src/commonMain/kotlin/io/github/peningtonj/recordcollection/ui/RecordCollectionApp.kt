package io.github.peningtonj.recordcollection.ui

import ArtistDetailScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import io.github.peningtonj.recordcollection.ui.screens.SettingsScreen
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCollectionApp(
    navigator: Navigator,
) {
    Napier.d("RecordCollectionApp composable started")

    val playbackViewModel = rememberPlaybackViewModel()
    val searchViewModel = rememberSearchViewModel()
    val settingsViewModel = rememberSettingsViewModel()

    RecordCollectionTheme(
        viewModel = settingsViewModel
    ) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            AuthNavigationWrapper {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Add TopAppBar here, only show when not on login screen
                    if (currentScreen != Screen.Login) {
                        TopAppBar(
                            title = { 
                                Text(
                                    when (currentScreen) {
                                        Screen.Library -> "Library"
                                        Screen.Search -> "Search"
                                        Screen.Profile -> "Profile"
                                        Screen.Settings -> "Settings"
                                        is Screen.Album -> "Album Details"
                                        is Screen.Artist -> "Artist Details"
                                        is Screen.Collection -> "Collection"
                                        else -> ""
                                    }
                                )
                            },
                            navigationIcon = {
                                val canNavigateBack by navigator.canNavigateBack.collectAsState()

                                IconButton(onClick = { navigator.navigateBack() },
                                        enabled = canNavigateBack
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Navigate back"
                                        )
                                    }
                                }
                        )
                    }

                    // Your existing Row with navigation and content
                    Row(modifier = Modifier.weight(1f)) {
                        if (currentScreen != Screen.Login) {
                            NavigationPanel(
                                navigator = navigator,
                                currentScreen = currentScreen,
                                modifier = Modifier
                                    .widthIn(min = 100.dp, max = 300.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )

                            // Vertical divider
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                            )
                        }

                        // Main content area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
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
                                    Screen.Settings -> SettingsScreen()
                                    Screen.Library -> LibraryScreen(
                                        playbackViewModel = playbackViewModel
                                    )

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
                        }
                    }

                    // Your existing PlaybackBar
                    if (currentScreen != Screen.Login) {
                        PlaybackBar(playbackViewModel)
                    }
                }
            }
        }
    }
}