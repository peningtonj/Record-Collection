package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator

/*
 * ViewModels are obtained through `viewModel { }`, which registers them with the
 * `LocalViewModelStoreOwner` in scope:
 *   - always-on VMs (auth, playback, search) resolve to the app-session store
 *     provided in `App.kt`;
 *   - screen VMs resolve to the per-screen store provided by `NavigationHost`, so
 *     `onCleared()` fires (cancelling `viewModelScope` + Firestore listeners) when
 *     the screen is popped.
 * The dependency container is a process singleton, so it is not a `viewModel` key.
 */

@Composable
fun rememberAuthViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): AuthViewModel = viewModel {
    AuthViewModel(
        authRepository = dependencies.authRepository,
        userSessionRepository = dependencies.userSessionRepository
    )
}

@Composable
fun rememberLoginViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current,
    navigator: Navigator = LocalNavigator.current
): LoginViewModel = viewModel {
    LoginViewModel(
        authRepository = dependencies.authRepository,
        navigator = navigator
    )
}

@Composable
fun rememberLibraryViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): LibraryViewModel = viewModel {
    LibraryViewModel(
        dependencies.libraryService,
        dependencies.collectionsService,
        dependencies.albumRepository,
        dependencies.artistRepository,
    )
}

@Composable
fun rememberPlaybackViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): PlaybackViewModel = viewModel {
    PlaybackViewModel(
        dependencies.playbackRepository,
        dependencies.playbackSessionManager
    )
}

@Composable
fun rememberAlbumViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): AlbumViewModel = viewModel {
    AlbumViewModel(
        dependencies.albumRepository,
        dependencies.ratingRepository,
        dependencies.collectionAlbumRepository,
        dependencies.tagService,
        dependencies.releaseGroupUseCase,
        dependencies.settingsRepository
    )
}

@Composable
fun rememberAlbumDetailViewModel(
    albumId: String,
    spotifyId: String,
    dependencies: DependencyContainer = LocalDependencyContainer.current
): AlbumDetailViewModel = viewModel {
    AlbumDetailViewModel(
        albumId = albumId,
        spotifyId = spotifyId,
        getAlbumDetailUseCase = dependencies.albumDetailUseCase,
        trackRepository = dependencies.trackRepository
    )
}

@Composable
fun rememberCollectionsViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionsViewModel = viewModel {
    CollectionsViewModel(
        repository = dependencies.albumCollectionRepository,
    )
}

@Composable
fun rememberArticleImportViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionImportViewModel = viewModel {
    CollectionImportViewModel(
        collectionImportService = dependencies.collectionImportService,
        profileRepository = dependencies.profileRepository
    )
}

@Composable
fun rememberCollectionDetailViewModel(
    collectionName: String,
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionDetailViewModel = viewModel {
    CollectionDetailViewModel(
        collectionRepository = dependencies.albumCollectionRepository,
        collectionAlbumRepository = dependencies.collectionAlbumRepository,
        collectionName = collectionName
    )
}

@Composable
fun rememberArtistDetailViewModel(
    artistId: String,
    dependencies: DependencyContainer = LocalDependencyContainer.current
): ArtistDetailViewModel = viewModel {
    ArtistDetailViewModel(
        artistRepository = dependencies.artistRepository,
        albumRepository = dependencies.albumRepository,
        artistId = artistId
    )
}

@Composable
fun rememberSearchViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): SearchViewModel = viewModel {
    SearchViewModel(
        searchRepository = dependencies.searchRepository,
        albumRepository = dependencies.albumRepository,
    )
}

/**
 * The settings ViewModel is a process-lifetime singleton owned by the DI container
 * (it also backs the app theme), so it is returned directly rather than through a
 * per-screen `viewModel { }` store.
 */
@Composable
fun rememberSettingsViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): SettingsViewModel = dependencies.settingsViewModel
