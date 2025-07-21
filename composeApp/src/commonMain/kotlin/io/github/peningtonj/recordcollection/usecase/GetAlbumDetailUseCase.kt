package io.github.peningtonj.recordcollection.usecase

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumCollectionUiState
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.models.TagUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class GetAlbumDetailUseCase(
    private val albumRepository: AlbumRepository,
    private val albumTagRepository: AlbumTagRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val trackRepository: TrackRepository,
    private val albumRatingRepository: RatingRepository
) {

    suspend fun execute(albumId: String, getTracks: Boolean = true, albumData: Album? = null): AlbumDetailUiState {
        val albumExistsInDb = albumRepository.albumExists(albumId)
        if (albumData != null) {
            return getDatabaseAlbum(albumData)
        }
        return if (albumExistsInDb) {
            val album = albumRepository.getAlbumById(albumId).first()
            getDatabaseAlbum(album)
        } else {
            getApiAlbumData(albumId, getTracks = getTracks)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getDatabaseAlbum(album: Album): AlbumDetailUiState {
        return combine(
            albumTagRepository.getTagsForAlbum(album.id),
            collectionAlbumRepository.getCollectionsForAlbum(album.id),
            trackRepository.getTracksForAlbum(album.id),
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
        }.first()
    }

    private suspend fun getApiAlbumData(albumId: String, getTracks: Boolean = true): AlbumDetailUiState {
        val apiAlbum = albumRepository.fetchAlbum(albumId)
        apiAlbum?.let {
            val apiTracks = if (getTracks) {
                trackRepository.fetchTracksForAlbum(apiAlbum)
            } else {
                emptyList()
            }
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
        throw (Error("Album not found"))
    }
    }
