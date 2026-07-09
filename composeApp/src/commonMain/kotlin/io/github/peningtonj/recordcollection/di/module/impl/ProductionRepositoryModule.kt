package io.github.peningtonj.recordcollection.di.module.impl

import com.russhwolf.settings.Settings
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.di.module.RepositoryModule
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.*

class ProductionRepositoryModule : RepositoryModule {

    override fun provideUserSessionRepository(settings: Settings): UserSessionRepository =
        UserSessionRepository(settings)

    override fun provideUserLibraryRepository(
        firestore: FirebaseFirestore,
        userSession: UserSessionRepository
    ): UserLibraryRepository = UserLibraryRepository(firestore, userSession)

    override fun provideAuthRepository(
        authHandler: AuthHandler,
        settings: Settings
    ): SpotifyAuthRepository = SpotifyAuthRepository(authHandler, settings)

    override fun provideAlbumRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi,
        eventDispatcher: AlbumEventDispatcher,
        userLibraryRepository: UserLibraryRepository
    ): AlbumRepository = AlbumRepository(firestore, spotifyApi, miscApi, eventDispatcher, userLibraryRepository)

    override fun provideArtistRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi
    ): ArtistRepository = ArtistRepository(firestore, spotifyApi, miscApi)

    override fun providePlaybackRepository(
        spotifyApi: SpotifyApi
    ): PlaybackRepository = PlaybackRepository(spotifyApi)

    override fun provideProfileRepository(
        spotifyApi: SpotifyApi
    ): ProfileRepository = ProfileRepository(spotifyApi)

    override fun provideRatingRepository(
        userLibraryRepository: UserLibraryRepository
    ): RatingRepository = RatingRepository(userLibraryRepository)

    override fun provideAlbumCollectionRepository(
        firestore: FirebaseFirestore,
        openAiApi: OpenAiApi,
        userSession: UserSessionRepository
    ): AlbumCollectionRepository = AlbumCollectionRepository(firestore, openAiApi, userSession)

    override fun provideCollectionAlbumRepository(
        firestore: FirebaseFirestore,
        albumRepository: AlbumRepository,
        userSession: UserSessionRepository,
        userLibraryRepository: UserLibraryRepository
    ): CollectionAlbumRepository = CollectionAlbumRepository(firestore, albumRepository, userSession, userLibraryRepository)

    override fun provideAlbumTagRepository(
        firestore: FirebaseFirestore,
        userLibraryRepository: UserLibraryRepository,
        userSession: UserSessionRepository
    ): AlbumTagRepository = AlbumTagRepository(firestore, userLibraryRepository, userSession)

    override fun provideTagRepository(
        firestore: FirebaseFirestore,
        userSession: UserSessionRepository
    ): TagRepository = TagRepository(firestore, userSession)

    override fun provideSearchRepository(
        spotifyApi: SpotifyApi
    ): SearchRepository = SearchRepository(spotifyApi)

    override fun providePlaylistRepository(
        spotifyApi: SpotifyApi
    ): PlaylistRepository = PlaylistRepository(spotifyApi)

    override fun provideTrackRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi
    ): TrackRepository = TrackRepository(firestore, spotifyApi)
}