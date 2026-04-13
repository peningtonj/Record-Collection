package io.github.peningtonj.recordcollection.repository

import AlbumResult
import Playlist
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
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

class AlbumRepository(
    private val firestore: FirebaseFirestore,
    private val spotifyApi: SpotifyApi,
    private val miscApi: MiscApi,
    private val eventDispatcher: AlbumEventDispatcher
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
    suspend fun saveAlbum(album: AlbumDto, addToUsersLibrary: Boolean = true) {
        val domainAlbum = AlbumMapper.toDomain(album)
        LoggingUtils.d(
            LoggingUtils.Category.REPOSITORY,
            "Saving album: ${album.name} by ${album.artists.firstOrNull()?.name} (ID: ${domainAlbum.id}, inLibrary: $addToUsersLibrary)"
        )
        LoggingUtils.logFirebaseWrite("albums", "set", domainAlbum.id, mapOf("name" to album.name))
        albumsRef.document(domainAlbum.id).set(
            AlbumMapper.toDocument(domainAlbum).copy(
                inLibrary = addToUsersLibrary,
                addedAt = Clock.System.now().toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
        eventDispatcher.dispatch(AlbumEvent.AlbumAdded(domainAlbum))
    }

    suspend fun saveAlbum(album: Album, overrideInLibrary: Boolean? = null) {
        val addToLibrary = overrideInLibrary ?: album.inLibrary
        LoggingUtils.d(
            LoggingUtils.Category.REPOSITORY,
            "Saving album: ${album.name} by ${album.artists.firstOrNull()?.name} (ID: ${album.id}, inLibrary: $addToLibrary)"
        )
        LoggingUtils.logFirebaseWrite("albums", "set", album.id, mapOf("name" to album.name))
        albumsRef.document(album.id).set(
            AlbumMapper.toDocument(album).copy(
                inLibrary = addToLibrary,
                addedAt = Clock.System.now().toString(),
                updatedAt = System.currentTimeMillis()
            )
        )
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

    fun getAlbumById(id: String): Flow<Album> {
        LoggingUtils.logFirebaseQuery("albums", "snapshot by id", mapOf("id" to id))
        return albumsRef.document(id).snapshots
            .map { snapshot ->
                if (!snapshot.exists) throw NoSuchElementException("Album with id '$id' not found")
                snapshot.toAlbum() ?: throw NoSuchElementException("Album with id '$id' failed to deserialize")
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

    fun getEarliestReleaseDate(): Flow<LocalDate?> = getAllAlbums()
        .map { albums -> albums.minOfOrNull { it.releaseDate } }

    fun getAllAlbums(): Flow<List<Album>> {
        LoggingUtils.logFirebaseQuery("albums", "snapshots (all)")
        return albumsRef.snapshots
            .map { snapshot ->
                LoggingUtils.logFirebaseResult("albums", "getAllAlbums", snapshot.documents.size)
                snapshot.documents.mapNotNull { it.toAlbum() }
            }
    }

    fun getAllAlbumsInLibrary(): Flow<List<Album>> {
        LoggingUtils.logFirebaseQuery("albums", "snapshots (in_library=true)")
        return albumsRef
            .where { "in_library" equalTo true }
            .snapshots
            .map { snapshot ->
                LoggingUtils.logFirebaseResult("albums", "getAllAlbumsInLibrary", snapshot.documents.size)
                snapshot.documents.mapNotNull { it.toAlbum() }
            }
    }

    fun getAllArtists(): Flow<List<String>> =
        getAllAlbums().map { albums -> albums.map { it.primaryArtist }.distinct().sorted() }

    fun getLibraryCount(): Flow<Long> =
        getAllAlbumsInLibrary().map { it.size.toLong() }

    fun getAlbumsByArtist(artistName: String): Flow<List<Album>> {
        LoggingUtils.logFirebaseQuery("albums", "snapshots by artist", mapOf("artist" to artistName))
        return albumsRef
            .where { "primary_artist" equalTo artistName }
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toAlbum() } }
    }

    suspend fun addAlbumToLibrary(albumId: String) {
        LoggingUtils.logFirebaseWrite("albums", "set merge (addToLibrary)", albumId)
        albumsRef.document(albumId).set(mapOf("in_library" to true), merge = true)
    }

    suspend fun removeAlbumFromLibrary(albumId: String) {
        LoggingUtils.logFirebaseWrite("albums", "set merge (removeFromLibrary)", albumId)
        albumsRef.document(albumId).set(mapOf("in_library" to false), merge = true)
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
    suspend fun fetchAlbum(albumId: String): Album? {
        // Determine if this is an internal ID (short hash) or Spotify ID (22 chars)
        val isInternalId = albumId.length < 20

        val spotifyId = if (isInternalId) {
            // Internal ID – look up spotify_id from Firestore
            albumsRef.document(albumId).get()
                .takeIf { it.exists }
                ?.data<AlbumDocument>()
                ?.spotifyId
                ?: albumId // If not found, try using the ID as-is
        } else {
            albumId
        }

        spotifyApi.library.getAlbum(spotifyId)
            .onSuccess { response ->
                return AlbumMapper.toDomain(response)
            }.onFailure { error ->
                throw (error)
            }
        return null
    }

    suspend fun fetchAllNewReleases(): List<Album> {
        val response = spotifyApi.user.getNewReleases().getOrNull() ?: return emptyList()

        val result = response.albums.getAllItems { nextUrl ->
            Napier.d("Fetching next page of new releases: $nextUrl")
            spotifyApi.user.fetchNextNewReleases(nextUrl)
        }

        return result.getOrNull()?.map { AlbumMapper.toDomain(it) } ?: emptyList()
    }

    suspend fun fetchMultipleAlbums(ids: List<String>, saveToDb: Boolean = true): Result<List<Album>> = runCatching {
        if (ids.isEmpty()) return@runCatching emptyList()

        // Convert internal IDs to Spotify IDs by looking up in Firestore
        val spotifyIds = ids.map { id ->
            albumsRef.document(id).get()
                .takeIf { it.exists }
                ?.data<AlbumDocument>()
                ?.spotifyId
                ?: id // If not found in Firestore, assume it's already a Spotify ID
        }

        val albums = mutableListOf<Album>()

        // Process in batches of 20 (Spotify API limit)
        spotifyIds.chunked(20).forEach { batch ->
            spotifyApi.library.getMultipleAlbums(batch)
                .onSuccess { response ->
                    response.albums.forEach { albumDto ->
                        albums.add(AlbumMapper.toDomain(albumDto))

                        if (saveToDb) {
                            saveAlbum(albumDto)
                        }
                        val album = AlbumMapper.toDomain(albumDto)
                        eventDispatcher.dispatch(AlbumEvent.AlbumAdded(album))
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
    suspend fun fetchReleaseGroupId(album: Album) =
        if (album.externalIds?.containsKey("upc") ?: false) {
            miscApi.getAlbumReleaseDetailsByUPC(album.externalIds["upc"]!!)
        } else {
            Napier.w("Album does not have an UPC, skipping release group fetch")
            throw IllegalArgumentException("Album does not have an UPC")
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
}
