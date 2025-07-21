package io.github.peningtonj.recordcollection.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.db.mapper.ArtistMapper
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.AristAlbumsRequest
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json



class ArtistRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi,
    private val miscApi: MiscApi
) {

    suspend fun fetchMultipleArtists(artists: List<SimplifiedArtist>) {
        artists.chunked(50).forEach { chunk ->
            Napier.d("Fetching ${chunk.size} artists from Spotify API")
            val artistIds = chunk.map { it.id }
            spotifyApi.library.getMultipleArtists(artistIds)
                .onSuccess { response ->
                    Napier.d("Successfully fetched ${response.artists.size} artists from Spotify API")
                    response.artists.forEach { artist ->
                        saveArtist(artist)
                    }
                }
        }
    }
    suspend fun fetchArtistWithEnhancedGenres(artistId: String): Result<EnrichedArtist> = runCatching {
        // Get artist from Spotify API
        val artist = spotifyApi.library.getArtist(artistId).getOrThrow()

        // Get enhanced genres from Every Noise API (with fallback)
        val enhancedGenres = fetchEnhancedGenresForArtist(artistId)
        Napier.d("Fetched enhanced genres for artist $artistId: $enhancedGenres")

        EnrichedArtist(artist, enhancedGenres)
    }

    suspend fun fetchEnhancedGenresForArtist(artistId: String): List<String> =
        miscApi.getArtistGenres(artistId).fold(
            onSuccess = { response ->
                response },
            onFailure = { response ->
                Napier.w("Failed to fetch enhanced genres for artist $artistId, using empty list")
                emptyList() 
            }
        )


    suspend fun fetchArtistsWithEnhancedGenres(artistIds: List<String>, saveToDb: Boolean): List<EnrichedArtist> = coroutineScope {
        val limit = 50

        try {
            // Process artists in chunks
            val allEnrichedArtists = artistIds
                .chunked(limit)
                .mapIndexed { index, chunk ->
                    Napier.d("Fetching artist chunk ${index + 1}/${(artistIds.size + limit - 1) / limit} (${chunk.size} artists)")

                    // Fetch Spotify artist data
                    val spotifyResult = spotifyApi.library.getMultipleArtists(chunk)
                    
                    spotifyResult.fold(
                        onSuccess = { response ->
                            Napier.d("Successfully fetched ${response.artists.size} artists from chunk ${index + 1}")
                            
                            // Fetch enhanced genres for each artist concurrently
                            val enrichedArtists = response.artists.map { artist ->
                                async {
                                    val enhancedGenres = fetchEnhancedGenresForArtist(artist.id)
                                    EnrichedArtist(artist, enhancedGenres)
                                }
                            }.awaitAll()

                            if (saveToDb) {
                                enrichedArtists.forEach { enrichedArtist ->
                                    database.transaction {
                                        saveArtist(enrichedArtist)
                                    }
                                }

                            }
                            
                            enrichedArtists
                        },
                        onFailure = { error ->
                            Napier.e("Failed to fetch artist chunk ${index + 1}: ${error.message}", error)
                            emptyList()
                        }
                    )
                }
                .flatten()

            Napier.d("Successfully fetched ${allEnrichedArtists.size} total enriched artists")
            return@coroutineScope allEnrichedArtists

        } catch (e: Exception) {
            Napier.e("Error fetching artists: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchArtist(artistId: String, saveToDb: Boolean = true): Result<EnrichedArtist> =
        fetchArtistWithEnhancedGenres(artistId)
            .onSuccess { enrichedArtist ->
                if (saveToDb) {
                    database.transaction {
                        saveArtist(enrichedArtist)
                    }
                }
            }
            .onFailure { error ->
                Napier.e("Error fetching artist $artistId: ${error.message}", error)
            }

    suspend fun fetchAlbumArtists(artistId: String, limit: Int = 20) : List<Album> {
        var offset = 0
        var hasMore = true

        val albums = mutableListOf<Album>()

        while (hasMore) {
            spotifyApi.library.getArtistsAlbums(
                AristAlbumsRequest(
                    artistId, limit, offset
                )
            )
                .onSuccess { response ->
                    albums.addAll(
                        response.items.map { AlbumMapper.toDomain(it) }
                    )
                    offset += response.items.size
                    hasMore = response.next != null && response.next.isNotEmpty()
                }
                .onFailure { error ->
                    throw error // Or handle error appropriately
                }
        }

        return albums
    }

    
    fun getArtistGenre(artist: Artist) = database.artistsQueries
        .selectArtistGenres(artist.id)
        .asFlow()
        .mapToList(Dispatchers.IO)

    fun getArtistById(id: String): Flow<Artist?> = database.artistsQueries
        .getArtistById(id)
        .asFlow()
        .mapToOneOrNull(Dispatchers.IO)
        .map { it?.let(ArtistMapper::toDomain) }

    fun saveArtists(artists: List<FullArtistDto>) {
        artists.forEach { artistDto ->
            saveArtist(artistDto)
        }
    }

    fun saveArtistsWithGenres(artists: List<EnrichedArtist>) {
        artists.forEach { artistDto ->
            saveArtist(artistDto)
        }
    }

    fun saveArtist(artist: FullArtistDto) {
        database.artistsQueries.insert(
            id = artist.id,
            followers = artist.followers.total.toLong(),
            genres = artist.genres,
            href = artist.href,
            images = Json.encodeToString(artist.images),
            name = artist.name,
            popularity = artist.popularity.toLong(),
            type = artist.type,
            uri = artist.uri
        )
    }

    fun saveArtist(artist: EnrichedArtist) {
        database.artistsQueries.insert(
            id = artist.artist.id,
            followers = artist.artist.followers.total.toLong(),
            genres = artist.enhancedGenres,
            href = artist.artist.href,
            images = Json.encodeToString(artist.artist.images),
            name = artist.artist.name,
            popularity = artist.artist.popularity.toLong(),
            type = artist.artist.type,
            uri = artist.artist.uri
        )
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

data class EnrichedArtist(
    val artist: FullArtistDto,
    val enhancedGenres: List<String>
)