package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.di.module.UseCaseModule
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase

class ProductionUseCaseModule : UseCaseModule {
    override fun provideGetAlbumDetailUseCase(
        albumRepository: AlbumRepository,
        albumTagRepository: AlbumTagRepository,
        collectionAlbumRepository: CollectionAlbumRepository,
        albumRatingRepository: RatingRepository
    ): GetAlbumDetailUseCase {
        return GetAlbumDetailUseCase(
            albumRepository = albumRepository,
            albumTagRepository = albumTagRepository,
            collectionAlbumRepository = collectionAlbumRepository,
            albumRatingRepository = albumRatingRepository
        )
    }
}