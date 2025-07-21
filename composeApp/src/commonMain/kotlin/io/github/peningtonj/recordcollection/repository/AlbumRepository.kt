package io.github.peningtonj.recordcollection.repository

import AlbumResult
import Playlist
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.events.AlbumEvent
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class AlbumRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi,
    private val miscApi: MiscApi,
    private val eventDispatcher: AlbumEventDispatcher

) {

    /**
     * DATABASE OPERATIONS
     */
    fun saveAlbum(album: AlbumDto, addToUsersLibrary: Boolean = true) {
        database.albumsQueries.insert(
            id = album.id,
            name = album.name,
            primary_artist = album.artists.firstOrNull()?.name ?: "Unknown Artist",
            artists = Json.encodeToString(album.artists),
            release_date = album.releaseDate,
            total_tracks = album.totalTracks.toLong(),
            spotify_uri = album.uri,
            added_at = Clock.System.now().toString(),
            album_type = album.albumType.toString(),
            images = Json.encodeToString(album.images),
            updated_at = System.currentTimeMillis(),
            external_ids = album.externalIds.let { Json.encodeToString(it) },
            in_library = if (addToUsersLibrary) 1 else 0,
            release_group_id = null
        )
    }

    fun saveAlbum(album: Album, overrideInLibrary: Boolean? = null) {
        val addToLibrary = overrideInLibrary ?: album.inLibrary

        database.albumsQueries.insert(
            id = album.id,
            name = album.name,
            primary_artist = album.artists.firstOrNull()?.name ?: "Unknown Artist",
            artists = Json.encodeToString(album.artists),
            release_date = album.releaseDate.toString(),
            total_tracks = album.totalTracks.toLong(),
            spotify_uri = album.spotifyUri,
            added_at = Clock.System.now().toString(),
            album_type = album.albumType.toString(),
            images = Json.encodeToString(album.images),
            updated_at = System.currentTimeMillis(),
            external_ids = album.externalIds.let { Json.encodeToString(it) },
            in_library = if (addToLibrary) 1 else 0,
            release_group_id = null
        )
    }

    fun albumExists(albumId: String) =
        database.albumsQueries
            .getAlbumById(albumId)
            .executeAsOneOrNull() != null

    fun getAlbumByNameAndArtistIfPresent(name: String, artistName: String): Flow<Album?> = database.albumsQueries
        .selectAlbumByNameAndArtist(name, artistName)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it?.let { AlbumMapper.toDomain(it) } }

    fun getAlbumById(id: String): Flow<Album> = database.albumsQueries
        .getAlbumById(id)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map {
            it?.let { AlbumMapper.toDomain(it) }
                ?: throw NoSuchElementException("Album with id '$id' not found")
        }


    fun getEarliestReleaseDate(): Flow<LocalDate?> = getAllAlbums()
        .map { albums ->
            albums.minOfOrNull { album ->
                album.releaseDate
            }
        }

    fun getAllAlbums(): Flow<List<Album>> = database.albumsQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map(AlbumMapper::toDomain) }

    fun getAllAlbumsInLibrary(): Flow<List<Album>> = database.albumsQueries
        .selectAllAlbumsInLibrary()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map(AlbumMapper::toDomain) }

    fun getAllArtists(): Flow<List<String>> = database.albumsQueries
        .getAllArtists()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getLibraryCount(): Flow<Long> = database.albumsQueries
        .getLibraryCount()
        .asFlow()
        .mapToOne(Dispatchers.IO)

    fun getAlbumsByArtist(artistName: String): Flow<List<Album>> {
        return database.albumsQueries.getAlbumsByArtist(artistName)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map(AlbumMapper::toDomain)
            }
    }

    fun addAlbumToLibrary(albumId: String) {
        database.albumsQueries.updateInLibraryStatus(1, albumId)
    }

    fun removeAlbumFromLibrary(albumId: String) {
        database.albumsQueries.updateInLibraryStatus(0, albumId)
    }

    fun updateReleaseGroupId(albumId: String, releaseGroupId: String) =
        database.albumsQueries.updateReleaseGroupId(releaseGroupId, albumId)

    fun getAlbumsFromReleaseGroup(releaseGroupId: String?): Flow<List<Album>> {
        return if (releaseGroupId == null) {
            flowOf(emptyList())
        } else {
            database.albumsQueries
                .selectAlbumsByReleaseId(releaseGroupId)
                .asFlow()
                .mapToList(Dispatchers.IO)
                .map { it.map(AlbumMapper::toDomain) }
        }
    }
    /**
     * SPOTIFY OPERATIONS
     */
    suspend fun fetchAlbum(albumId: String): Album? {
        spotifyApi.library.getAlbum(albumId)
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

        return result.getOrNull()?.map { albumDto ->
            AlbumMapper.toDomain(albumDto)
        } ?: emptyList()
    }

    suspend fun fetchMultipleAlbums(ids: List<String>, saveToDb: Boolean = true): Result<List<Album>> = runCatching {
        if (ids.isEmpty()) return@runCatching emptyList()

        val albums = mutableListOf<Album>()

        // Process in batches of 20 (Spotify API limit)
        ids.chunked(20).forEach { batch ->
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

    suspend fun fetchAndSaveAlbum(albumId: String, replaceArtist: Boolean = false) {
        Napier.d("Fetching Album $albumId")
        spotifyApi.library.getAlbum(albumId)
            .onSuccess { response ->
                database.transaction {
                    saveAlbum(response)
                }
                val album = AlbumMapper.toDomain(response)
                eventDispatcher.dispatch(AlbumEvent.AlbumAdded(album, replaceArtist))
            }
    }

    fun saveAlbumIfNotPresent(album: Album) {
        if (albumExists(album.id)) {
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
