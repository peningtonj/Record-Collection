package io.github.peningtonj.recordcollection.repository

import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FirebaseFirestore
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.domain.TrackDocument
import io.github.peningtonj.recordcollection.db.mapper.TrackMapper
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackRepository(
    firestore: FirebaseFirestore,
    private val spotifyApi: SpotifyApi
) {
    private val tracksCollection = firestore.collection("tracks")

    fun getTracksForAlbum(albumId: String): Flow<List<Track>> =
        tracksCollection
            .where { "album_id" equalTo albumId }
            .orderBy("track_number")
            .snapshots
            .map { snapshot -> snapshot.documents.mapNotNull { it.toTrack() } }

    suspend fun checkAndUpdateTracksIfNeeded(albumId: String, spotifyId: String) {
        Napier.d("Checking if tracks exist for album $albumId")
        val tracksExist = tracksCollection
            .where { "album_id" equalTo albumId }
            .get()
            .documents
            .isNotEmpty()
        Napier.d { "Tracks exist: $tracksExist" }
        if (!tracksExist) {
            fetchAndSaveTracks(albumId, spotifyId)
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

    suspend fun fetchAndSaveTracks(albumId: String, spotifyId: String) {
        Napier.d("Fetching tracks for album $albumId (Spotify ID: $spotifyId)")
        spotifyApi.library.getAlbumTracks(spotifyId)
            .onSuccess { response ->
                response.items.forEach { track ->
                    tracksCollection.document(track.id).set(TrackMapper.toDocument(track, albumId))
                }
            }
            .onFailure { error -> Napier.e("Failed to fetch tracks for album $albumId", error) }
    }

    suspend fun saveTracksLocalAndRemote(trackIds: List<String>) {
        saveTracksRemote(trackIds)
        trackIds.forEach { addTrackToLibrary(it) }
    }

    suspend fun addTrackToLibrary(trackId: String) {
        tracksCollection.document(trackId).set(mapOf("is_saved" to true), merge = true)
    }

    suspend fun saveTrackToLibrary(track: Track) {
        tracksCollection.document(track.id).set(TrackMapper.toDocument(track.copy(isSaved = true)))
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
            .map { snapshot -> snapshot.documents.mapNotNull { it.toTrack() } }

    suspend fun removeTrackFromLibrary(trackId: String) {
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
}
