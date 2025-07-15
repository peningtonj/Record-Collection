package io.github.peningtonj.recordcollection.network.openAi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class Message(
    val role: String,
    val content: String,
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    @SerialName("temperature") val temperature: Double = 0.2,
    @SerialName("response_format") val responseFormat: Map<String, String>? = null,
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>,
) {
    @Serializable
    data class Choice(
        val message: Message,
    )
}
