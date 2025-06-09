package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.ktor.client.HttpClient

actual class AuthHandler actual constructor(
    client: HttpClient,
    clientId: String,
    clientSecret: String
) {
    actual suspend fun authenticate(): Result<String> {
        TODO("Not yet implemented")
    }

    actual suspend fun exchangeCodeForToken(code: String): Result<AccessToken> {
        TODO("Not yet implemented")
    }
}