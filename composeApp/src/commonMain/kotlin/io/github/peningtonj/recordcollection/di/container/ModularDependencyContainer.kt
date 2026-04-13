package io.github.peningtonj.recordcollection.di.container

import PlaybackQueueService
import io.github.peningtonj.recordcollection.di.module.EventModule
import io.github.peningtonj.recordcollection.di.module.FirebaseModule
import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
import io.github.peningtonj.recordcollection.di.module.SettingsModule
import io.github.peningtonj.recordcollection.di.module.UseCaseModule
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.repository.*
import io.github.peningtonj.recordcollection.service.CollectionImportService
import io.github.peningtonj.recordcollection.service.CollectionsService
import io.github.peningtonj.recordcollection.service.LibraryService
import io.github.peningtonj.recordcollection.service.TagService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ModularDependencyContainer(
    private val networkModule: NetworkModule,
    private val repositoryModule: RepositoryModule,
    override val authHandler: AuthHandler,
    private val useCaseModule: UseCaseModule,
    private val eventModule: EventModule,
    private val settingsModule: SettingsModule,
    private val firebaseModule: FirebaseModule,
) : DependencyContainer {

    private val firestore by lazy { firebaseModule.provideFirebaseFirestore() }

    // Create event scope
    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override val authRepository by lazy {
        repositoryModule.provideAuthRepository(
            authHandler = authHandler,
            settings = settingsModule.provideSettings()
        )
    }

    private val spotifyApi by lazy {
        networkModule.provideSpotifyApi(authRepository)
    }

    private val miscApi by lazy {
        networkModule.provideMiscApi()
    }

    override val openAiApi: OpenAiApi by lazy {
        networkModule.provideOpenAiApi()
    }

    override val playlistRepository: PlaylistRepository by lazy {
        repositoryModule.providePlaylistRepository(spotifyApi)
    }

    // Create the event dispatcher
    private val albumEventDispatcher by lazy {
        val tagService = eventModule.provideTagService(
            tagRepository,
            albumTagRepository
        )
        val albumTagRepository = repositoryModule.provideAlbumTagRepository(firestore)
        val eventHandlers = eventModule.provideAlbumEventHandlers(
            tagService,
            albumTagRepository,
            tagRepository,
            artistRepository,
            spotifyApi
        )
        eventModule.provideAlbumEventDispatcher(eventHandlers, eventScope)
    }
    
    override val albumRepository by lazy {
        repositoryModule.provideAlbumRepository(
            firestore = firestore,
            miscApi = miscApi,
            spotifyApi = spotifyApi,
            eventDispatcher = albumEventDispatcher
        )
    }
    
    override val artistRepository by lazy {
        repositoryModule.provideArtistRepository(
            firestore = firestore,
            spotifyApi = spotifyApi,
            miscApi = miscApi
        )
    }
    
    override val playbackRepository by lazy {
        repositoryModule.providePlaybackRepository(spotifyApi)
    }
    
    override val profileRepository by lazy {
        repositoryModule.provideProfileRepository(spotifyApi)
    }
    
    override val libraryService by lazy {
        LibraryService(albumRepository, artistRepository, profileRepository, settingsRepository, trackRepository)
    }

    override val collectionImportService by lazy {
        CollectionImportService(albumCollectionRepository, searchRepository, playlistRepository, settingsRepository)
    }

    override val playbackQueueService by lazy {
        PlaybackQueueService(
            playbackRepository,
            trackRepository
        )
    }

    override val tagService by lazy {
        TagService(
            tagRepository = tagRepository,
            albumTagRepository = albumTagRepository
        )
    }

    override val ratingRepository by lazy {
        repositoryModule.provideRatingRepository(firestore)
    }
    
    override val albumCollectionRepository by lazy {
        repositoryModule.provideAlbumCollectionRepository(firestore, openAiApi)
    }

    override val collectionAlbumRepository by lazy {
        repositoryModule.provideCollectionAlbumRepository(firestore, albumRepository)
    }

    override val albumTagRepository by lazy {
        repositoryModule.provideAlbumTagRepository(firestore)
    }

    override val tagRepository by lazy {
        repositoryModule.provideTagRepository(firestore)
    }

    override val albumDetailUseCase by lazy {
        useCaseModule.provideGetAlbumDetailUseCase(
            albumRepository,
            albumTagRepository,
            collectionAlbumRepository,
            trackRepository
        )
    }

    override val collectionsService: CollectionsService by lazy {
        CollectionsService(collectionAlbumRepository, albumCollectionRepository, albumRepository)
    }

    override val searchRepository by lazy {
        SearchRepository(spotifyApi)
    }


    override fun close() {
        networkModule.close()
    }

    override val releaseGroupUseCase by lazy {
        useCaseModule.provideReleaseGroupUseCase(
            albumRepository,
            searchRepository
        )
    }

    override val trackRepository: TrackRepository by lazy {
        repositoryModule.provideTrackRepository(firestore, spotifyApi)
    }

    override val settingsRepository by lazy {
        settingsModule.provideSettingsRepository()
    }

    override val settingsViewModel by lazy {
        settingsModule.provideSettingsViewModel(settingsRepository, openAiApi)
    }

}