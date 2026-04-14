package io.github.peningtonj.recordcollection.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.peningtonj.recordcollection.ui.AppPlatform
import io.github.peningtonj.recordcollection.ui.LocalPlatform
import io.github.peningtonj.recordcollection.navigation.AuthNavigationWrapper
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.NavigationHost
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.playback.PlaybackBar
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun RecordCollectionApp(navigator: Navigator) {
    val playbackViewModel = rememberPlaybackViewModel()
    val searchViewModel = rememberSearchViewModel()
    val settingsViewModel = rememberSettingsViewModel()

    RecordCollectionTheme(viewModel = settingsViewModel) {
        CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalPlatform provides AppPlatform.ANDROID
            ) {
            AuthNavigationWrapper {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                val canNavigateBack by navigator.canNavigateBack.collectAsState()
                BackHandler(enabled = canNavigateBack) {
                    navigator.navigateBack()
                }

                Scaffold(
                    topBar = {
                        if (currentScreen != Screen.Login) {
                            TopAppBar(
                                title = { Text(currentScreen.toTitle()) },
                                navigationIcon = {
                                    if (canNavigateBack) {
                                        IconButton(onClick = { navigator.navigateBack() }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Navigate back"
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen != Screen.Login) {
                            Column {
                                PlaybackBar(playbackViewModel)
                                MobileNavigationBar(navigator, currentScreen)
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavigationHost(
                            startScreen = Screen.Login,
                            navigator = navigator
                        ) { screen ->
                            currentScreen = screen
                            AppScreenContent(screen, playbackViewModel, searchViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileNavigationBar(navigator: Navigator, currentScreen: Screen) {
    NavigationBar {
        NavigationBarItem(
            selected = currentScreen is Screen.Library,
            onClick = { navigator.navigateTo(Screen.Library) },
            icon = { Icon(Icons.Default.LocalLibrary, contentDescription = "Library") },
            label = { Text("Library") }
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Search,
            onClick = { navigator.navigateTo(Screen.Search) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = currentScreen == Screen.Collections || currentScreen is Screen.Collection,
            onClick = { navigator.navigateTo(Screen.Collections) },
            icon = { Icon(Icons.Default.Folder, contentDescription = "Collections") },
            label = { Text("Collections") }
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Profile,
            onClick = { navigator.navigateTo(Screen.Profile) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
        NavigationBarItem(
            selected = currentScreen is Screen.Settings,
            onClick = { navigator.navigateTo(Screen.Settings) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

