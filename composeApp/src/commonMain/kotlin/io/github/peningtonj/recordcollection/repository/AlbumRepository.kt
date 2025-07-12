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
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class AlbumRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi,
    private val eventDispatcher: AlbumEventDispatcher

) {
    fun saveAlbum(album: AlbumDto) {
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
            updated_at = System.currentTimeMillis()
        )
    }

    suspend fun fetchAlbum(albumId: String, replaceArtist : Boolean = false) {
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
    suspend fun syncSavedAlbums() {
        Napier.d("Syncing Albums")
        var offset = 0
        val limit = 50 // Max allowed by Spotify API
        var hasMore = true

        database.albumsQueries.deleteAll()

        while (hasMore) {
            spotifyApi.library.getUsersSavedAlbums(limit, offset)
                .onSuccess { response ->
                    response.items.forEach { savedAlbum ->
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
                            updated_at = System.currentTimeMillis()
                        )
                        val album = AlbumMapper.toDomain(savedAlbum.album)
                        eventDispatcher.dispatch(AlbumEvent.AlbumAdded(album, true))
                    }

                    offset += response.items.size
                    hasMore = response.next != null && response.next.isNotEmpty()
                }
                .onFailure { error ->
                    throw error // Or handle error appropriately
                }
        }
        Napier.d("Finished Syncing Albums")

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

    private suspend fun fetchAndSaveTracks(albumId: String) {
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
                            is_explicit = if(track.explicit) 1 else 0,
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

    fun getAlbumById(id: String) : Flow<Album> = database.albumsQueries
        .getAlbumById(id)
        .asFlow()
        .mapToOne(Dispatchers.IO)
        .map { AlbumMapper.toDomain(it) }



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

    fun getAllArtists(): Flow<List<String>> = database.albumsQueries
        .getAllArtists()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getAlbumCount(): Flow<Long> = database.albumsQueries
        .getCount()
        .asFlow()
        .mapToOne(Dispatchers.IO)

    fun getLatestAlbum(): Flow<Album?> {
        Napier.d("DB QUERY: getLatestAlbum")

        return database.albumsQueries.getLatest()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity ->
                entity?.let(AlbumMapper::toDomain)
            }
    }

    fun getAlbumsByYear(year : String): Flow<List<Album>> {
        return database.albumsQueries.getByReleaseDate(year)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map(AlbumMapper::toDomain)
            }
    }


    suspend fun import() : Map<Playlist, List<AlbumResult>> {
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
}