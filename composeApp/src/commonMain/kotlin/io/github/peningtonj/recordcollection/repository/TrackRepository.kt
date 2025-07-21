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

    suspend fun fetchTracksForAlbum(album: Album): List<Track> {
        Napier.d("Fetching tracks for album ${album.id}")
        return spotifyApi.library.getAlbumTracks(album.id)
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
}
