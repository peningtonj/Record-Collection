package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.util.LoggingUtils
import io.github.peningtonj.recordcollection.db.domain.TrackDocument
import io.github.peningtonj.recordcollection.db.mapper.TrackMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.hours

class TrackRepository(
    firestore: FirebaseFirestore,
    private val spotifyApi: SpotifyApi
) {
    private val tracksCollection = firestore.collection("tracks")

    // ── Album tracklists ──────────────────────────────────────────────────────
    // Tracklists are volatile Spotify metadata: fetched on demand into a short-TTL
    // in-memory cache, never persisted to Firestore (see docs/DATA_MODEL.md, TECH_DEBT 2.7).
    private data class CachedTracklist(val tracks: List<Track>, val fetchedAtMs: Long)
    private val tracklistCache = mutableMapOf<String, CachedTracklist>()
    private val tracklistMutex = Mutex()

    /**
     * Local like/unlike actions since the last full fetch (`trackId -> saved`). Layered on
     * top of Spotify's `contains` check so a heart toggle shows immediately and survives
     * a tracklist re-emit — see [setTrackSaved].
     */
    private val savedOverrides = mutableMapOf<String, Boolean>()

    /**
     * Album tracklist, cache-first. Hits Spotify on a cold/stale entry (and cross-checks
     * each track against the user's Liked Songs); the result lives in memory for
     * [TRACKLIST_TTL] and is dropped on app restart. Returns `emptyList()` if the fetch
     * fails (and does not cache the failure).
     */
    suspend fun getAlbumTracks(
        album: Album,
        forceRefresh: Boolean = false,
        checkSaved: Boolean = true,
    ): List<Track> {
        val now = Clock.System.now().toEpochMilliseconds()
        if (!forceRefresh) {
            val cached = tracklistMutex.withLock { tracklistCache[album.id] }
            if (cached != null && now - cached.fetchedAtMs < TRACKLIST_TTL.inWholeMilliseconds) {
                return cached.tracks
            }
        }
        val fetched = fetchTracksForAlbum(album)
        if (fetched.isEmpty()) return fetched
        val withSaved = if (checkSaved) markSavedStatus(fetched) else fetched
        tracklistMutex.withLock { tracklistCache[album.id] = CachedTracklist(withSaved, now) }
        return withSaved
    }

    /** Sets each track's `isSaved` from `GET /me/tracks/contains`, with local overrides winning. */
    private suspend fun markSavedStatus(tracks: List<Track>): List<Track> {
        val savedById = mutableMapOf<String, Boolean>()
        tracks.map { it.id }.chunked(50).forEach { chunk ->
            spotifyApi.library.checkSavedTracks(chunk)
                .onSuccess { flags -> chunk.forEachIndexed { i, id -> savedById[id] = flags.getOrElse(i) { false } } }
                .onFailure { Napier.w("checkSavedTracks failed for ${chunk.size} ids", it) }
        }
        return tracks.map { t ->
            t.copy(isSaved = savedOverrides[t.id] ?: savedById[t.id] ?: t.isSaved)
        }
    }

    /**
     * Record a local like/unlike so cached tracklists and the next fetch reflect it
     * immediately, without waiting for Spotify's `contains` view to catch up.
     */
    suspend fun setTrackSaved(trackId: String, saved: Boolean) {
        tracklistMutex.withLock {
            savedOverrides[trackId] = saved
            for (key in tracklistCache.keys.toList()) {
                val cached = tracklistCache.getValue(key)
                if (cached.tracks.any { it.id == trackId }) {
                    tracklistCache[key] = cached.copy(
                        tracks = cached.tracks.map { if (it.id == trackId) it.copy(isSaved = saved) else it }
                    )
                }
            }
        }
    }

    suspend fun fetchLibraryTracks(): List<Track> {
        val savedTracksResult = spotifyApi.library.getUsersSavedTracks()
        return savedTracksResult.getOrNull()
            ?.getAllItems { nextUrl -> spotifyApi.library.getNextPaginated(nextUrl) }
            ?.getOrNull()
            ?.map { track -> TrackMapper.toDomain(track.track) } ?: emptyList()
    }

    suspend fun fetchTracksForAlbum(album: Album): List<Track> {
        val spotifyId = album.spotifyId
        Napier.d("Fetching tracks for album ${album.id} (Spotify ID: $spotifyId)")
        return spotifyApi.library.getAlbumTracks(spotifyId)
            .mapCatching { response -> response.items.map { TrackMapper.toDomain(it, album) } }
            .getOrElse {
                Napier.e("Failed to fetch tracks for album ${album.id}", it)
                emptyList()
            }
    }

    suspend fun saveTracksLocalAndRemote(trackIds: List<String>) {
        saveTracksRemote(trackIds)
        trackIds.forEach { addTrackToLibrary(it) }
    }

    suspend fun addTrackToLibrary(trackId: String) {
        LoggingUtils.logFirebaseWrite("tracks", "set merge is_saved=true", trackId)
        tracksCollection.document(trackId).set(mapOf("is_saved" to true), merge = true)
    }

    suspend fun saveTrackToLibrary(track: Track) {
        LoggingUtils.logFirebaseWrite("tracks", "set (saveTrackToLibrary)", track.id)
        tracksCollection.document(track.id).set(TrackMapper.toDocumentMap(track.copy(isSaved = true)))
    }

    suspend fun saveTracksRemote(trackIds: List<String>) {
        spotifyApi.user.saveTracksToLikedTracks(trackIds)
    }

    suspend fun removeTracksRemote(trackIds: List<String>) {
        spotifyApi.user.removeTracksFromLikedTracks(trackIds)
    }

    fun getSavedTracks(): Flow<List<Track>> =
        tracksCollection
            .where { "is_saved" equalTo true }
            .snapshots
            .map { snapshot ->
                LoggingUtils.logFirebaseResult("tracks", "snapshots where is_saved", snapshot.documents.size)
                snapshot.documents.mapNotNull { it.toTrack() }
            }

    suspend fun removeTrackFromLibrary(trackId: String) {
        LoggingUtils.logFirebaseWrite("tracks", "set merge is_saved=false", trackId)
        tracksCollection.document(trackId).set(mapOf("is_saved" to false), merge = true)
    }

    private fun DocumentSnapshot.toTrack(): Track? {
        return try {
            val doc = data<TrackDocument?>() ?: return null
            TrackMapper.toDomain(id, doc)
        } catch (e: Exception) {
            // TODO: remove after diagnosis
            fun probe(block: () -> Any?): String = try {
                block().let { v -> "$v (${v?.let { it::class.simpleName } ?: "null"})" }
            } catch (ex: Exception) { "ERROR: ${ex.message}" }

            Napier.e(tag = "TrackDeserialise") {
                buildString {
                    appendLine("❌ Failed id=$id  error=${e.message}")
                    appendLine("   name             = ${probe { get<String?>("name") }}")
                    appendLine("   album_id         = ${probe { get<String?>("album_id") }}")
                    appendLine("   track_number     = ${probe { get<Long?>("track_number") }}")
                    appendLine("   track_number(D)  = ${probe { get<Double?>("track_number") }}")
                    appendLine("   disc_number      = ${probe { get<Long?>("disc_number") }}")
                    appendLine("   disc_number(D)   = ${probe { get<Double?>("disc_number") }}")
                    appendLine("   duration_ms      = ${probe { get<Long?>("duration_ms") }}")
                    appendLine("   duration_ms(D)   = ${probe { get<Double?>("duration_ms") }}")
                    appendLine("   spotify_uri      = ${probe { get<String?>("spotify_uri") }}")
                    appendLine("   primary_artist   = ${probe { get<String?>("primary_artist") }}")
                    appendLine("   is_explicit      = ${probe { get<Boolean?>("is_explicit") }}")
                    appendLine("   is_saved         = ${probe { get<Boolean?>("is_saved") }}")
                }
            }
            null
        }
    }

    companion object {
        /** How long an in-memory album tracklist stays fresh before a re-fetch. */
        val TRACKLIST_TTL = 24.hours
    }
}
