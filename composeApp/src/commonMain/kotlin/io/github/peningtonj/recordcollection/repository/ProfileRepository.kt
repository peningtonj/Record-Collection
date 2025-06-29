package io.github.peningtonj.recordcollection.repository

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.Profile
import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.db.mapper.ProfileMapper.toProfileEntity
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfile

class ProfileRepository(private val database: RecordCollectionDatabase) {
    fun saveProfile(profile: SpotifyProfile) {
        Napier.d { "Saving Profile to DB" }
        database.profileQueries.upsertProfile(
            profile.toProfileEntity()
        )
    }

    // Add a function to retrieve profile
    fun getProfile(): Profile? {
        return database.profileQueries
            .getProfile()
            .executeAsOneOrNull()
    }
}
