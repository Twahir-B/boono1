package com.aether.agent.ai

/**
 * Multi-provider AI abstraction.
 * User pastes their own API key; all requests go directly from the device to the provider.
 */
enum class ProviderId(val displayName: String, val defaultBaseUrl: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta"),
    XAI("xAI Grok", "https://api.x.ai/v1"),
    GROQ("Groq", "https://api.groq.com/openai/v1"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1"),
    CUSTOM("Custom (OpenAI-compatible)", "")
}

data class ChatMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String,
    val imagesBase64: List<String> = emptyList() // for vision
)

data class ChatRequest(
    val provider: ProviderId,
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val stream: Boolean = true
)

sealed class ChatEvent {
    data class Delta(val text: String) : ChatEvent()
    data class Done(val fullText: String) : ChatEvent()
    data class Error(val message: String) : ChatEvent()
}

interface AiClient {
    suspend fun chat(request: ChatRequest, onEvent: (ChatEvent) -> Unit)
    fun supportsVision(model: String): Boolean
}
