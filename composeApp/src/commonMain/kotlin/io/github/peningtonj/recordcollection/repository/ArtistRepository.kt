package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.mapper.ArtistMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.collections.emptyList

class ArtistRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi
) {
    suspend fun getArtistsForAlbums(albums: List<Album>): List<FullArtistDto> {
        Napier.d("Getting artists associated with ${albums.size} albums, e.g. ${albums.firstOrNull()?.name}")

        // Early return if no albums
        if (albums.isEmpty()) {
            Napier.d("No albums provided, skipping artist fetch")
            return emptyList()
        }


        val existingArtistIds = getAllArtistIds().first().toSet()

        // Extract artist IDs, filtering out nulls and duplicates
        val artistIds = albums
            .mapNotNull { album -> album.artists.firstOrNull()?.id }
            .distinct()
            .filterNot { it in existingArtistIds }

        // Early return if no valid artist IDs
        if (artistIds.isEmpty()) {
            Napier.d("No valid artist IDs found in albums")
            return emptyList()
        }


        Napier.d("Found ${artistIds.size} unique artist IDs to fetch")
        val limit = 50

        try {
            // Process artists in chunks
            val allArtists = artistIds
                .chunked(limit)
                .mapIndexed { index, chunk ->
                    Napier.d("Fetching artist chunk ${index + 1}/${(artistIds.size + limit - 1) / limit} (${chunk.size} artists)")

                    val result = spotifyApi.library.getMultipleArtists(chunk)
                    result.fold(
                        onSuccess = { response ->
                            Napier.d("Successfully fetched ${response.artists.size} artists from chunk ${index + 1}")
                            response.artists
                        },
                        onFailure = { error ->
                            Napier.e("Failed to fetch artist chunk ${index + 1}: ${error.message}", error)
                            emptyList()
                        }
                    )
                }
                .flatten()

            Napier.d("Successfully fetched ${allArtists.size} total artists")
            return allArtists

        } catch (e: Exception) {
            Napier.e("Error fetching artists: ${e.message}", e)
            throw e // or handle according to your error strategy
        }

    }

    fun getArtistGenre(artist: Artist) = database.artistsQueries
        .selectArtistGenres(artist.id)
        .asFlow()
        .mapToList(Dispatchers.IO)




    fun saveArtists(artists: List<FullArtistDto>) {
        artists.forEach { artistDto ->
            database.artistsQueries.insert(
                id = artistDto.id,
                followers = artistDto.followers.total.toLong(),
                genres = artistDto.genres,
                href = artistDto.href,
                images = Json.encodeToString(artistDto.images),
                name = artistDto.name,
                popularity = artistDto.popularity.toLong(),
                type = artistDto.type,
                uri = artistDto.uri
            )
        }
    }

    fun getAllArtists(): Flow<List<Artist>> = database.artistsQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { it.map(ArtistMapper::toDomain) }

    fun getAllArtistIds(): Flow<List<String>> = database.artistsQueries
        .selectAllIds()
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getAllGenres(): Flow<List<String>> = database.artistsQueries
        .selectAllGenres()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { genresList ->
            genresList
                .flatten() // Flatten List<List<String>> to List<String>
                .groupingBy { it } // Group by genre name
                .eachCount() // Count occurrences of each genre
                .toList() // Convert to List<Pair<String, Int>>
                .sortedByDescending { it.second } // Sort by count (descending)
                .map { it.first } // Extract just the genre names
        }
}
