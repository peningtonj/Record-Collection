package io.github.peningtonj.recordcollection.usecase

import io.github.peningtonj.recordcollection.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.repository.UserLibraryRepository
import io.github.peningtonj.recordcollection.repository.UserLibraryRepository.LibraryAlbumDocument
import io.github.peningtonj.recordcollection.ui.models.AlbumCollectionUiState
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.ui.models.TagUiState
import io.github.peningtonj.recordcollection.util.DomainException
import io.github.aakira.napier.Napier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class GetAlbumDetailUseCase(
    private val albumRepository: AlbumRepository,
    private val albumTagRepository: AlbumTagRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val trackRepository: TrackRepository,
    private val userLibraryRepository: UserLibraryRepository,
) {

    suspend fun execute(albumId: String, spotifyId: String, getTracks: Boolean = true): Flow<AlbumDetailUiState> {
        val albumExistsInDb = albumRepository.albumExists(albumId)
        return if (albumExistsInDb) {
            getDatabaseAlbum(albumId)
        } else {
            getApiAlbumData(spotifyId, getTracks = getTracks)
        }
    }

    /**
     * A live [LibraryAlbumDocument]`?` for [albumId], falling back to a Spotify-ID match
     * if the internal id doesn't hit. Both sources are itself live (Firestore snapshot
     * listeners), so rating/inLibrary changes made anywhere in the app — including from
     * this same screen's own buttons — show up here without the caller having to re-fetch.
     *
     * The internal id is tried first because it collapses remasters/deluxe/regional
     * variants onto one identity by design (TECH_DEBT 2.2's "Identity model" note), which
     * a Spotify ID does not: the artist/search endpoints can return a different catalog
     * entry for the "same" album than whichever one the user actually saved. The Spotify
     * ID match only matters for a library entry that hasn't been re-keyed onto the
     * current id scheme yet.
     */
    private fun libraryEntryFlow(albumId: String, spotifyId: String): Flow<LibraryAlbumDocument?> =
        combine(
            userLibraryRepository.getLibraryEntry(albumId),
            userLibraryRepository.getLibraryEntryBySpotifyId(spotifyId),
        ) { byId, bySpotifyId -> byId ?: bySpotifyId }

    /**
     * Streams a live [AlbumDetailUiState] for an album that exists in Firestore.
     *
     * The user library entry provides rating and inLibrary status; all other sources
     * remain unchanged. Using combine(5) keeps a single reactive stream.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getDatabaseAlbum(albumId: String): Flow<AlbumDetailUiState> {
        return albumRepository.getAlbumById(albumId).filterNotNull().flatMapLatest { album ->
            val tracks = trackRepository.getAlbumTracks(album)
            combine(
                albumTagRepository.getTagsForAlbum(album.id),
                collectionAlbumRepository.getCollectionsForAlbum(album.id),
                albumRepository.getAlbumsFromReleaseGroup(album.releaseGroupId),
                libraryEntryFlow(album.id, album.spotifyId)
            ) { tags, collections, releaseGroup, libraryEntry ->
                val enrichedAlbum = album.copy(
                    inLibrary = libraryEntry?.inLibrary ?: false,
                    rating = libraryEntry?.rating
                )
                AlbumDetailUiState(
                    album = enrichedAlbum,
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
                    rating = libraryEntry?.rating,
                    isLoading = false,
                    error = null,
                    releaseGroup = releaseGroup
                )
            }
        }
    }

    /**
     * Streams a live [AlbumDetailUiState] for an album with no `albums/{id}` catalog doc
     * yet (a fresh add-to-library never writes one — see
     * AlbumRepository.addAlbumToLibrary). Metadata/tracks are fetched from Spotify once
     * (they don't change), but inLibrary/rating are a live [libraryEntryFlow] — this used
     * to be a one-shot lookup, so the rating and add/remove-from-library buttons on this
     * screen updated Firestore correctly but the screen itself never noticed until it was
     * re-opened.
     */
    private suspend fun getApiAlbumData(spotifyId: String, getTracks: Boolean = true): Flow<AlbumDetailUiState> {
        val apiAlbum = albumRepository.fetchAlbum(spotifyId).getOrElse { cause ->
            Napier.w("fetchAlbum($spotifyId) failed", cause)
            throw DomainException.AlbumNotFoundException(spotifyId)
        }
        val apiTracks = if (getTracks) trackRepository.fetchTracksForAlbum(apiAlbum) else emptyList()

        return libraryEntryFlow(apiAlbum.id, apiAlbum.spotifyId).map { libraryEntry ->
            val enrichedAlbum = apiAlbum.copy(
                inLibrary = libraryEntry?.inLibrary ?: false,
                rating = libraryEntry?.rating,
            )
            AlbumDetailUiState(
                album = enrichedAlbum,
                tags = emptyList(),
                collections = emptyList(),
                tracks = apiTracks,
                totalDuration = apiTracks.sumOf { it.durationMs },
                rating = libraryEntry?.rating,
                isLoading = false,
                error = null,
                releaseGroup = emptyList()
            )
        }
    }
}
