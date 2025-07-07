package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.ktor.client.HttpClient

class AndroidAuthHandler(
    client: HttpClient = HttpClientProvider.create()
) : BaseAuthHandler(client) {
    override fun getRedirectUri() = "recordcollection://callback"

    override suspend fun authenticate(): Result<String> {
        // TODO: Implement Android-specific authentication
        TODO("Implement Android authentication flow")
    }

    override suspend fun refreshToken(): Result<AccessToken> {
        TODO("Not yet implemented")
    }
}
