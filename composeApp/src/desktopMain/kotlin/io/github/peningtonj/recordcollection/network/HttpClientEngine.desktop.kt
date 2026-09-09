package io.github.peningtonj.recordcollection.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp

actual val httpClientEngine: HttpClientEngineFactory<*> = OkHttp
