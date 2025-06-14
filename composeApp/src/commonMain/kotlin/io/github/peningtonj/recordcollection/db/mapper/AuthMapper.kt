package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Spotify_auth
import io.github.peningtonj.recordcollection.network.oauth.spotify.AccessToken

object AuthMapper {
    fun Spotify_auth.toAccessToken() = AccessToken(
        accessToken = access_token,
        tokenType = token_type,
        scope = scope,
        expiresIn = expires_in,
        refreshToken = refresh_token
    )
}
