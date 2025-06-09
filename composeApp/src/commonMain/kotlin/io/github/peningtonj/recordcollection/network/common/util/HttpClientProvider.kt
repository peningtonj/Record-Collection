package io.github.peningtonj.recordcollection.network.common.util

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

object HttpClientProvider {
    fun create(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
    }
}