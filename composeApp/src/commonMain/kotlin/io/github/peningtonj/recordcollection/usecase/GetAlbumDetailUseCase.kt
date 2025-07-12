package io.github.peningtonj.recordcollection.usecase

import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumCollectionUiState
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.models.TagUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class GetAlbumDetailUseCase(
    private val albumRepository: AlbumRepository,
    private val albumTagRepository: AlbumTagRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val albumRatingRepository: RatingRepository
) {

    fun execute(albumId: String): Flow<AlbumDetailUiState> {
        return combine(
            albumRepository.getAlbumById(albumId),
            albumTagRepository.getTagsForAlbum(albumId),
            collectionAlbumRepository.getCollectionsForAlbum(albumId),
            albumRepository.getTracksForAlbum(albumId),
            albumRatingRepository.getAlbumRating(albumId)
        ) { album, tags, collections, tracks, rating ->
            AlbumDetailUiState(
                album = album,
                tags = tags.map { TagUiState(it) },
                collections = collections.map { AlbumCollectionUiState(
                    it.collection,
                    position = it.position,
                    addedAt = it.addedAt,
                ) },
                tracks = tracks,
                totalDuration = tracks.sumOf { it.durationMs },
                rating = rating,
                isLoading = false,
                error = null
            )
        }
    }
}