package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.ktor.util.encodeBase64

@Serializable
data class AccessToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("token_type")
    val tokenType: String,
    @SerialName("scope")
    val scope: String,
    @SerialName("expires_in")
    val expiresIn: Long,
    @SerialName("refresh_token")
    val refreshToken: String
)

interface AuthHandler {
    suspend fun authenticate(): Result<String>
    suspend fun exchangeCodeForToken(code: String): Result<AccessToken>
    suspend fun refreshToken(): Result<AccessToken>
}

abstract class BaseAuthHandler(
    protected val client: HttpClient,
    protected val clientId: String = "50709a6502f34b179880dc6e77ad8200",
    protected val clientSecret: String = "fdef37339685451fb88e1100cbffd47c"
) : AuthHandler {

    override suspend fun exchangeCodeForToken(code: String): Result<AccessToken> {
        return try {
            val response = client.post("https://accounts.spotify.com/api/token") {
                headers {
                    append("Authorization", "Basic ${buildBasicAuth()}")
                }
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", getRedirectUri())
                }))
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    protected abstract fun getRedirectUri(): String

    protected fun buildBasicAuth(): String {
        return "$clientId:$clientSecret".encodeToByteArray().encodeBase64()
    }
}
