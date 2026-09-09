package io.github.peningtonj.recordcollection.testDataFactory

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track

object TestTrackDataFactory {
    fun track(
        trackNumber: Int = 1,
        album: Album
    ): Track {
        return Track(
            id = "track_${trackNumber}_id",
            name = "Track $trackNumber",
            artists = listOf(TestSimplifiedArtistFactory.simplifiedArtist()),
            albumId = album.id,
            isExplicit = true,
            trackNumber = trackNumber.toLong(),
            discNumber = 1L,
            durationMs = 100L,
            spotifyUri = "spotify:track:${trackNumber}_id",
            album = album,
        )
    }
}