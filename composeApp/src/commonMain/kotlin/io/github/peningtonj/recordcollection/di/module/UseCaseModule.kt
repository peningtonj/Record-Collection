// di/module/UseCaseModule.kt
package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase

interface UseCaseModule {
    fun provideGetAlbumDetailUseCase(
        albumRepository: AlbumRepository,
        albumTagRepository: AlbumTagRepository,
        collectionAlbumRepository: CollectionAlbumRepository,
        albumRatingRepository: RatingRepository
    ): GetAlbumDetailUseCase
}