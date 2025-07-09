
package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
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
        spotifyApi: SpotifyApi
    ): AlbumRepository = AlbumRepository(database, spotifyApi)
    
    override fun provideArtistRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi
    ): ArtistRepository = ArtistRepository(database, spotifyApi)
    
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



}