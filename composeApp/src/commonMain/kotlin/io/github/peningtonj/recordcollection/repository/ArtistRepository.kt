package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.ArtistDocument
import io.github.peningtonj.recordcollection.db.domain.Image
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.AristAlbumsRequest
import io.github.peningtonj.recordcollection.network.spotify.model.FullArtistDto
import io.github.peningtonj.recordcollection.util.LoggingUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class ArtistRepository(
    private val firestore: FirebaseFirestore,
    private val spotifyApi: SpotifyApi,
    private val miscApi: MiscApi
) {
    private val artistsRef = firestore.collection("artists")

    // ── Reads ─────────────────────────────────────────────────────────────────

    fun getArtistById(id: String): Flow<Artist?> {
        LoggingUtils.logFirebaseQuery("artists", "snapshot by id", mapOf("id" to id))
        return artistsRef.document(id).snapshots
            .map { snapshot ->
                if (!snapshot.exists) null
                else snapshot.data<ArtistDocument?>()?.toArtist()
            }
    }

    fun getArtistGenre(artist: Artist): Flow<List<String>> {
        LoggingUtils.logFirebaseQuery("artists", "snapshot genres", mapOf("id" to artist.id))
        return artistsRef.document(artist.id).snapshots
            .map { snapshot -> snapshot.data<ArtistDocument?>()?.genres ?: emptyList() }
    }

    fun getAllArtists(): Flow<List<Artist>> {
        LoggingUtils.logFirebaseQuery("artists", "snapshots (all)")
        return artistsRef.snapshots.map { snapshot ->
            LoggingUtils.logFirebaseResult("artists", "getAllArtists", snapshot.documents.size)
            snapshot.documents.mapNotNull { doc ->
                runCatching { doc.data<ArtistDocument?>()?.toArtist() }
                    .onFailure { Napier.e("Failed to deserialize artist doc '${doc.id}': ${it.message}") }
                    .getOrNull()
            }
        }
    }

    fun getAllArtistIds(): Flow<List<String>> {
        LoggingUtils.logFirebaseQuery("artists", "snapshots (ids only)")
        return artistsRef.snapshots.map { snapshot -> snapshot.documents.map { it.id } }
    }

    fun getAllGenres(): Flow<List<String>> =
        getAllArtists().map { artists ->
            artists
                .flatMap { it.genres }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .map { it.first }
        }

    // ── Writes ────────────────────────────────────────────────────────────────

    suspend fun saveArtist(artist: FullArtistDto) {
        LoggingUtils.logFirebaseWrite("artists", "set", artist.id, mapOf("name" to artist.name))
        artistsRef.document(artist.id).set(
            ArtistDocument(
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
        )
    }

    suspend fun saveArtist(artist: EnrichedArtist) {
        LoggingUtils.logFirebaseWrite("artists", "set", artist.artist.id, mapOf("name" to artist.artist.name))
        artistsRef.document(artist.artist.id).set(
            ArtistDocument(
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
        )
    }

    suspend fun saveArtists(artists: List<FullArtistDto>) {
        artists.forEach { saveArtist(it) }
    }

    suspend fun saveArtistsWithGenres(artists: List<EnrichedArtist>) {
        artists.forEach { saveArtist(it) }
    }

    // ── API fetches ───────────────────────────────────────────────────────────

    suspend fun fetchMultipleArtists(artists: List<SimplifiedArtist>) {
        artists.chunked(50).forEach { chunk ->
            Napier.d("Fetching ${chunk.size} artists from Spotify API")
            spotifyApi.library.getMultipleArtists(chunk.map { it.id })
                .onSuccess { response ->
                    Napier.d("Successfully fetched ${response.artists.size} artists from Spotify API")
                    response.artists.forEach { saveArtist(it) }
                }
        }
    }

    suspend fun fetchArtistWithEnhancedGenres(artistId: String): Result<EnrichedArtist> = runCatching {
        val artist = spotifyApi.library.getArtist(artistId).getOrThrow()
        val enhancedGenres = fetchEnhancedGenresForArtist(artistId)
        Napier.d("Fetched enhanced genres for artist $artistId: $enhancedGenres")
        EnrichedArtist(artist, enhancedGenres)
    }

    suspend fun fetchEnhancedGenresForArtist(artistId: String): List<String> =
        miscApi.getArtistGenres(artistId).fold(
            onSuccess = { it },
            onFailure = {
                Napier.w("Failed to fetch enhanced genres for artist $artistId, using empty list")
                emptyList()
            }
        )

    suspend fun fetchArtistsWithEnhancedGenres(artistIds: List<String>, saveToDb: Boolean): List<EnrichedArtist> = coroutineScope {
        val limit = 50
        try {
            artistIds
                .chunked(limit)
                .mapIndexed { index, chunk ->
                    Napier.d("Fetching artist chunk ${index + 1}/${(artistIds.size + limit - 1) / limit} (${chunk.size} artists)")
                    spotifyApi.library.getMultipleArtists(chunk).fold(
                        onSuccess = { response ->
                            val enriched = response.artists.map { artist ->
                                async {
                                    val genres = fetchEnhancedGenresForArtist(artist.id)
                                    EnrichedArtist(artist, genres)
                                }
                            }.awaitAll()
                            if (saveToDb) enriched.forEach { saveArtist(it) }
                            enriched
                        },
                        onFailure = { error ->
                            Napier.e("Failed to fetch artist chunk ${index + 1}: ${error.message}", error)
                            emptyList()
                        }
                    )
                }
                .flatten()
                .also { Napier.d("Successfully fetched ${it.size} total enriched artists") }
        } catch (e: Exception) {
            Napier.e("Error fetching artists: ${e.message}", e)
            throw e
        }
    }

    suspend fun fetchArtist(artistId: String, saveToDb: Boolean = true): Result<EnrichedArtist> =
        fetchArtistWithEnhancedGenres(artistId)
            .onSuccess { if (saveToDb) saveArtist(it) }
            .onFailure { Napier.e("Error fetching artist $artistId: ${it.message}", it) }

    suspend fun fetchAlbumArtists(artistId: String, limit: Int = 20): List<Album> {
        var offset = 0
        var hasMore = true
        val albums = mutableListOf<Album>()
        while (hasMore) {
            spotifyApi.library.getArtistsAlbums(AristAlbumsRequest(artistId, limit, offset))
                .onSuccess { response ->
                    albums.addAll(response.items.map { AlbumMapper.toDomain(it) })
                    offset += response.items.size
                    hasMore = response.next != null && response.next.isNotEmpty()
                }
                .onFailure { throw it }
        }
        return albums
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun ArtistDocument.toArtist() = Artist(
        id = id,
        followers = followers,
        genres = genres,
        href = href,
        images = runCatching { Json.decodeFromString<List<Image>>(images) }.getOrElse { emptyList() },
        name = name,
        popularity = popularity,
        type = type,
        uri = uri
    )
}

data class EnrichedArtist(
    val artist: FullArtistDto,
    val enhancedGenres: List<String>
)