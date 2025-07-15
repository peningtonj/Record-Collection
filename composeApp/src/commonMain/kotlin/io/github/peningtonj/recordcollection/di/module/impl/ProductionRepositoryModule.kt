
package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.everynoise.EveryNoiseApi
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.*

class ProductionRepositoryModule : RepositoryModule {
    override fun provideAuthRepository(
        authHandler: AuthHandler,
        database: RecordCollectionDatabase
    ): SpotifyAuthRepository = SpotifyAuthRepository(authHandler, database)
    
    override fun provideAlbumRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi,
        eventDispatcher: AlbumEventDispatcher
    ): AlbumRepository = AlbumRepository(database, spotifyApi, eventDispatcher)
    
    override fun provideArtistRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi,
        everyNoiseApi: EveryNoiseApi
    ): ArtistRepository = ArtistRepository(database, spotifyApi, everyNoiseApi)
    
    override fun providePlaybackRepository(
        spotifyApi: SpotifyApi
    ): PlaybackRepository = PlaybackRepository(spotifyApi)
    
    override fun provideProfileRepository(
        database: RecordCollectionDatabase
    ): ProfileRepository = ProfileRepository(database)

    override fun provideRatingRepository(
        database: RecordCollectionDatabase
    ): RatingRepository = RatingRepository(database)

    override fun provideAlbumCollectionRepository(
        database: RecordCollectionDatabase
    ): AlbumCollectionRepository = AlbumCollectionRepository(database)


    override fun provideCollectionAlbumRepository(
        database: RecordCollectionDatabase
    ): CollectionAlbumRepository = CollectionAlbumRepository(database)

    override fun provideAlbumTagRepository(
        database: RecordCollectionDatabase
    ): AlbumTagRepository = AlbumTagRepository(database)

    override fun provideTagRepository(
        database: RecordCollectionDatabase
    ): TagRepository = TagRepository(database)

    override fun provideSearchRepository(
        spotifyApi: SpotifyApi
    ): SearchRepository = SearchRepository(spotifyApi)

}