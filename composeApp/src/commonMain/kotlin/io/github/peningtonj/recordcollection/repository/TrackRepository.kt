package io.github.peningtonj.recordcollection.repository

import io.github.peningtonj.recordcollection.db.RecordCollectionDatabase
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi

class TrackRepository(
    private val database: RecordCollectionDatabase,
    private val spotifyApi: SpotifyApi

) {

}