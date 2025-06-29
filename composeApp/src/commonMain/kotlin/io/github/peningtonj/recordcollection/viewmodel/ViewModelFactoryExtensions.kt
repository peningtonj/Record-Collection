// commonMain/viewmodel/ViewModelFactory.kt
package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi

@Composable
fun rememberProfileViewModel(dependencies: DependencyContainer = LocalDependencyContainer.current): ProfileViewModel {
    return remember {
        ProfileViewModel(
            repository = dependencies.profileRepository,
            spotifyApi = SpotifyApi(
                client = HttpClientProvider.create(),
                authRepository = dependencies.authRepository
            )
        )
    }
}

@Composable
fun rememberMainViewModel(dependencies: DependencyContainer = LocalDependencyContainer.current): MainViewModel {
    return remember {
        MainViewModel(dependencies.profileRepository)
    }
}