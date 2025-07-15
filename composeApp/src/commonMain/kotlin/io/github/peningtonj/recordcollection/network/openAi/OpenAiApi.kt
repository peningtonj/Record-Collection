package io.github.peningtonj.recordcollection.network.openAi

import io.github.peningtonj.recordcollection.network.openAi.model.ChatRequest
import io.github.peningtonj.recordcollection.network.openAi.model.ChatResponse
import io.github.peningtonj.recordcollection.network.openAi.model.Message
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

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
    private val model: String = "gpt-3.5-turbo",
) {
    /**
     * Sends a prompt to the model and returns the assistant’s reply.
     *
     * @param prompt      user text to send
     * @param formatJson  optional: set to true if you want the model to
     *                    validate its reply as JSON (requires GPT‑4o or GPT‑3.5‑turbo‑1106).
     */
    suspend fun prompt(
        prompt: String,
        formatJson: Boolean = false,
    ): String {
        val reqBody = ChatRequest(
            model = model,
            messages = listOf(Message("user", prompt)),
            responseFormat = if (formatJson) mapOf("type" to "json_object") else null,
        )

        val response: ChatResponse = client.post("https://api.openai.com/v1/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(reqBody)
        }.body()

        return response.choices.firstOrNull()?.message?.content
            ?: error("No choices returned from OpenAI")
    }
}
