package io.github.peningtonj.recordcollection.network.common.util

import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*

object HttpClientProvider {
    private var client: HttpClient? = null
    
    fun create(authRepository: SpotifyAuthRepository? = null): HttpClient {
        return client ?: HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }

            install(HttpRequestRetry) {
                retryOnException(maxRetries = 1, retryOnTimeout = false)
                retryIf(maxRetries = 1) { _, httpResponse ->
                    httpResponse.status == HttpStatusCode.Unauthorized
                }

                modifyRequest { request ->
                    authRepository?.let { repo ->
                        try {
                            repo.getRefreshToken()
                        } catch (e: Exception) {
                            println("Token refresh failed: ${e.message}")
                            return@modifyRequest
                        }
                    }
                }
            }
        }.also { client = it }
    }

    fun close() {
        try {
            client?.close()
        } finally {
            client = null
        }
    }
}