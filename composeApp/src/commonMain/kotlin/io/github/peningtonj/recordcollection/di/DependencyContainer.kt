package io.github.peningtonj.recordcollection.di

import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.BaseSpotifyAuthRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository

interface DependencyContainer {
    val profileRepository: ProfileRepository
    val authHandler: AuthHandler
    val authRepository: BaseSpotifyAuthRepository
    val albumRepository: AlbumRepository
    // Add this
}
