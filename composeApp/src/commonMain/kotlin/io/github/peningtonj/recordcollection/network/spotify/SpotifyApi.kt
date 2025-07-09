package io.github.peningtonj.recordcollection.network.spotify

import PlaylistAlbumExtractor
import io.ktor.client.*

class SpotifyApi(
    client: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://api.spotify.com/v1"
    }

    // Use the client directly since auth is handled in HttpClientProvider
    val library = LibraryApi(client)
    val user = UserApi(client)
    val playback = PlaybackApi(client)
    val temp = PlaylistAlbumExtractor(client)
}