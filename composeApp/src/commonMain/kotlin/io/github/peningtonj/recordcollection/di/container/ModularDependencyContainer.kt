package io.github.peningtonj.recordcollection.di.container

import PlaybackQueueService
import io.github.peningtonj.recordcollection.di.module.DatabaseModule
import io.github.peningtonj.recordcollection.di.module.EventModule
import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
import io.github.peningtonj.recordcollection.di.module.UseCaseModule
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.repository.*
import io.github.peningtonj.recordcollection.service.CollectionsService
import io.github.peningtonj.recordcollection.service.LibraryService
import io.github.peningtonj.recordcollection.service.TagService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ModularDependencyContainer(
    private val networkModule: NetworkModule,
    private val databaseModule: DatabaseModule,
    private val repositoryModule: RepositoryModule,
    override val authHandler: AuthHandler,
    private val useCaseModule: UseCaseModule,
    private val eventModule: EventModule,
) : DependencyContainer {

    private val database by lazy { databaseModule.provideDatabase() }
    
    // Create event scope
    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override val authRepository by lazy {
        repositoryModule.provideAuthRepository(
            authHandler = authHandler,
            database = database
        )
    }

    private val spotifyApi by lazy {
        networkModule.provideSpotifyApi(authRepository)
    }

    private val everyNoiseApi by lazy {
        networkModule.provideEveryNoiseApi()
    }
    
    // Create the event dispatcher
    private val albumEventDispatcher by lazy {
        val tagService = eventModule.provideTagService()
        val albumTagRepository = repositoryModule.provideAlbumTagRepository(database)
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
            database = database,
            spotifyApi = spotifyApi,
            eventDispatcher = albumEventDispatcher
        )
    }
    
    override val artistRepository by lazy {
        repositoryModule.provideArtistRepository(
            database = database,
            spotifyApi = spotifyApi,
            everyNoiseApi = everyNoiseApi
        )
    }
    
    override val playbackRepository by lazy {
        repositoryModule.providePlaybackRepository(spotifyApi)
    }
    
    override val profileRepository by lazy {
        repositoryModule.provideProfileRepository(database)
    }
    
    override val libraryService by lazy {
        LibraryService(albumRepository, artistRepository, ratingRepository)
    }

    override val playbackQueueService by lazy {
        PlaybackQueueService(
            playbackRepository,
            albumRepository
        )
    }

    override val tagService by lazy {
        TagService()
    }

    override val ratingRepository by lazy {
        repositoryModule.provideRatingRepository(database)
    }
    
    override val albumCollectionRepository by lazy {
        repositoryModule.provideAlbumCollectionRepository(database)
    }
    
    override val collectionAlbumRepository by lazy {
        repositoryModule.provideCollectionAlbumRepository(database)
    }

    override val albumTagRepository by lazy {
        repositoryModule.provideAlbumTagRepository(database)
    }

    override val tagRepository by lazy {
        repositoryModule.provideTagRepository(database)
    }

    override val albumDetailUseCase by lazy {
        useCaseModule.provideGetAlbumDetailUseCase(
            albumRepository,
            albumTagRepository,
            collectionAlbumRepository,
            ratingRepository
        )
    }

    override val collectionsService: CollectionsService by lazy {
        CollectionsService(collectionAlbumRepository, albumCollectionRepository, albumRepository)
    }

    override val searchRepository by lazy {
        SearchRepository(spotifyApi)
    }

    override val openAiApi: OpenAiApi by lazy {
        networkModule.provideOpenAiApi()
    }

    override fun close() {
        networkModule.close()
        databaseModule.close()
    }
}