package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

class AlbumRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi
) {
    suspend fun syncSavedAlbums() {
        Napier.d("Syncing Albums")
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
                    }

                    offset += response.items.size
                    hasMore = response.next != null
                }
                .onFailure { error ->
                    throw error // Or handle error appropriately
                }
        }
        Napier.d("Finished Syncing Albums")

    }

    fun getEarliestReleaseDate(): Flow<LocalDate?> = getAllAlbums()
        .map { albums ->
            albums.minOfOrNull { album ->
                album.releaseDate
            }
        }

    fun getAllAlbums(): Flow<List<Album>> = database.albumQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map(AlbumMapper::toDomain) }

    fun getAllArtists(): Flow<List<String>> = database.albumQueries
        .getAllArtists()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getAlbumCount(): Flow<Long> = database.albumQueries
        .getCount()
        .asFlow()
        .mapToOne(Dispatchers.IO)

    fun getLatestAlbum(): Flow<Album?> {
        Napier.d("DB QUERY: getLatestAlbum")

        return database.albumQueries.getLatest()
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { entity ->
                entity?.let(AlbumMapper::toDomain)
            }
    }

    fun getAlbumsByYear(year : String): Flow<List<Album>> {
        return database.albumQueries.getByReleaseDate(year)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                list.map(AlbumMapper::toDomain)
            }
    }


}
