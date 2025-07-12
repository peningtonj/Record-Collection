package io.github.peningtonj.recordcollection.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalDependencyContainer
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.ui.collection.CollectionDetailViewModel
import io.github.peningtonj.recordcollection.ui.collections.CollectionsViewModel

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
        PlaybackViewModel(dependencies.playbackRepository)
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
            getAlbumDetailUseCase = dependencies.albumDetailUseCase,
            tagRepository = dependencies.tagRepository,
            albumTagRepository = dependencies.albumTagRepository,
        )
    }
}

@Composable
fun rememberCollectionsViewModel(
    dependencies: DependencyContainer = LocalDependencyContainer.current
): CollectionsViewModel {
    return remember(dependencies) {
        CollectionsViewModel(
            repository = dependencies.albumCollectionRepository
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
            ratingRepository = dependencies.ratingRepository,
            collectionName = collectionName
        )
    }
}