package io.github.peningtonj.recordcollection.usecase

import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumCollectionUiState
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.models.TagUiState
import io.github.peningtonj.recordcollection.util.DomainException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class GetAlbumDetailUseCase(
    private val albumRepository: AlbumRepository,
    private val albumTagRepository: AlbumTagRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val trackRepository: TrackRepository,
) {

    suspend fun execute(albumId: String, spotifyId: String, getTracks: Boolean = true): Flow<AlbumDetailUiState> {
        val albumExistsInDb = albumRepository.albumExists(albumId)
        return if (albumExistsInDb) {
            getDatabaseAlbum(albumId)
        } else {
            flowOf(getApiAlbumData(spotifyId, getTracks = getTracks))
        }
    }

    /**
     * Streams a live [AlbumDetailUiState] for an album that exists in Firestore.
     *
     * [albumRepository.getAlbumById] is the primary source-of-truth snapshot; wrapping
     * the inner combine in [flatMapLatest] means that any change to the album document
     * (including a rating update) causes all dependent flows to re-subscribe automatically.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getDatabaseAlbum(albumId: String): Flow<AlbumDetailUiState> {
        return albumRepository.getAlbumById(albumId).flatMapLatest { album ->
            combine(
                albumTagRepository.getTagsForAlbum(album.id),
                collectionAlbumRepository.getCollectionsForAlbum(album.id),
                trackRepository.getTracksForAlbum(album.id),
                albumRepository.getAlbumsFromReleaseGroup(album.releaseGroupId)
            ) { tags, collections, tracks, releaseGroup ->
                AlbumDetailUiState(
                    album = album,
                    tags = tags.map { TagUiState(it) },
                    collections = collections.map {
                        AlbumCollectionUiState(
                            it.collection,
                            position = it.position,
                            addedAt = it.addedAt,
                        )
                    },
                    tracks = tracks,
                    totalDuration = tracks.sumOf { it.durationMs },
                    rating = album.rating,
                    isLoading = false,
                    error = null,
                    releaseGroup = releaseGroup
                )
            }
        }
    }

    private suspend fun getApiAlbumData(albumId: String, getTracks: Boolean = true): AlbumDetailUiState {
        val apiAlbum = albumRepository.fetchAlbum(albumId)
        apiAlbum?.let {
            val apiTracks = if (getTracks) trackRepository.fetchTracksForAlbum(apiAlbum) else emptyList()
            return AlbumDetailUiState(
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
        }
        throw DomainException.AlbumNotFoundException(albumId)
    }
}

