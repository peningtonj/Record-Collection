package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi

@Composable
fun rememberAuthViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): AuthViewModel {
    return remember(dependencies) {
        AuthViewModel(
            authRepository = dependencies.authRepository
        )
    }
}

@Composable
fun rememberLoginViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current,
    navigator: Navigator = LocalNavigator.current
): LoginViewModel {
    return remember(dependencies, navigator) {
        LoginViewModel(
            authRepository = dependencies.authRepository,
            navigator = navigator
        )
    }
}

@Composable
fun rememberLibraryViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): LibraryViewModel {
    return remember {
        LibraryViewModel(
            dependencies.libraryService,
            dependencies.albumRepository,
            dependencies.artistRepository
        )
    }
}

@Composable
fun rememberPlaybackViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): PlaybackViewModel {
    return remember {
        PlaybackViewModel(dependencies.playbackRepository)
    }
}