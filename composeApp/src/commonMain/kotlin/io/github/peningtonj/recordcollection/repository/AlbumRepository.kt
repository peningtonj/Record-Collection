package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.db.Album_entity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class AlbumRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi
) {
    suspend fun syncSavedAlbums() {
        var offset = 0
        val limit = 50 // Max allowed by Spotify API
        var hasMore = true

        database.albumQueries.deleteAll()

        while (hasMore) {
            spotifyApi.library.getUsersSavedAlbums(limit, offset)
                .onSuccess { response ->
                    response.items.forEach { savedAlbum ->
                        database.albumQueries.insert(
                            id = savedAlbum.album.id,
                            name = savedAlbum.album.name,
                            artist_name = savedAlbum.album.artists.firstOrNull()?.name ?: "Unknown Artist",
                            release_date = savedAlbum.album.releaseDate,
                            total_tracks = savedAlbum.album.totalTracks.toLong(),
                            spotify_uri = savedAlbum.album.uri,
                            spotify_url = savedAlbum.album.externalUrls["spotify"],
                            added_at = savedAlbum.added_at,
                            album_type = savedAlbum.album.albumType.toString(),
                            cover_image_url = savedAlbum.album.images.firstOrNull()?.url,
                            updated_at = System.currentTimeMillis()
                        )
                    }

                    offset += response.items.size
                    hasMore = response.next != null
                }
                .onFailure { error ->
                    throw error // Or handle error appropriately
                }
        }
    }

    fun getAllAlbums(): Flow<List<Album_entity>> = database.albumQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getAlbumCount(): Flow<Long> = database.albumQueries
        .getCount()
        .asFlow()
        .mapToOne(Dispatchers.IO)

    fun getLatestAlbum(): Flow<Album_entity?> = database.albumQueries
        .getLatest()
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)

}
