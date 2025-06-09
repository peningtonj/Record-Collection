package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.ktor.client.HttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("scope")
    val scope: String,
    @SerialName("expires_in")
    val expiresIn: Int,
    @SerialName("refresh_token")
    val refreshToken: String
)

expect class AuthHandler(
    client: HttpClient = HttpClientProvider.create(),
    clientId: String = "50709a6502f34b179880dc6e77ad8200",
    clientSecret: String = "fdef37339685451fb88e1100cbffd47c",
) {
    suspend fun authenticate(): Result<String>

    suspend fun exchangeCodeForToken(code: String): Result<AccessToken>

    suspend fun refreshToken(): Result<AccessToken>
}