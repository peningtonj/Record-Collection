package io.github.peningtonj.recordcollection.repository

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.Profiles
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.mapper.ProfileMapper.toProfileEntity
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto

class ProfileRepository(private val database: RecordCollectionDatabase) {
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
}
