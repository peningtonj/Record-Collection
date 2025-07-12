package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.network.everynoise.EveryNoiseApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class ProductionNetworkModule : NetworkModule {
    private var httpClient: HttpClient? = null
    
    // Track rate limiting statistics
    private var rateLimitCount = 0
    private var totalRequests = 0
    
    override fun provideHttpClient(): HttpClient {
        return httpClient ?: HttpClient(OkHttp) {
            install(ContentNegotiation) { 
                json()
            }
            
            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnServerErrors(maxRetries)
                retryOnExceptionIf { request, cause ->
                    cause is kotlinx.coroutines.TimeoutCancellationException ||
                    cause is java.net.SocketTimeoutException ||
                    cause is java.io.IOException
                }
                
                retryIf(maxRetries) { request, response ->
                    val isRateLimit = response.status == HttpStatusCode.TooManyRequests ||
                                     response.status == HttpStatusCode.ServiceUnavailable
                    
                    if (isRateLimit) {
                        rateLimitCount++
                        val retryAfter = response.headers["Retry-After"]
                        val resetTime = response.headers["X-RateLimit-Reset"]
                        
                        println("🚨 RATE LIMITED! ${response.status} for ${request.url.host}")
                        println("   Retry-After: $retryAfter, Reset: $resetTime")
                        println("   Total rate limits: $rateLimitCount out of $totalRequests requests")
                    }
                    
                    isRateLimit
                }
                
                exponentialDelay(
                    base = 2.0,
                    maxDelayMs = 30_000,
                    randomizationMs = 1_000
                )
                
                modifyRequest { request ->
                    totalRequests++
                    request.header("User-Agent", "RecordCollection/1.0")
                }
            }
        }.also { httpClient = it }
    }

    override fun provideEveryNoiseApi(): EveryNoiseApi {
        return EveryNoiseApi(provideHttpClient())
    }

    override fun provideSpotifyApi(authRepository: SpotifyAuthRepository): SpotifyApi {
        val spotifyClient = HttpClient(OkHttp) {
            install(ContentNegotiation) { 
                json()
            }
            
            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnServerErrors(maxRetries)
                retryOnExceptionIf { request, cause ->
                    cause is kotlinx.coroutines.TimeoutCancellationException ||
                    cause is java.net.SocketTimeoutException ||
                    cause is java.io.IOException
                }
                
                retryIf(maxRetries) { request, response ->
                    val isRateLimit = response.status == HttpStatusCode.TooManyRequests ||
                                     response.status == HttpStatusCode.ServiceUnavailable
                    
                    if (isRateLimit) {
                        val retryAfter = response.headers["Retry-After"]
                        val remaining = response.headers["X-RateLimit-Remaining"]
                        val limit = response.headers["X-RateLimit-Limit"]
                        
                        println("🎵 SPOTIFY RATE LIMITED! ${response.status}")
                        println("   Retry-After: $retryAfter")
                        println("   Rate limit: $remaining/$limit remaining")
                    }
                    
                    isRateLimit
                }
                
                exponentialDelay(
                    base = 2.0,
                    maxDelayMs = 60_000,
                    randomizationMs = 2_000
                )
                
                modifyRequest { request ->
                    request.header("User-Agent", "RecordCollection/1.0")
                }
            }
            
            install(Auth) {
                bearer {
                    loadTokens {
                        val storedToken = authRepository.getStoredToken()
                        storedToken?.let { token ->
                            BearerTokens(
                                accessToken = token.access_token,
                                refreshToken = token.refresh_token
                            )
                        }
                    }
                    
                    refreshTokens {
                        val result = authRepository.ensureValidToken()
                        if (result.isSuccess) {
                            val newToken = result.getOrThrow()
                            BearerTokens(
                                accessToken = newToken.accessToken,
                                refreshToken = newToken.refreshToken
                            )
                        } else {
                            null
                        }
                    }
                    
                    sendWithoutRequest { request ->
                        request.url.host.contains("spotify.com", ignoreCase = true)
                    }
                }
            }
        }
        
        return SpotifyApi(spotifyClient)
    }
    
    override fun close() {
        // Print final stats before closing
        val percentage = if (totalRequests > 0) (rateLimitCount * 100 / totalRequests) else 0
        println("📊 Network Stats: $rateLimitCount rate limits out of $totalRequests requests ($percentage%)")
        httpClient?.close()
        httpClient = null
    }
}