package io.github.peningtonj.recordcollection.viewmodel

import ArtistDetailViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel

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
            dependencies.collectionsService,
            dependencies.albumDetailUseCase,
            dependencies.albumRepository,
            dependencies.artistRepository,
        )
    }
}

@Composable
fun rememberPlaybackViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): PlaybackViewModel {
    return remember {
        PlaybackViewModel(
            dependencies.playbackRepository,
            dependencies.playbackQueueService,
            dependencies.settingsRepository
        )
    }
}


@Composable
fun rememberAlbumViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): AlbumViewModel {
    return remember {
        AlbumViewModel(
            dependencies.albumRepository,
            dependencies.ratingRepository,
            dependencies.collectionAlbumRepository,
            dependencies.tagService,
            dependencies.releaseGroupUseCase,
            dependencies.settingsRepository
        )
    }
}

@Composable
fun rememberAlbumDetailViewModel(
    albumId: String,
    spotifyId: String,
    dependencies: DependencyContainer = LocalDependencyContainer.current
): AlbumDetailViewModel {
    return remember {
        AlbumDetailViewModel(
            albumId = albumId,
            spotifyId = spotifyId,
            getAlbumDetailUseCase = dependencies.albumDetailUseCase,
            trackRepository = dependencies.trackRepository
        )
    }
}

@Composable
fun rememberCollectionsViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionsViewModel {
    return remember(dependencies) {
        CollectionsViewModel(
            repository = dependencies.albumCollectionRepository,
        )
    }
}

@Composable
fun rememberArticleImportViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionImportViewModel {
    return remember(dependencies) {
        CollectionImportViewModel(
            collectionImportService = dependencies.collectionImportService,
            profileRepository = dependencies.profileRepository
        )
    }
}


@Composable
fun rememberCollectionDetailViewModel(
    collectionName: String,
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionDetailViewModel {
    return remember(collectionName, dependencies) {
        CollectionDetailViewModel(
            collectionRepository = dependencies.albumCollectionRepository,
            collectionAlbumRepository = dependencies.collectionAlbumRepository,
            getAlbumDetailUseCase = dependencies.albumDetailUseCase,
            ratingRepository = dependencies.ratingRepository,
            collectionName = collectionName
        )
    }
}

@Composable
fun rememberArtistDetailViewModel(
    artistId: String,
    dependencies: DependencyContainer = LocalDependencyContainer.current
): ArtistDetailViewModel {
    return remember(artistId, dependencies) {
        ArtistDetailViewModel(
            artistRepository = dependencies.artistRepository,
            albumRepository = dependencies.albumRepository,
            artistId = artistId
        )
    }
}

@Composable
fun rememberSearchViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): SearchViewModel {
    return remember(dependencies) {
        SearchViewModel(
            searchRepository = dependencies.searchRepository,
            albumRepository = dependencies.albumRepository,
            getAlbumUseCase = dependencies.albumDetailUseCase,
        )
    }
}
@Composable
fun rememberSettingsViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): SettingsViewModel {
    return remember(dependencies) {
        dependencies.settingsViewModel
    }
}