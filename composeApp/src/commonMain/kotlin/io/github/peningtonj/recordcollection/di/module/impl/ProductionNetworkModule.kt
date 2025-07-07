
package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

class ProductionNetworkModule : NetworkModule {
    private var httpClient: HttpClient? = null
    
    override fun provideHttpClient(): HttpClient {
        return httpClient ?: HttpClient(OkHttp) {
            install(ContentNegotiation) { 
                json()
            }
        }.also { httpClient = it }
    }
    
    override fun provideSpotifyApi(authRepository: SpotifyAuthRepository): SpotifyApi {
        return SpotifyApi(provideHttpClient(), authRepository)
    }
    
    override fun close() {
        httpClient?.close()
        httpClient = null
    }
}