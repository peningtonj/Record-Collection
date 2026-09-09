package io.github.peningtonj.recordcollection.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.js.Js

actual val httpClientEngine: HttpClientEngineFactory<*> = Js
