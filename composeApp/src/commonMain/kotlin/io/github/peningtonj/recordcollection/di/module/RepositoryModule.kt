
package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.*

interface RepositoryModule {
    fun provideAuthRepository(
        authHandler: AuthHandler,
        database: RecordCollectionDatabase
    ): SpotifyAuthRepository
    
    fun provideAlbumRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi,
        eventDispatcher: AlbumEventDispatcher
    ): AlbumRepository
    
    fun provideArtistRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi
    ): ArtistRepository
    
    fun providePlaybackRepository(
        spotifyApi: SpotifyApi
    ): PlaybackRepository
    
    fun provideProfileRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi
    ): ProfileRepository

    fun provideRatingRepository(
        database: RecordCollectionDatabase
    ): RatingRepository

    fun provideAlbumCollectionRepository(
        database: RecordCollectionDatabase,
        openAiApi: OpenAiApi
    ): AlbumCollectionRepository

    fun provideCollectionAlbumRepository(
        database: RecordCollectionDatabase
    ): CollectionAlbumRepository

    fun provideAlbumTagRepository(
        database: RecordCollectionDatabase
    ): AlbumTagRepository

    fun provideTagRepository(
        database: RecordCollectionDatabase
    ): TagRepository

    fun provideSearchRepository(
        spotifyApi: SpotifyApi
    ): SearchRepository

    fun providePlaylistRepository(
        spotifyApi: SpotifyApi
    ): PlaylistRepository

    fun provideTrackRepository(
        database: RecordCollectionDatabase,
        spotifyApi: SpotifyApi
    ): TrackRepository
}