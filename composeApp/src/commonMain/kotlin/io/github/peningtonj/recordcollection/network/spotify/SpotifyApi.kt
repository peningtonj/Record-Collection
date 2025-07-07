
package io.github.peningtonj.recordcollection.network.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.util.*

class SpotifyAuthPlugin private constructor(private val config: Config) {
    class Config {
        var authRepository: SpotifyAuthRepository? = null
    }

    companion object Plugin : HttpClientPlugin<Config, SpotifyAuthPlugin> {
        override val key = AttributeKey<SpotifyAuthPlugin>("SpotifyAuthPlugin")

        override fun prepare(block: Config.() -> Unit): SpotifyAuthPlugin {
            val config = Config().apply(block)
            requireNotNull(config.authRepository) { "AuthRepository must be configured" }
            return SpotifyAuthPlugin(config)
        }

        override fun install(plugin: SpotifyAuthPlugin, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { request ->
                val token = plugin.config.authRepository?.getStoredToken()
                    ?: throw IllegalStateException("No valid access token found")

                request.headers.append("Authorization", "Bearer ${token.access_token}")
                Napier.d { "Request authorized with token $token" }

                execute(request)
            }
        }
    }
}

class SpotifyApi(
    client: HttpClient,
    private val authRepository: SpotifyAuthRepository
) {
    companion object {
        const val BASE_URL = "SpotifyApi.BASE_URL"
    }

    private val authorizedClient = client.config {
        install(SpotifyAuthPlugin) {
            authRepository = this@SpotifyApi.authRepository
        }
    }

    val library = LibraryApi(authorizedClient)
    val user = UserApi(authorizedClient)
    val playback = PlaybackApi(authorizedClient)
}