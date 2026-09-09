package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.network.spotify.AlbumResult
import io.github.peningtonj.recordcollection.network.spotify.Playlist
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumDocument
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.events.AlbumEvent
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.miscApi.model.MusicBrainzResponse
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems
import io.github.peningtonj.recordcollection.util.LoggingUtils
import io.github.peningtonj.recordcollection.util.resultOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class AlbumRepository(
    private val firestore: FirebaseFirestore,
    private val spotifyApi: SpotifyApi,
    private val miscApi: MiscApi,
    private val eventDispatcher: AlbumEventDispatcher,
    private val userLibraryRepository: UserLibraryRepository
) {
    private val albumsRef = firestore.collection("albums")

    /** Maps a Firestore DocumentSnapshot to an Album, using the document ID as the album ID. */
    private fun DocumentSnapshot.toAlbum(): Album? =
        runCatching { data<AlbumDocument>().copy(id = id).let { AlbumMapper.toDomain(it) } }
            .onFailure { Napier.e("Failed to deserialize album doc '$id': ${it.message}") }
            .getOrNull()

    /**
     * FIRESTORE OPERATIONS
     */

    /**
     * Writes the album's metadata document. `addedAt` is set only on first insert —
     * a re-sync must not reset it, or `SortOrder.DATE_ADDED` breaks. `updatedAt` is
     * bumped on every write.
     */
    private suspend fun writeAlbumDocument(album: Album) {
        val docRef = albumsRef.document(album.id)
        val existingAddedAt = runCatching {
            docRef.get().takeIf { it.exists }?.data<AlbumDocument>()?.addedAt
        }.getOrNull()
        val now = Clock.System.now()
        // Plain-primitive map, not set(AlbumDocument): GitLive's JS SDK rejects the
        // boxed Long in total_tracks / updated_at. See db/FirestoreMap.kt.
        docRef.set(
            AlbumMapper.toDocumentMap(album) + mapOf(
                "added_at" to (existingAddedAt ?: now.toString()),
                "updated_at" to now.toEpochMilliseconds().toDouble(),
            )
        )
    }

    suspend fun saveAlbum(album: AlbumDto, addToUsersLibrary: Boolean = true) {
        val domainAlbum = AlbumMapper.toDomain(album)
        LoggingUtils.d(
            LoggingUtils.Category.REPOSITORY,
            "Saving album: ${album.name} by ${album.artists.firstOrNull()?.name} (ID: ${domainAlbum.id}, inLibrary: $addToUsersLibrary)"
        )
        LoggingUtils.logFirebaseWrite("albums", "set", domainAlbum.id, mapOf("name" to album.name))
        writeAlbumDocument(domainAlbum)
        if (addToUsersLibrary) {
            userLibraryRepository.addToLibrary(domainAlbum)
        }
        eventDispatcher.dispatch(AlbumEvent.AlbumAdded(domainAlbum))
    }

    suspend fun saveAlbum(album: Album, overrideInLibrary: Boolean? = null) {
        val addToLibrary = overrideInLibrary ?: album.inLibrary
        LoggingUtils.d(
            LoggingUtils.Category.REPOSITORY,
            "Saving album: ${album.name} by ${album.artists.firstOrNull()?.name} (ID: ${album.id}, inLibrary: $addToLibrary)"
        )
        LoggingUtils.logFirebaseWrite("albums", "set", album.id, mapOf("name" to album.name))
        writeAlbumDocument(album)
        if (addToLibrary) {
            userLibraryRepository.addToLibrary(album)
        }
        eventDispatcher.dispatch(AlbumEvent.AlbumAdded(album))
    }

    suspend fun albumExists(albumId: String): Boolean {
        LoggingUtils.logFirebaseQuery("albums", "get (albumExists)", mapOf("id" to albumId))
        return albumsRef.document(albumId).get().exists
    }

    fun getAlbumByNameAndArtistIfPresent(name: String, artistName: String): Flow<Album?> {
        LoggingUtils.logFirebaseQuery("albums", "snapshots by name+artist", mapOf("name" to name, "artist" to artistName))
        return albumsRef
            .where { ("name" equalTo name) and ("primary_artist" equalTo artistName) }
            .snapshots
            .map { snapshot ->
                LoggingUtils.logFirebaseResult("albums", "getAlbumByNameAndArtistIfPresent", snapshot.documents.size)
                snapshot.documents.firstOrNull()?.toAlbum()
            }
    }

    /**
     * Streams the album with [id], or `null` if it does not exist / fails to deserialize.
     * Never throws inside the flow — collectors decide how to handle a missing album.
     */
    fun getAlbumById(id: String): Flow<Album?> {
        LoggingUtils.logFirebaseQuery("albums", "snapshot by id", mapOf("id" to id))
        return albumsRef.document(id).snapshots
            .map { snapshot ->
                if (!snapshot.exists) {
                    Napier.w("Album with id '$id' not found")
                    null
                } else {
                    snapshot.toAlbum()
                }
            }
    }

    /**
     * Fetches multiple albums in a single batched Firestore query using whereIn on document IDs.
     * Handles the 30-document Firestore whereIn limit by chunking and combining.
     */
    fun getAlbumsByIds(ids: List<String>): Flow<List<Album>> {
        if (ids.isEmpty()) return flowOf(emptyList())
        val chunks = ids.chunked(30)
        LoggingUtils.logFirebaseQuery("albums", "snapshots whereIn (${ids.size} ids, ${chunks.size} chunks)")
        val chunkFlows = chunks.map { chunk ->
            albumsRef
                .where { FieldPath.documentId inArray chunk }
                .snapshots
                .map { snapshot ->
                    LoggingUtils.logFirebaseResult("albums", "getAlbumsByIds chunk", snapshot.documents.size)
                    snapshot.documents.mapNotNull { it.toAlbum() }
                }
        }
        return if (chunkFlows.size == 1) {
            chunkFlows.first()
        } else {
            combine(chunkFlows) { results -> results.flatMap { it } }
        }
    }

    suspend fun getAlbumBySpotifyId(spotifyId: String): Album? {
        LoggingUtils.logFirebaseQuery("albums", "get by spotify_id", mapOf("spotifyId" to spotifyId))
        return albumsRef
            .where { "spotify_id" equalTo spotifyId }
            .get()
            .documents
            .firstOrNull()
            ?.toAlbum()
    }

    fun getEarliestReleaseDate(): Flow<LocalDate?> = getAllAlbumsInLibrary()
        .map { albums -> albums.minOfOrNull { it.releaseDate } }

    /**
     * Every document in the shared `albums` collection. This is the *whole catalogue*
     * (everything anyone has ever added or searched), not the current user's library —
     * it grows unbounded, so prefer [getAllAlbumsInLibrary] for anything user-facing.
     */
    fun getAllAlbums(): Flow<List<Album>> {
        LoggingUtils.logFirebaseQuery("albums", "snapshots (all)")
        return albumsRef.snapshots
            .map { snapshot ->
                LoggingUtils.logFirebaseResult("albums", "getAllAlbums", snapshot.documents.size)
                snapshot.documents.mapNotNull { it.toAlbum() }
            }
    }

    /**
     * All albums in the current user's library, rendered from the denormalised projection
     * on `users/{uid}/library_albums` — **one** collection listener, no join against the
     * shared `albums` catalogue. See docs/DATA_MODEL.md.
     *
     * Transition fallback: entries written before the projection existed (blank `name`)
     * are hydrated from the shared `albums` collection until `backfill_library_projection.py`
     * runs; after that the fallback branch is dead.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllAlbumsInLibrary(): Flow<List<Album>> =
        userLibraryRepository.getAllLibraryEntries().flatMapLatest { entries ->
            val inLibrary = entries.filter { it.inLibrary }
            if (inLibrary.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val staleIds = inLibrary.filter { it.name.isBlank() }.map { it.albumId }
            if (staleIds.isEmpty()) {
                flowOf(inLibrary.map { it.toDomainAlbum() })
            } else {
                Napier.w("${staleIds.size} library entries lack a projection — hydrating from `albums` (run backfill_library_projection.py)")
                getAlbumsByIds(staleIds).map { fetched ->
                    val fetchedById = fetched.associateBy { it.id }
                    inLibrary.map { entry ->
                        if (entry.name.isNotBlank()) entry.toDomainAlbum()
                        else fetchedById[entry.albumId]?.copy(inLibrary = true, rating = entry.rating)
                            ?: entry.toDomainAlbum()
                    }
                }
            }
        }

    private fun UserLibraryRepository.LibraryAlbumDocument.toDomainAlbum(): Album =
        AlbumMapper.libraryProjectionToDomain(
            albumId = albumId, name = name, primaryArtist = primaryArtist, artistsJson = artists,
            releaseDate = releaseDate, albumType = albumType, totalTracks = totalTracks,
            spotifyId = spotifyId, spotifyUri = spotifyUri, imageUrl = imageUrl,
            rating = rating, addedAt = addedAt,
        )

    /** Distinct primary-artist names across the current user's library, sorted. */
    fun getAllArtists(): Flow<List<String>> =
        getAllAlbumsInLibrary().map { albums -> albums.map { it.primaryArtist }.distinct().sorted() }

    fun getLibraryCount(): Flow<Long> =
        getAllAlbumsInLibrary().map { it.size.toLong() }

    fun getAlbumsByArtist(artistName: String): Flow<List<Album>> {
        LoggingUtils.logFirebaseQuery("albums", "snapshots by artist", mapOf("artist" to artistName))
        return albumsRef
            .where { "primary_artist" equalTo artistName }
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toAlbum() } }
    }

    suspend fun addAlbumToLibrary(album: Album) {
        userLibraryRepository.addToLibrary(album)
    }

    suspend fun removeAlbumFromLibrary(albumId: String) {
        userLibraryRepository.removeFromLibrary(albumId)
    }

    suspend fun updateReleaseGroupId(albumId: String, releaseGroupId: String) {
        LoggingUtils.logFirebaseWrite("albums", "set merge (updateReleaseGroupId)", albumId)
        albumsRef.document(albumId).set(mapOf("release_group_id" to releaseGroupId), merge = true)
    }

    fun getAlbumsFromReleaseGroup(releaseGroupId: String?): Flow<List<Album>> {
        return if (releaseGroupId == null) {
            flowOf(emptyList())
        } else {
            LoggingUtils.logFirebaseQuery("albums", "snapshots by release_group_id", mapOf("id" to releaseGroupId))
            albumsRef
                .where { "release_group_id" equalTo releaseGroupId }
                .snapshots
                .map { snapshot -> snapshot.documents.mapNotNull { it.toAlbum() } }
        }
    }

    /**
     * SPOTIFY OPERATIONS
     */

    /** Resolves an internal (short hash) album id to its Spotify id via Firestore; passes a real Spotify id through. */
    private suspend fun resolveSpotifyId(albumId: String): String {
        if (albumId.length >= 20) return albumId
        return albumsRef.document(albumId).get()
            .takeIf { it.exists }
            ?.data<AlbumDocument>()
            ?.spotifyId
            ?: albumId
    }

    /** Fetches an album from Spotify. `failure` on a network/API error or if it isn't found. */
    suspend fun fetchAlbum(albumId: String): Result<Album> =
        spotifyApi.library.getAlbum(resolveSpotifyId(albumId))
            .map { AlbumMapper.toDomain(it) }

    // New-releases browse feed: first page only (20 albums fills the grid) and cached for
    // the app session, so opening Search — or bouncing in and out of it — doesn't re-hit
    // Spotify or re-run the whereIn against `albums`. Refresh chip passes forceRefresh.
    private var newReleasesCache: Pair<Long, List<Album>>? = null

    suspend fun fetchAllNewReleases(forceRefresh: Boolean = false): List<Album> {
        val now = Clock.System.now().toEpochMilliseconds()
        newReleasesCache?.let { (fetchedAt, albums) ->
            if (!forceRefresh && now - fetchedAt < NEW_RELEASES_TTL_MS) return albums
        }
        val response = spotifyApi.user.getNewReleases().getOrNull()
            ?: return newReleasesCache?.second ?: emptyList()
        val albums = response.albums.items.map { AlbumMapper.toDomain(it) }
        newReleasesCache = now to albums
        return albums
    }

    suspend fun fetchMultipleAlbums(ids: List<String>, saveToDb: Boolean = true): Result<List<Album>> = resultOf {
        if (ids.isEmpty()) return@resultOf emptyList()

        val spotifyIds = ids.map { resolveSpotifyId(it) }

        val albums = mutableListOf<Album>()

        // Process in batches of 20 (Spotify API limit)
        spotifyIds.chunked(20).forEach { batch ->
            spotifyApi.library.getMultipleAlbums(batch)
                .onSuccess { response ->
                    response.albums.forEach { albumDto ->
                        val album = AlbumMapper.toDomain(albumDto)
                        albums.add(album)

                        if (saveToDb) {
                            // saveAlbum already dispatches AlbumEvent.AlbumAdded
                            saveAlbum(albumDto)
                        } else {
                            eventDispatcher.dispatch(AlbumEvent.AlbumAdded(album))
                        }
                    }
                }
                .onFailure { error ->
                    throw error
                }
        }
        albums
    }

    /**
     * OTHER APIS
     */
    suspend fun fetchReleaseGroupId(album: Album): Result<MusicBrainzResponse> {
        val upc = album.externalIds?.get("upc")
        if (upc.isNullOrEmpty()) {
            Napier.w("Album '${album.name}' has no UPC, skipping release group fetch")
            return Result.failure(IllegalArgumentException("Album has no UPC"))
        }
        return miscApi.getAlbumReleaseDetailsByUPC(upc)
    }

    suspend fun fetchReleaseGroup(releaseGroupId: String) =
        miscApi.getReleasesForGroup(releaseGroupId)

    /**
     * CHAINED OPERATIONS
     */
    suspend fun saveAlbumIfNotPresent(album: Album) {
        if (!albumExists(album.id)) {
            saveAlbum(album)
        }
    }

    suspend fun import(): Map<Playlist, List<AlbumResult>> {
        Napier.d("Importing Collections")
        val playlistsResponse = spotifyApi.temp.getPlaylists()
        val playlists = playlistsResponse.getOrThrow()
        Napier.d("Fetched ${playlists.size} playlists")

        return playlists.associateWith { playlist -> spotifyApi.temp.extractAlbumsFromPlaylist(playlist) }
    }

    companion object {
        private const val NEW_RELEASES_TTL_MS = 1_800_000L // 30 min
    }
}
