package io.github.peningtonj.recordcollection.repository

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.mapper.TrackMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems

class TrackRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi
) {

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

    suspend fun fetchLibraryTracks(): List<Track> {
        val savedTracksResult =  spotifyApi.library.getUsersSavedTracks()
        return savedTracksResult.getOrNull()
            ?.getAllItems { nextUrl ->
                spotifyApi.library.getNextPaginated(nextUrl)
            }
            ?.getOrNull()
            ?.map { track -> TrackMapper.toDomain(track.track) } ?: emptyList()
    }
    suspend fun fetchTracksForAlbum(album: Album): List<Track> {
        val spotifyId = album.spotifyId ?: album.id
        Napier.d("Fetching tracks for album ${album.id} (Spotify ID: $spotifyId)")
        return spotifyApi.library.getAlbumTracks(spotifyId)
            .mapCatching { response ->
                response.items.map { track ->
                    TrackMapper.toDomain(track, album)
                }
            }
            .getOrElse {
                Napier.e("Failed to fetch tracks for album ${album.id}", it)
                emptyList()
            }
    }

    suspend fun fetchAndSaveTracks(albumId: String) {
        // Look up spotify_id from our database
        val spotifyId = database.albumsQueries
            .getAlbumById(albumId)
            .executeAsOneOrNull()
            ?.spotify_id
            ?: albumId // If not found, assume it's already a Spotify ID
        
        Napier.d("Fetching tracks for album $albumId (Spotify ID: $spotifyId)")
        spotifyApi.library.getAlbumTracks(spotifyId)
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
                            is_saved = 0
                        )
                    }
                }
            }
            .onFailure { error ->
                Napier.e("Failed to fetch tracks for album $albumId", error)
            }
    }

    suspend fun saveTracksLocalAndRemote(trackIds: List<String>) {
        saveTracksRemote(trackIds)
        trackIds.forEach { trackId ->
            addTrackToLibrary(trackId)
        }
    }

    fun addTrackToLibrary(trackId: String) {
        database.tracksQueries.updateInLibraryStatus(1, trackId)
    }

    suspend fun saveTracksRemote(trackIds: List<String>) {
        spotifyApi.user.saveTracksToLikedTracks(trackIds)
    }

    suspend fun removeTracksRemote(trackIds: List<String>) {
        spotifyApi.user.removeTracksFromLikedTracks(trackIds)
    }

    fun getSavedTracks(): Flow<List<Track>> = database.tracksQueries
            .getSavedTracks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { track -> TrackMapper.toDomain(track, null) } }


    fun removeTrackFromLibrary(trackId: String) {
        database.tracksQueries.updateInLibraryStatus(0, trackId)
    }

}
