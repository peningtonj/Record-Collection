// commonMain/network/spotify/SpotifyApiFactory.kt
package io.github.peningtonj.recordcollection.network.spotify

import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider

object SpotifyApiFactory {
    fun create(dependencies: DependencyContainer): SpotifyApi {
        return SpotifyApi(
            client = HttpClientProvider.create(dependencies.authRepository),
            authRepository = dependencies.authRepository
        )
    }
}