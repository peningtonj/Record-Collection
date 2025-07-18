package io.github.peningtonj.recordcollection.usecase

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumCollectionUiState
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.models.TagUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GetAlbumDetailUseCase(
    private val albumRepository: AlbumRepository,
    private val albumTagRepository: AlbumTagRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val albumRatingRepository: RatingRepository
) {

    fun execute(albumId: String): Flow<AlbumDetailUiState> {
        return flow {
            // Check if album exists in database
            val albumExistsInDb = albumRepository.albumExists(albumId)
            
            if (albumExistsInDb) {
                val album = albumRepository.getAlbumById(albumId).first()
                // Use database data
                getDatabaseAlbumFlow(album).collect { emit(it) }
            } else {
                // Fetch from API
                getApiAlbumData(albumId).collect { emit(it) }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getDatabaseAlbumFlow(album: Album): Flow<AlbumDetailUiState> {
        return combine(
            albumTagRepository.getTagsForAlbum(album.id),
            collectionAlbumRepository.getCollectionsForAlbum(album.id),
            albumRepository.getTracksForAlbum(album.id),
            albumRatingRepository.getAlbumRating(album.id),
            albumRepository.getAlbumsFromReleaseGroup(album.releaseGroupId)
        ) { tags, collections, tracks, rating, releaseGroup ->
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
                error = null,
                releaseGroup = releaseGroup
            )
        }
    }

    private fun getApiAlbumData(albumId: String): Flow<AlbumDetailUiState> {
        return flow {
            try {
                val apiAlbum = albumRepository.fetchAlbum(albumId)
                apiAlbum?.let {
                    val apiTracks = albumRepository.fetchTracksForAlbum(apiAlbum)

                    emit(
                        AlbumDetailUiState(
                            album = apiAlbum,
                            tags = emptyList(),
                            collections = emptyList(),
                            tracks = apiTracks,
                            totalDuration = apiTracks.sumOf { it.durationMs },
                            rating = null,
                            isLoading = false,
                            error = null,
                            releaseGroup = emptyList()
                        )
                    )
                }
            } catch (e: Exception) {
                Napier.e(e) { "Error fetching album $albumId from API" }
            }
        }
    }
}