package io.github.peningtonj.recordcollection.network.oauth.spotify

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.encodeBase64
import kotlinx.serialization.Serializable
import java.awt.Desktop
import java.io.OutputStream
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest

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

    override fun getRedirectUri() = "http://127.0.0.1:8888/callback"

    /**
     * SHA-256 implementation for desktop using Java's MessageDigest
     */
    override fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        
        // Base64 URL encoding
        return digest.encodeBase64()
            .replace("+", "-")
            .replace("/", "_")
            .replace("=", "")
    }

    override suspend fun authenticate(): Result<String> {
        Napier.d { "Starting Desktop Authentication with PKCE" }

        // Generate PKCE parameters for this auth flow
        currentPKCEParams = generatePKCEParams()
        Napier.d { "Generated PKCE code_challenge" }

        val server = ServerSocket(8888)
        return try {
            val state = generateState()
            val authUrl = buildAuthorizationUrl(currentPKCEParams!!.codeChallenge, state)

            Napier.d { "Opening browser for authentication" }
            Desktop.getDesktop().browse(URI(authUrl))
            
            Napier.d { "Waiting for callback on port 8888..." }
            val socket = server.accept()
            val reader = socket.getInputStream().bufferedReader()
            val requestLine = reader.readLine()

            // Parse the HTTP request
            val uri = URI(requestLine.split(" ")[1])

            // Create Parameters object from query string
            val parameters = Parameters.build {
                uri.query?.split("&")?.forEach { param ->
                    val parts = param.split("=")
                    if (parts.size == 2) {
                        append(parts[0], URLDecoder.decode(parts[1], "UTF-8"))
                    }
                }
            }

            // Verify state parameter
            val receivedState = parameters["state"]
            if (receivedState != state) {
                throw SecurityException("State parameter mismatch - possible CSRF attack")
            }

            // Create CallbackResponse object and get the code
            val callbackResponse = CallbackResponse.fromParameters(parameters)

            // Send success response to browser
            val outputStream = socket.getOutputStream()
            sendSuccessResponse(outputStream)
            socket.close()
            
            Napier.d("Received authorization code")

            Result.success(callbackResponse.code)
        } catch (e: Exception) {
            Napier.e("Authentication failed", e)
            Result.failure(e)
        } finally {
            server.close()
        }
    }

    /**
     * Generate a random state parameter for CSRF protection
     */
    private fun generateState(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..32)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun sendSuccessResponse(outputStream: OutputStream) {
        val writer = PrintWriter(outputStream, true)
        writer.print(
            """
        HTTP/1.1 200 OK
        Content-Type: text/html
        Connection: close

        <!DOCTYPE html>
        <html>
        <head>
            <title>Authentication Successful</title>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    min-height: 100vh;
                    margin: 0;
                    background-color: #f5f5f5;
                }
                .container {
                    text-align: center;
                    padding: 48px;
                    background: white;
                    border-radius: 8px;
                    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                    max-width: 400px;
                }
                .success-icon {
                    width: 64px;
                    height: 64px;
                    margin: 0 auto 24px;
                    background-color: #1DB954;
                    border-radius: 50%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-size: 32px;
                }
                h1 {
                    font-size: 24px;
                    font-weight: 600;
                    color: #191414;
                    margin: 0 0 8px 0;
                }
                p {
                    font-size: 16px;
                    color: #535353;
                    margin: 0 0 24px 0;
                    line-height: 1.5;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="success-icon">✓</div>
                <h1>Authentication Successful</h1>
                <p>You can now close this window and return to Record Collection.</p>
            </div>
        </body>
        </html>
    """.trimIndent()
        )
        writer.flush()
    }
}