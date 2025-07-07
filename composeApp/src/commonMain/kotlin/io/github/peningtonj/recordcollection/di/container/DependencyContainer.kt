package io.github.peningtonj.recordcollection.di.container

import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.github.peningtonj.recordcollection.service.LibraryService

interface DependencyContainer : AutoCloseable {
    val profileRepository: ProfileRepository
    val authHandler: AuthHandler
    val authRepository: SpotifyAuthRepository
    val albumRepository: AlbumRepository
    val playbackRepository: PlaybackRepository
    val artistRepository: ArtistRepository
    val libraryService: LibraryService
}