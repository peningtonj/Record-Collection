package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

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
    val refreshToken: String? = null
)

/**
 * PKCE (Proof Key for Code Exchange) parameters for secure OAuth flow
 */
data class PKCEParams(
    val codeVerifier: String,
    val codeChallenge: String
)

interface AuthHandler {
    suspend fun authenticate(): Result<String>
    suspend fun exchangeCodeForToken(code: String, codeVerifier: String): Result<AccessToken>
    suspend fun refreshToken(refreshToken: String): Result<AccessToken>
    fun generatePKCEParams(): PKCEParams
    fun getCodeVerifier(): String  // Allow retrieving the stored code verifier
}

abstract class BaseAuthHandler(
    protected val client: HttpClient,
    protected val clientId: String = "50709a6502f34b179880dc6e77ad8200"
    // NOTE: No client_secret needed for PKCE flow - this is intentional and secure!
) : AuthHandler {

    // Store PKCE parameters for the current auth flow
    protected var currentPKCEParams: PKCEParams? = null

    /**
     * Get the stored code verifier - must be called after authenticate()
     */
    override fun getCodeVerifier(): String {
        return currentPKCEParams?.codeVerifier 
            ?: throw IllegalStateException("No PKCE parameters available - authenticate() must be called first")
    }

    /**
     * Generate PKCE code verifier and challenge
     * PKCE (Proof Key for Code Exchange) is the secure way to authenticate public clients
     * without exposing a client secret in the application code
     */
    override fun generatePKCEParams(): PKCEParams {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)
        return PKCEParams(codeVerifier, codeChallenge)
    }

    /**
     * Exchange authorization code for access token using PKCE
     * This uses the code_verifier instead of client_secret
     */
    override suspend fun exchangeCodeForToken(code: String, codeVerifier: String): Result<AccessToken> {
        return try {
            val response = client.post("https://accounts.spotify.com/api/token") {
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", getRedirectUri())
                    append("client_id", clientId)
                    append("code_verifier", codeVerifier)  // PKCE parameter
                }))
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh an access token
     * Note: Refresh tokens don't require PKCE parameters
     */
    override suspend fun refreshToken(refreshToken: String): Result<AccessToken> {
        return try {
            val response = client.post("https://accounts.spotify.com/api/token") {
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                    append("client_id", clientId)
                }))
            }

            Result.success(response.body())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    protected abstract fun getRedirectUri(): String

    /**
     * Generate a cryptographically random code verifier
     * Must be 43-128 characters long
     */
    private fun generateCodeVerifier(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') + '-' + '.' + '_' + '~'
        return (1..128)
            .map { allowedChars.random() }
            .joinToString("")
    }

    /**
     * Generate code challenge from code verifier using SHA-256
     * For KMP, we'll use the platform-specific implementation
     */
    protected abstract fun generateCodeChallenge(codeVerifier: String): String

    /**
     * Build the authorization URL with PKCE parameters
     */
    protected fun buildAuthorizationUrl(codeChallenge: String, state: String): String {
        val scopes = listOf(
            "user-read-private",
            "user-read-email",
            "user-library-read",
            "user-library-modify",
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "playlist-read-private",
            "playlist-read-collaborative"
        ).joinToString(" ")

        return buildString {
            append("https://accounts.spotify.com/authorize?")
            append("client_id=${clientId.encodeURLParameter()}")
            append("&response_type=code")
            append("&redirect_uri=${getRedirectUri().encodeURLParameter()}")
            append("&code_challenge_method=S256")
            append("&code_challenge=${codeChallenge.encodeURLParameter()}")
            append("&state=${state.encodeURLParameter()}")
            append("&scope=${scopes.encodeURLParameter()}")
        }
    }
}

