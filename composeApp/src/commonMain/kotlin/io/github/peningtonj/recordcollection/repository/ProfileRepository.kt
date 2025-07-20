package io.github.peningtonj.recordcollection.repository

import Playlist
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.Profiles
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.mapper.ProfileMapper.toProfileEntity
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto
import io.github.peningtonj.recordcollection.network.spotify.model.getAllItems

class ProfileRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi,
    ) {
    fun saveProfile(profile: SpotifyProfileDto) {
        Napier.d { "Saving Profile to DB" }
        database.profilesQueries.upsertProfile(
            profile.toProfileEntity()
        )
    }

    // Add a function to retrieve profile
    fun getProfile(): Profiles? {
        return database.profilesQueries
            .getProfile()
            .executeAsOneOrNull()
    }

    suspend fun getUserSavedPlaylist() =
        spotifyApi.user.getUserPlaylists().getOrNull()
            ?.getAllItems{ nextUrl ->
                spotifyApi.library.getNextPaginated(nextUrl)
            }
            ?.getOrNull()
            ?.map { playlist ->
                Playlist(
                    name = playlist.name,
                    id = playlist.id
                )
        } ?: emptyList()
}
