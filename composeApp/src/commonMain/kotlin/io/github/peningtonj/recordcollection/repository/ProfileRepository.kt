package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.network.spotify.Playlist
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.SavedAlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems
import io.github.peningtonj.recordcollection.util.aggregate
import io.github.peningtonj.recordcollection.util.resultOf
import kotlin.collections.chunked

class ProfileRepository(
    private val spotifyApi: SpotifyApi,
) {
    /** Returns the current Spotify user's profile, or null on failure. */
    suspend fun getCurrentUserProfile(): SpotifyProfileDto? =
        spotifyApi.user.getCurrentUserProfile().getOrNull()

    suspend fun getUserSavedPlaylist() =
        spotifyApi.user.getUserPlaylists().getOrNull()
            ?.getAllItems { nextUrl ->
                spotifyApi.library.getNextPaginated(nextUrl)
            }
            ?.getOrNull()
            ?.map { playlist ->
                Playlist(
                    name = playlist.name,
                    id = playlist.id
                )
            } ?: emptyList()

    /**
     * Removes [albums] from the user's Spotify library, 20 at a time. Every chunk is
     * attempted; the [Result] is a failure (carrying an [AggregateException]) if any
     * chunk failed, so a partial failure is never silently dropped.
     */
    suspend fun removeAlbumsFromSpotifyLibrary(albums: List<Album>): Result<Unit> =
        writeAlbumChunks(albums) { chunk ->
            spotifyApi.user.removeAlbumsFromCurrentUsersLibrary(chunk)
        }

    /** Adds [albums] to the user's Spotify library, 20 at a time. See [removeAlbumsFromSpotifyLibrary]. */
    suspend fun addAlbumsToSpotifyLibrary(albums: List<Album>): Result<Unit> =
        writeAlbumChunks(albums) { chunk ->
            spotifyApi.user.saveAlbumsToCurrentUsersLibrary(chunk)
        }

    private suspend fun writeAlbumChunks(
        albums: List<Album>,
        write: suspend (List<String>) -> Unit,
    ): Result<Unit> {
        val chunks = albums.map { it.spotifyId }.filter { it.isNotEmpty() }.chunked(20)
        return chunks.map { chunk -> resultOf { write(chunk) } }.aggregate().map { }
    }

    suspend fun fetchUserSavedAlbums(): List<SavedAlbumDto> {
        val userSavedAlbums = spotifyApi.library.getUsersSavedAlbums()
        return userSavedAlbums.getOrNull()
            ?.getAllItems { nextUrl ->
                spotifyApi.library.getNextPaginated(nextUrl)
            }
            ?.getOrNull()
            ?: emptyList()
    }

}
