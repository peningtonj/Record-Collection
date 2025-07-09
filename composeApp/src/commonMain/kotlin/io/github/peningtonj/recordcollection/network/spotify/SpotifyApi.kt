package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.*

class SpotifyApi(
    client: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://api.spotify.com/v1"
        const val Auths_URL = "https://accounts.spotify.com/authorize"
        const val SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token"
        const val CLIENT_ID = "333333333333-33333333333333333333333333333333.apps.googleusercontent.com"
        const val CLIENT_SECRET = "<KEY>"
        const val SCOPES = "user-read-playback-state,user-modify-playback-state,playlist-read-private,playlist-read-collaborative,playlist-modify-public,playlist-modify-private"
        const val RESPONSE_TYPE = "code"
        const val STATE = "recordcollection://callback"
        const val GRANT_TYPE_CODE = "authorization_code"
    }

    // Use the client directly since auth is handled in HttpClientProvider
    val library = LibraryApi(client)
    val user = UserApi(client)
    val playback = PlaybackApi(client)
}