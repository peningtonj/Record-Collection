package io.github.peningtonj.recordcollection.di

import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.db.DatabaseHelper
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.BaseSpotifyAuthRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository

class BaseDependencyContainer(
    databaseDriver: DatabaseDriver,
    override val authHandler: AuthHandler
) : DependencyContainer {
    private val databaseHelper = DatabaseHelper(databaseDriver)
    override val profileRepository = ProfileRepository(databaseHelper.database)
    override val authRepository = BaseSpotifyAuthRepository(
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
}