package io.github.peningtonj.recordcollection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.ui.AppPlatform
import io.github.peningtonj.recordcollection.ui.LocalPlatform
import io.github.peningtonj.recordcollection.navigation.AuthNavigationWrapper
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.NavigationHost
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.navigation.NavigationPanel
import io.github.peningtonj.recordcollection.ui.components.playback.PlaybackBar
import io.github.peningtonj.recordcollection.viewmodel.rememberPlaybackViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSearchViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun RecordCollectionApp(navigator: Navigator) {
    Napier.d("RecordCollectionApp (desktop) composable started")

    val playbackViewModel = rememberPlaybackViewModel()
    val searchViewModel = rememberSearchViewModel()
    val settingsViewModel = rememberSettingsViewModel()

    RecordCollectionTheme(viewModel = settingsViewModel) {
        CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalPlatform provides AppPlatform.DESKTOP
            ) {
            AuthNavigationWrapper {
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (currentScreen != Screen.Login) {
                        TopAppBar(
                            title = { Text(currentScreen.toTitle()) },
                            navigationIcon = {
                                val canNavigateBack by navigator.canNavigateBack.collectAsState()
                                IconButton(
                                    onClick = { navigator.navigateBack() },
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

                    Row(modifier = Modifier.weight(1f)) {
                        if (currentScreen != Screen.Login) {
                            NavigationPanel(
                                navigator = navigator,
                                currentScreen = currentScreen,
                                modifier = Modifier
                                    .widthIn(min = 100.dp, max = 300.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp),
                            )
                        }

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
                                Napier.d("Desktop NavigationHost rendering screen: $screen")
                                currentScreen = screen
                                AppScreenContent(screen, playbackViewModel, searchViewModel)
                            }
                        }
                    }

                    if (currentScreen != Screen.Login) {
                        PlaybackBar(playbackViewModel)
                    }
                }
            }
        }
    }
}


