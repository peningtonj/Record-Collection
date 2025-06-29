// commonMain/network/spotify/SpotifyApiFactory.kt
package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.di.DependencyContainer
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider

object SpotifyApiFactory {
    fun create(dependencies: DependencyContainer): SpotifyApi {
        return SpotifyApi(
            client = HttpClientProvider.create(),
            authRepository = dependencies.authRepository
        )
    }
}