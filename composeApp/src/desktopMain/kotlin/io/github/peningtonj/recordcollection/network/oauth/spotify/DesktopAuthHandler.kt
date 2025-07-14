package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import org.apache.http.client.utils.URIBuilder
import java.awt.Desktop
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URI
import java.net.URLDecoder

@Serializable
data class CallbackResponse(
    val code: String,
    val state: String? = null
) {
    companion object {
        fun fromParameters(parameters: Parameters) = CallbackResponse(
            code = parameters["code"] ?: throw IllegalArgumentException("Required parameter 'code' is missing"),
            state = parameters["state"]
        )
    }
}

class DesktopAuthHandler(
    client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }
) : BaseAuthHandler(client) {

    override fun getRedirectUri() = "http://localhost:8888/callback"

    override suspend fun authenticate(): Result<String> {
        Napier.d { "Starting Desktop Authentication" }

        val server = ServerSocket(8888)
        return try {
            val authUrl = URIBuilder()
                .setScheme("https")
                .setHost("accounts.spotify.com")
                .setPath("/authorize")
                .addParameter("client_id", clientId)
                .addParameter("response_type", "code")
                .addParameter("redirect_uri", getRedirectUri())
                .addParameter(
                    "scope",
                    "playlist-modify-public playlist-modify-private user-library-read user-modify-playback-state user-read-playback-state user-read-recently-played"
                )
                .build()

            Napier.d { "Browsing to URL in Desktop" }
            Desktop.getDesktop().browse(authUrl)
            Napier.d { "Waiting for callback" }
            val socket = server.accept()
            val reader = socket.getInputStream().bufferedReader()
            val requestLine = reader.readLine()

            // Parse the HTTP request using URI
            val uri = URI(requestLine.split(" ")[1])

            // Create Parameters object from query string
            val parameters = Parameters.build {
                uri.query?.split("&")?.forEach { param ->
                    val (key, value) = param.split("=")
                    append(key, URLDecoder.decode(value, "UTF-8"))
                }
            }

            // Create CallbackResponse object and get the code
            val callbackResponse = CallbackResponse.fromParameters(parameters)

            // Send success response to browser
            val outputStream = socket.getOutputStream()
            sendSuccessResponse(outputStream)
            socket.close()
            Napier.d("Received code: ${callbackResponse.code}")

            Result.success(callbackResponse.code)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            server.close()
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<AccessToken> {
        return try {
            Napier.d { "Refreshing access token" }

            val response = client.post("https://accounts.spotify.com/api/token") {
                headers {
                    append("Authorization", "Basic ${buildBasicAuth()}")
                }
                setBody(FormDataContent(Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refreshToken)
                }))
            }

            val newToken: AccessToken = response.body()
            Napier.d { "Successfully refreshed access token" }

            Result.success(newToken)
        } catch (e: Exception) {
            Napier.e(e) { "Failed to refresh token: ${e.message}" }
            Result.failure(e)
        }
    }

    private fun sendSuccessResponse(outputStream: OutputStream) {
        val writer = PrintWriter(outputStream, true)
        writer.print(
            """
        HTTP/1.1 200 OK
        Content-Type: text/html
        Connection: close

        <html>
        <head>
            <title>Record Collection - Authentication Success</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background: linear-gradient(135deg, #1DB954 0%, #1ed760 100%);
                    margin: 0;
                    padding: 0;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                    color: white;
                }
                .container {
                    text-align: center;
                    background: rgba(255, 255, 255, 0.1);
                    padding: 3rem;
                    border-radius: 20px;
                    backdrop-filter: blur(10px);
                    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                    max-width: 500px;
                }
                .icon {
                    font-size: 4rem;
                    margin-bottom: 1rem;
                    animation: bounce 2s infinite;
                }
                h1 {
                    font-size: 2.5rem;
                    margin-bottom: 1rem;
                    font-weight: 300;
                }
                p {
                    font-size: 1.2rem;
                    margin-bottom: 2rem;
                    opacity: 0.9;
                }
                .spotify-logo {
                    width: 60px;
                    height: 60px;
                    margin: 1rem auto;
                    background: #1DB954;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-size: 24px;
                    font-weight: bold;
                }
                .close-hint {
                    font-size: 0.9rem;
                    opacity: 0.7;
                    margin-top: 1rem;
                }
                @keyframes bounce {
                    0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
                    40% { transform: translateY(-10px); }
                    60% { transform: translateY(-5px); }
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="icon">🎵</div>
                <div class="spotify-logo">♪</div>
                <h1>Authentication Successful!</h1>
                <p>Your Record Collection app is now connected to Spotify</p>
                <p>✅ Access to your playlists granted<br>
                   ✅ Library management enabled<br>
                   ✅ Playback control activated</p>
                <div class="close-hint">
                    You can safely close this window and return to the app
                </div>
            </div>
            <script>
                // Auto-close after 5 seconds (optional)
                setTimeout(() => {
                    window.close();
                }, 5000);
            </script>
        </body>
        </html>
    """.trimIndent()
        )
        writer.flush()
    }
}