package io.github.peningtonj.recordcollection.db.mapper
//
import io.github.peningtonj.recordcollection.db.Profiles
import io.github.peningtonj.recordcollection.network.spotify.model.SpotifyProfileDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object ProfileMapper {
    @OptIn(ExperimentalTime::class)
    fun SpotifyProfileDto.toProfileEntity() = Profiles(
        id = id,
        display_name = displayName,
        email = email,
        country = country,
        spotify_uri = uri,
        spotify_url = externalUrls["spotify"] ?: "",
        profile_image_url = images?.firstOrNull()?.url,
        followers_count = followers?.total?.toLong() ?: 0,
        product_type = product,
        updated_at = Clock.System.now().epochSeconds
    )
}
