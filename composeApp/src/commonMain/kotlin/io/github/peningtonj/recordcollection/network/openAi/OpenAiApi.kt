package io.github.peningtonj.recordcollection.network.openAi

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.openAi.model.ChatRequest
import io.github.peningtonj.recordcollection.network.openAi.model.ChatResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import net.dankito.readability4j.Readability4J

/**
 * Very small OpenAI wrapper for chat‑style prompts.
 *
 * @param client  a Ktor HttpClient with JSON serialization installed
 * @param apiKey  your OpenAI secret key, e.g. System.getenv("OPENAI_API_KEY")
 * @param model   chat model to use (default "gpt-3.5-turbo")
 */
class OpenAiApi(
    private val client: HttpClient,
    private val apiKey: String,
    private val model: String = "gpt-4.1",
) {

    private val openAiJson = Json {
        ignoreUnknownKeys = true
    }

    suspend fun getUrlContent(url: String): String {
        val html = client.get(url).body<String>()
        val readability = Readability4J(url, html)
        val article = readability.parse()
        return article.textContent ?: error("No content found in $url")
    }
    /**
     * Sends a prompt to the model and returns the assistant’s reply.
     *
     * @param prompt      user text to send
     */
    suspend fun prompt(prompt: String): String {
        Napier.d { "Sending prompt: $prompt" }

        val reqBody = ChatRequest(model, prompt)

        val raw = client.post("https://api.openai.com/v1/responses") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(reqBody)
            timeout {
                requestTimeoutMillis = 120_000
                socketTimeoutMillis = 120_000
            }
        }.body<String>()

        Napier.d { "Response: $raw" }

        val chatResponse = openAiJson.decodeFromString<ChatResponse>(raw)
        return chatResponse.firstAssistantText()
    }


    private fun ChatResponse.firstAssistantText(): String =
        output
            .firstOrNull { it.type == "message" }          // skip web_search_call
            ?.content                                      // may be null
            ?.firstOrNull { it.type == "output_text" }     // skip images etc.
            ?.text
            ?: error("No assistant text in response")
}
