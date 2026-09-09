package io.github.peningtonj.recordcollection.network

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * The Ktor engine for this platform — `OkHttp` on JVM/Android, `Js` (browser `fetch`) on
 * web. Injected into [io.github.peningtonj.recordcollection.di.module.impl.ProductionNetworkModule]
 * so `commonMain` no longer hard-codes a JVM-only engine.
 */
expect val httpClientEngine: HttpClientEngineFactory<*>
