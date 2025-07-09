package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
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
        // Create a separate authenticated client for Spotify API
        val spotifyClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { 
                json()
            }
            
            install(Auth) {
                bearer {
                    loadTokens {
                        // Load initial tokens from storage
                        val storedToken = authRepository.getStoredToken()
                        storedToken?.let { token ->
                            BearerTokens(
                                accessToken = token.access_token,
                                refreshToken = token.refresh_token
                            )
                        }
                    }
                    
                    refreshTokens {
                        // This is called automatically when a 401 is received
                        val result = authRepository.ensureValidToken()
                        if (result.isSuccess) {
                            val newToken = result.getOrThrow()
                            BearerTokens(
                                accessToken = newToken.accessToken,
                                refreshToken = newToken.refreshToken
                            )
                        } else {
                            // Token refresh failed - this will trigger re-authentication
                            null
                        }
                    }
                    
                    sendWithoutRequest { request ->
                        // Only send tokens to Spotify API endpoints
                        request.url.host.contains("spotify.com", ignoreCase = true)
                    }
                }
            }
        }
        
        return SpotifyApi(spotifyClient)
    }
    
    override fun close() {
        httpClient?.close()
        httpClient = null
    }
}