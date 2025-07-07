
package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.*

interface RepositoryModule {
    fun provideAuthRepository(
        authHandler: AuthHandler,
        database: RecordCollectionDatabase
    ): SpotifyAuthRepository
    
    fun provideAlbumRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi
    ): AlbumRepository
    
    fun provideArtistRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi
    ): ArtistRepository
    
    fun providePlaybackRepository(
        spotifyApi: SpotifyApi
    ): PlaybackRepository
    
    fun provideProfileRepository(
        database: RecordCollectionDatabase
    ): ProfileRepository
}