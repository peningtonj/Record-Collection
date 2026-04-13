package io.github.peningtonj.recordcollection.di.module

import com.russhwolf.settings.Settings
import dev.gitlive.firebase.firestore.FirebaseFirestore
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
        settings: Settings
    ): SpotifyAuthRepository

    fun provideAlbumRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi,
        eventDispatcher: AlbumEventDispatcher
    ): AlbumRepository

    fun provideArtistRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi
    ): ArtistRepository

    fun providePlaybackRepository(
        spotifyApi: SpotifyApi
    ): PlaybackRepository

    fun provideProfileRepository(
        spotifyApi: SpotifyApi
    ): ProfileRepository

    fun provideRatingRepository(
        firestore: FirebaseFirestore
    ): RatingRepository

    fun provideAlbumCollectionRepository(
        firestore: FirebaseFirestore,
        openAiApi: OpenAiApi
    ): AlbumCollectionRepository

    fun provideCollectionAlbumRepository(
        firestore: FirebaseFirestore,
        albumRepository: AlbumRepository
    ): CollectionAlbumRepository

    fun provideAlbumTagRepository(
        firestore: FirebaseFirestore
    ): AlbumTagRepository

    fun provideTagRepository(
        firestore: FirebaseFirestore
    ): TagRepository

    fun provideSearchRepository(
        spotifyApi: SpotifyApi
    ): SearchRepository

    fun providePlaylistRepository(
        spotifyApi: SpotifyApi
    ): PlaylistRepository

    fun provideTrackRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi
    ): TrackRepository

}