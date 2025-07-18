
package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
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
        miscApi: MiscApi,
        eventDispatcher: AlbumEventDispatcher
    ): AlbumRepository = AlbumRepository(database, spotifyApi, miscApi, eventDispatcher)
    
    override fun provideArtistRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi
    ): ArtistRepository = ArtistRepository(database, spotifyApi, miscApi)
    
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
        database: RecordCollectionDatabase,
        openAiApi: OpenAiApi
    ): AlbumCollectionRepository = AlbumCollectionRepository(database, openAiApi)


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