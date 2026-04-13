package io.github.peningtonj.recordcollection.network.oauth.spotify

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest

class AndroidAuthHandler(
    private val context: Context,
    client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
    }
) : BaseAuthHandler(client) {

    companion object {
        private var pendingAuthCode: CompletableDeferred<String>? = null

        /** Called from MainActivity.onNewIntent when recordcollection://callback fires */
        fun handleCallback(uri: Uri) {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")
            when {
                code != null -> pendingAuthCode?.complete(code)
                error != null -> pendingAuthCode?.completeExceptionally(
                    Exception("Spotify auth error: $error")
                )
                else -> pendingAuthCode?.completeExceptionally(
                    Exception("No code or error in OAuth callback")
                )
            }
        }
    }

    override fun getRedirectUri() = "recordcollection://callback"

    override fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateState(): String {
        val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..32).map { chars.random() }.joinToString("")
    }

    override suspend fun authenticate(): Result<String> {
        currentPKCEParams = generatePKCEParams()
        val state = generateState()
        val authUrl = buildAuthorizationUrl(currentPKCEParams!!.codeChallenge, state)

        pendingAuthCode = CompletableDeferred()

        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .also { it.intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
            .launchUrl(context, Uri.parse(authUrl))

        return try {
            val code = withTimeoutOrNull(5 * 60 * 1_000L) {
                pendingAuthCode!!.await()
            } ?: return Result.failure(Exception("Spotify authentication timed out"))
            Result.success(code)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            pendingAuthCode = null
        }
    }
}
