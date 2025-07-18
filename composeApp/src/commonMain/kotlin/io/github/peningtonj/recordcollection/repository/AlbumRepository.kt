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
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.db.mapper.TrackMapper
import io.github.peningtonj.recordcollection.events.AlbumEvent
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SavedAlbumDto
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

    suspend fun fetchAlbum(albumId: String): Album? {
        spotifyApi.library.getAlbum(albumId)
            .onSuccess { response ->
                return AlbumMapper.toDomain(response)
            }.onFailure { error ->
                throw (error)
            }
        return null

    }

    suspend fun removeAlbumsFromSpotifyLibrary(albums: List<Album>) {
        albums.map { it.id }
            .chunked(20)
            .forEach { chunk ->
                spotifyApi.user.removeAlbumsFromCurrentUsersLibrary(chunk)
            }
    }

    suspend fun addAlbumsToSpotifyLibrary(albums: List<Album>) {
        albums.map { it.id }
            .chunked(20)
            .forEach { chunk ->
                spotifyApi.user.saveAlbumsToCurrentUsersLibrary(chunk)
            }
    }

    fun saveAlbum(savedAlbum: SavedAlbumDto) {
        database.albumsQueries.insert(
            id = savedAlbum.album.id,
            name = savedAlbum.album.name,
            primary_artist = savedAlbum.album.artists.firstOrNull()?.name ?: "Unknown Artist",
            artists = Json.encodeToString(savedAlbum.album.artists),
            release_date = savedAlbum.album.releaseDate,
            total_tracks = savedAlbum.album.totalTracks.toLong(),
            spotify_uri = savedAlbum.album.uri,
            added_at = savedAlbum.addedAt,
            album_type = savedAlbum.album.albumType.toString(),
            images = Json.encodeToString(savedAlbum.album.images),
            updated_at = System.currentTimeMillis(),
            external_ids = savedAlbum.album.externalIds?.let { Json.encodeToString(it) } ?: "",
            in_library = 1,
            release_group_id = null
        )


        eventDispatcher.dispatch(AlbumEvent.AlbumAdded(AlbumMapper.toDomain(savedAlbum.album), true))
    }
    suspend fun fetchUserSavedAlbums(): List<SavedAlbumDto> {
        Napier.d("Syncing Albums")
        var offset = 0
        val limit = 50 // Max allowed by Spotify API
        var hasMore = true
        val userSavedAlbums = mutableListOf<SavedAlbumDto>()

        while (hasMore) {
            spotifyApi.library.getUsersSavedAlbums(limit, offset)
                .onSuccess { response ->
                    userSavedAlbums.addAll(response.items)
                    offset += response.items.size
                    hasMore = response.next != null && response.next.isNotEmpty()
                }
                .onFailure { error ->
                    throw error // Or handle error appropriately
                }
        }
        return userSavedAlbums
    }

    fun getTracksForAlbum(albumId: String): Flow<List<Track>> {
        return database.tracksQueries
            .getByAlbumId(albumId = albumId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { track -> TrackMapper.toDomain(track, null) } }
    }

    suspend fun checkAndUpdateTracksIfNeeded(albumId: String) {
        Napier.d("Checking if tracks exist for album $albumId")
        val tracksExist = database.tracksQueries
            .countTracksForAlbum(albumId)
            .executeAsOne() > 0

        Napier.d { "Tracks exist: $tracksExist" }
        if (!tracksExist) {
            fetchAndSaveTracks(albumId)
        }
    }

    fun albumExists(albumId: String) =
        database.albumsQueries
            .getAlbumById(albumId)
            .executeAsOneOrNull() != null

    suspend fun fetchTracksForAlbum(album: Album): List<Track> {
        Napier.d("Fetching tracks for album ${album.id}")
        spotifyApi.library.getAlbumTracks(album.id)
            .onSuccess { response ->
                return response.items.map { track ->
                    TrackMapper.toDomain(track, album)
                }
            }
        return emptyList()
    }

    suspend fun fetchAndSaveTracks(albumId: String) {
        Napier.d("Fetching tracks for album $albumId")
        spotifyApi.library.getAlbumTracks(albumId)
            .onSuccess { response ->
                database.transaction {
                    response.items.forEach { track ->
                        database.tracksQueries.insert(
                            id = track.id,
                            album_id = albumId,
                            name = track.name,
                            track_number = track.trackNumber.toLong(),
                            duration_ms = track.durationMs.toLong(),
                            spotify_uri = track.uri,
                            artists = Json.encodeToString(track.artists),
                            preview_url = track.previewUrl,
                            primary_artist = track.artists.firstOrNull()?.name ?: "Unknown Artist",
                            is_explicit = if (track.explicit) 1 else 0,
                            disc_number = track.discNumber.toLong(),
                            popularity = null,
                        )
                    }
                }

            }
            .onFailure { error ->
                Napier.e("Failed to fetch tracks for album $albumId", error)
            }
    }

    fun getAlbumByIdIfPresent(id: String): Flow<Album?> = database.albumsQueries
        .getAlbumById(id)
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


    suspend fun import(): Map<Playlist, List<AlbumResult>> {
        Napier.d("Importing Collections")
        val playlistsResponse = spotifyApi.temp.getPlaylists()
        val playlists = playlistsResponse.getOrThrow()
        Napier.d("Fetched ${playlists.size} playlists")

        return playlists.associateWith { playlist -> spotifyApi.temp.extractAlbumsFromPlaylist(playlist) }
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

    fun saveAlbumIfNotPresent(album: Album) {
        if (albumExists(album.id)) {
            saveAlbum(album)
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

    suspend fun fetchReleaseGroupId(album: Album) =
        if (album.externalIds?.containsKey("upc") ?: false) {
            miscApi.getAlbumReleaseDetailsByUPC(album.externalIds["upc"]!!)
        } else {
            Napier.w("Album does not have an UPC, skipping release group fetch")
            throw IllegalArgumentException("Album does not have an UPC")
        }

    suspend fun fetchReleaseGroup(releaseGroupId: String) =
        miscApi.getReleasesForGroup(releaseGroupId)

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
}
