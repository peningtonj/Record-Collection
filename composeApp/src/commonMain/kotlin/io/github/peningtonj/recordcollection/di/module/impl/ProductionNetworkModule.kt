package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.di.module.NetworkModule
import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.github.peningtonj.recordcollection.util.LoggingUtils
import io.github.aakira.napier.Napier
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
import kotlinx.serialization.json.Json

class ProductionNetworkModule : NetworkModule {
    private var httpClient: HttpClient? = null
    
    // Track rate limiting statistics
    private var rateLimitCount = 0
    private var totalRequests = 0

    /** Strips the Spotify base URL so log lines are concise. */
    private fun String.spotifyPath() = removePrefix("https://api.spotify.com/v1")
        .removePrefix("https://api.spotify.com")
    
    // Configure JSON with more lenient settings and logging
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }
    
    override fun provideHttpClient(): HttpClient {
        return httpClient ?: HttpClient(OkHttp) {
            // Configure timeouts
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000  // 30 seconds for the entire request
                connectTimeoutMillis = 10_000  // 10 seconds to establish connection
                socketTimeoutMillis = 30_000   // 30 seconds for socket read/write
            }
            
            install(ContentNegotiation) { 
                json(jsonConfig)
            }

            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnServerErrors(maxRetries)
                retryOnExceptionIf { request, cause ->
                    println("🔄 HTTP Exception: ${cause::class.simpleName}: ${cause.message}")
                    cause.printStackTrace()
                    
                    cause is kotlinx.coroutines.TimeoutCancellationException ||
                    cause is java.net.SocketTimeoutException ||
                    cause is java.io.IOException
                }
                
                retryIf(maxRetries) { request, response ->
                    val isRateLimit = response.status == HttpStatusCode.TooManyRequests ||
                                     response.status == HttpStatusCode.ServiceUnavailable ||
                                    response.status == HttpStatusCode.Forbidden
                    
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
            
            // Add response validation to catch serialization errors
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    println("❌ HTTP Response Exception for ${request.url}")
                    println("   Exception: ${exception::class.simpleName}: ${exception.message}")
                    
                    when (exception) {
                        is kotlinx.serialization.SerializationException -> {
                            println("🔍 SERIALIZATION ERROR:")
                            println("   Message: ${exception.message}")
                            exception.printStackTrace()
                        }
                        is kotlinx.serialization.MissingFieldException -> {
                            println("🔍 MISSING FIELD ERROR:")
                            println("   Field: ${exception.message}")
                            exception.printStackTrace()
                        }
                        else -> {
                            println("🔍 OTHER ERROR:")
                            exception.printStackTrace()
                        }
                    }
                }
                
                validateResponse { response ->
                    if (!response.status.isSuccess()) {
                        val responseBody = try {
                            response.bodyAsText()
                        } catch (e: Exception) {
                            "Unable to read response body: ${e.message}"
                        }
                        
                        println("❌ HTTP Error ${response.status.value} for ${response.request.url}")
                        println("   Response body: $responseBody")
                    }
                }
            }
        }.also { httpClient = it }
    }

    override fun provideMiscApi(): MiscApi {
        return MiscApi(provideHttpClient())
    }

    override fun provideOpenAiApi(): OpenAiApi {
        return OpenAiApi(provideHttpClient())
    }

    override fun provideSpotifyApi(authRepository: SpotifyAuthRepository): SpotifyApi {
        val spotifyClient = HttpClient(OkHttp) {
            // Configure timeouts for Spotify API
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000  // 30 seconds for the entire request
                connectTimeoutMillis = 10_000  // 10 seconds to establish connection
                socketTimeoutMillis = 30_000   // 30 seconds for socket read/write
            }
            
            install(ContentNegotiation) {
                json(jsonConfig)
            }


            install(HttpRequestRetry) {
                maxRetries = 3
                retryOnServerErrors(maxRetries)
                retryOnExceptionIf { request, cause ->
                    Napier.w(
                        "Spotify exception: ${cause::class.simpleName}: ${cause.message} — ${request.method.value} ${request.url.buildString().spotifyPath()}",
                        cause,
                        LoggingUtils.Category.SPOTIFY.tag
                    )
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
                        Napier.w(
                            "RATE LIMITED ${response.status} — ${request.method.value} ${request.url.toString().spotifyPath()} | retry-after=${retryAfter}s remaining=$remaining/$limit",
                            tag = LoggingUtils.Category.SPOTIFY.tag
                        )
                        val isPolling = request.headers["X-No-Retry"] == "true"
                        if (isPolling) return@retryIf false
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
                    LoggingUtils.logSpotifyRequest(
                        method = request.method.value,
                        path = request.url.buildString().spotifyPath()
                    )
                }
            }

            // Log every Spotify request and response via Napier → SpotifyFileAntilog
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    val path = request.url.toString().spotifyPath()
                    Napier.e(
                        "Exception for ${request.method.value} $path: ${exception::class.simpleName}: ${exception.message}",
                        exception,
                        LoggingUtils.Category.SPOTIFY.tag
                    )
                }

                validateResponse { response ->
                    val path = response.request.url.toString().spotifyPath()
                    val method = response.request.method.value
                    val status = response.status.value
                    val retryAfter = response.headers["Retry-After"]
                    val remaining = response.headers["X-RateLimit-Remaining"]
                    val limit = response.headers["X-RateLimit-Limit"]
                    LoggingUtils.logSpotifyResponse(
                        method = method,
                        path = path,
                        status = status,
                        durationMs = 0L,
                        rateLimitRemaining = remaining,
                        rateLimitLimit = limit,
                        retryAfter = retryAfter,
                    )
                    if (status >= 400) {
                        val body = try { response.bodyAsText() } catch (e: Exception) { "<unreadable>" }
                        Napier.w("Response body: $body", tag = LoggingUtils.Category.SPOTIFY.tag)
                    }
                }
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        val storedToken = authRepository.getStoredToken()
                        storedToken?.let { token ->
                            BearerTokens(
                                accessToken = token.accessToken,
                                refreshToken = token.refreshToken ?: ""
                            )
                        }
                    }

                    refreshTokens {
                        Napier.d("Refreshing Spotify token…", tag = LoggingUtils.Category.SPOTIFY.tag)
                        val result = authRepository.ensureValidToken()
                        if (result.isSuccess) {
                            val newToken = result.getOrThrow()
                            Napier.i("Token refreshed successfully", tag = LoggingUtils.Category.SPOTIFY.tag)
                            BearerTokens(
                                accessToken = newToken.accessToken,
                                refreshToken = newToken.refreshToken
                            )
                        } else {
                            Napier.e("Token refresh failed: ${result.exceptionOrNull()?.message}", tag = LoggingUtils.Category.SPOTIFY.tag)
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        // Only send to Spotify if we have a valid token
                        if (request.url.host.contains("spotify.com", ignoreCase = true)) {
                            val storedToken = authRepository.getStoredToken()
                            val hasValidToken = storedToken?.accessToken?.isNotEmpty() == true
                            if (!hasValidToken) {
                                Napier.w("Blocking Spotify request — no valid token | ${request.method.value} ${request.url.buildString().spotifyPath()}", tag = LoggingUtils.Category.SPOTIFY.tag)
                            }

                            hasValidToken
                        } else {
                            false
                        }
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