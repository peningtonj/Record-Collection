package io.github.peningtonj.recordcollection.di

import DatabaseHelper
import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.service.LibraryService

class BaseDependencyContainer(
    databaseDriver: DatabaseDriver,
    override val authHandler: AuthHandler
) : DependencyContainer {
    private val databaseHelper = DatabaseHelper(databaseDriver)
    override val profileRepository = ProfileRepository(databaseHelper.database)
    override val authRepository = SpotifyAuthRepository(
        authHandler = authHandler,
        database = databaseHelper.database
    )

    private val spotifyApi = SpotifyApi(
        client = HttpClientProvider.create(),
        authRepository = authRepository
    )

    override val albumRepository = AlbumRepository(
        database = databaseHelper.database,
        spotifyApi = spotifyApi
    )

    override val playbackRepository = PlaybackRepository(
        spotifyApi = spotifyApi
    )

    override val artistRepository = ArtistRepository(
        database = databaseHelper.database,
        spotifyApi = spotifyApi,
    )

    override val libraryService = LibraryService(
        albumRepository,
        artistRepository
    )

    override fun close() {
        HttpClientProvider.close()
    }

}