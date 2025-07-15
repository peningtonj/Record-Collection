package io.github.peningtonj.recordcollection.network.openAi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val input: String,
    val tools: List<Map<String, String>>? =
        emptyList()
//        listOf(mapOf("type" to "web_search_preview")),
)
@Serializable
data class Content(
    val type: String,
    val text: String,
    val annotations: List<String>? = null,
)

@Serializable
data class ChatResponse(
    val output: List<ChatOutput>,
)

@Serializable
data class ChatOutput(
    val type: String,
    val id: String,
    val status: String,
    val role: String? = null,          // <- now optional
    val content: List<Content>? = null // <- now optional
)
