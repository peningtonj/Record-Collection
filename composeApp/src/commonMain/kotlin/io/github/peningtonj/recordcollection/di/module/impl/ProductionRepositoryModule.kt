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
    override fun provideAuthRepository(
        authHandler: AuthHandler,
        settings: Settings
    ): SpotifyAuthRepository = SpotifyAuthRepository(authHandler, settings)
    
    override fun provideAlbumRepository(
        firestore: FirebaseFirestore,
        spotifyApi: SpotifyApi,
        miscApi: MiscApi,
        eventDispatcher: AlbumEventDispatcher
    ): AlbumRepository = AlbumRepository(firestore, spotifyApi, miscApi, eventDispatcher)
    
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
        firestore: FirebaseFirestore
    ): RatingRepository = RatingRepository(firestore)

    override fun provideAlbumCollectionRepository(
        firestore: FirebaseFirestore,
        openAiApi: OpenAiApi
    ): AlbumCollectionRepository = AlbumCollectionRepository(firestore, openAiApi)

    override fun provideCollectionAlbumRepository(
        firestore: FirebaseFirestore,
        albumRepository: AlbumRepository
    ): CollectionAlbumRepository = CollectionAlbumRepository(firestore, albumRepository)

    override fun provideAlbumTagRepository(
        firestore: FirebaseFirestore
    ): AlbumTagRepository = AlbumTagRepository(firestore)

    override fun provideTagRepository(
        firestore: FirebaseFirestore
    ): TagRepository = TagRepository(firestore)

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