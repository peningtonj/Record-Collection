package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.util.secureRandomHex
import io.ktor.client.HttpClient
import io.ktor.http.URLBuilder
import io.ktor.util.encodeBase64
import kotlinx.browser.window
import kotlinx.coroutines.delay

/**
 * Browser PKCE handler. Spotify's Authorization-Code-with-PKCE flow is built for SPAs,
 * so this is the simplest of the three handlers: open the authorize URL in a popup and
 * poll until it redirects back to our origin with `?code=`.
 *
 * The redirect URI is `<origin>/callback`. Served from `http://127.0.0.1:8888` in dev, that
 * resolves to `http://127.0.0.1:8888/callback` — the URI the desktop handler already
 * registers (Spotify requires the loopback IP, not `localhost`, for non-HTTPS URIs). A
 * production deploy registers its own `https://<host>/callback`. Nothing is served at that
 * path — the popup is closed as soon as the opener reads `?code=` off its URL.
 */
class WebAuthHandler(client: HttpClient) : BaseAuthHandler(client) {

    override fun getRedirectUri(): String = "${window.location.origin}/callback"

    override fun generateCodeChallenge(codeVerifier: String): String {
        val hex = io.github.peningtonj.recordcollection.util.sha256Hex(codeVerifier)
        val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return bytes.encodeBase64().replace('+', '-').replace('/', '_').trimEnd('=')
    }

    override suspend fun authenticate(): Result<String> = runCatching {
        currentPKCEParams = generatePKCEParams()
        val state = secureRandomHex(16)
        val authUrl = buildAuthorizationUrl(currentPKCEParams!!.codeChallenge, state)

        val popup = window.open(authUrl, "spotify-auth", "width=500,height=720")
            ?: error("Popup blocked — allow popups for this site and retry")

        Napier.d("Waiting for Spotify auth popup to redirect back…")
        while (true) {
            delay(300)
            if (popup.closed) error("Authentication window was closed")
            // Throws while the popup is on accounts.spotify.com (cross-origin); readable once
            // it redirects back to our origin.
            val href = runCatching { popup.location.href }.getOrNull() ?: continue
            if (!href.contains("/callback")) continue

            val params = URLBuilder(href).parameters
            popup.close()
            params["error"]?.let { error("Spotify auth error: $it") }
            check(params["state"] == state) { "State mismatch — possible CSRF" }
            return@runCatching params["code"] ?: error("No authorization code in callback")
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }
}
