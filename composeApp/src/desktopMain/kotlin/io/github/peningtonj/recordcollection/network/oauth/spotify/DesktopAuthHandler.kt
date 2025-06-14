package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.common.util.HttpClientProvider
import io.ktor.client.HttpClient
import io.ktor.http.Parameters
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
    client: HttpClient = HttpClientProvider.create()
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
                .addParameter("scope", "playlist-modify-public playlist-modify-private user-library-read")
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
            val parameters = Parameters.Companion.build {
                uri.query?.split("&")?.forEach { param ->
                    val (key, value) = param.split("=")
                    append(key, URLDecoder.decode(value, "UTF-8"))
                }
            }

            // Create CallbackResponse object and get the code
            val callbackResponse = CallbackResponse.Companion.fromParameters(parameters)

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

    override suspend fun refreshToken(): Result<AccessToken> {
        TODO("Not yet implemented")
    }

    private fun sendSuccessResponse(outputStream: OutputStream) {
        val writer = PrintWriter(outputStream, true)
        writer.print("""
        HTTP/1.1 200 OK
        Content-Type: text/html
        Connection: close

        <html>
        <body>
            <h1>Authentication successful!</h1>
            <p>You can close this window and return to the app</p>
        </body>
        </html>
    """.trimIndent())
        writer.flush()
    }

}