package io.github.peningtonj.recordcollection.di.container

import io.github.peningtonj.recordcollection.di.module.DatabaseModule
import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.repository.*
import io.github.peningtonj.recordcollection.service.CollectionsService
import io.github.peningtonj.recordcollection.service.LibraryService

class ModularDependencyContainer(
    private val networkModule: NetworkModule,
    private val databaseModule: DatabaseModule,
    private val repositoryModule: RepositoryModule,
    override val authHandler: AuthHandler,
) : DependencyContainer {

    private val database by lazy { databaseModule.provideDatabase() }
    
    override val authRepository by lazy {
        repositoryModule.provideAuthRepository(
            authHandler = authHandler,
            database = database
        )
    }

    private val spotifyApi by lazy {
        networkModule.provideSpotifyApi(authRepository)
    }
    
    override val albumRepository by lazy {
        repositoryModule.provideAlbumRepository(
            database = database,
            spotifyApi = spotifyApi
        )
    }
    
    override val artistRepository by lazy {
        repositoryModule.provideArtistRepository(
            database = database,
            spotifyApi = spotifyApi
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

    override val ratingRepository by lazy {
        repositoryModule.provideRatingRepository(database)
    }
    
    override val albumCollectionRepository by lazy {
        repositoryModule.provideAlbumCollectionRepository(database)
    }
    
    override val collectionAlbumRepository by lazy {
        repositoryModule.provideCollectionAlbumRepository(database)
    }

    override val collectionsService: CollectionsService by lazy {
        CollectionsService(collectionAlbumRepository, albumCollectionRepository, albumRepository)
    }

    override fun close() {
        networkModule.close()
        databaseModule.close()
    }
}