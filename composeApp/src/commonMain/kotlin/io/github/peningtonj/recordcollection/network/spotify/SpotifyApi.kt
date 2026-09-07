package io.github.peningtonj.recordcollection.network.spotify

import io.ktor.client.*

class SpotifyApi(
    client: HttpClient,
) {
    companion object {
        const val BASE_URL = "https://api.spotify.com/v1"
    }

    // The client already has the Ktor Auth (Bearer) plugin installed by
    // ProductionNetworkModule.provideSpotifyApi, so sub-APIs use it directly.
    val library = LibraryApi(client)
    val user = UserApi(client)
    val playback = PlaybackApi(client)
    val temp = PlaylistAlbumExtractor(client)
    val search = SearchApi(client)
}