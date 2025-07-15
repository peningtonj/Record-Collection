package io.github.peningtonj.recordcollection.di.container

import PlaybackQueueService
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.PlaybackRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import io.github.peningtonj.recordcollection.service.ArticleImportService
import io.github.peningtonj.recordcollection.service.CollectionsService
import io.github.peningtonj.recordcollection.service.LibraryService
import io.github.peningtonj.recordcollection.service.TagService
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase

interface DependencyContainer : AutoCloseable {
    val profileRepository: ProfileRepository
    val authHandler: AuthHandler
    val authRepository: SpotifyAuthRepository
    val albumRepository: AlbumRepository
    val playbackRepository: PlaybackRepository
    val artistRepository: ArtistRepository
    val libraryService: LibraryService
    val tagService: TagService
    val ratingRepository: RatingRepository
    val collectionAlbumRepository: CollectionAlbumRepository
    val albumCollectionRepository: AlbumCollectionRepository

    val collectionsService: CollectionsService
    val albumTagRepository: AlbumTagRepository
    val tagRepository: TagRepository
    val albumDetailUseCase : GetAlbumDetailUseCase
    val playbackQueueService: PlaybackQueueService
    val searchRepository: SearchRepository
    val openAiApi: OpenAiApi
    val articleImportService: ArticleImportService
}