package com.aether.agent.ai

import com.aether.agent.AetherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Works for OpenAI, Groq, OpenRouter, xAI, and any OpenAI-compatible endpoint.
 * Anthropic and Gemini need slight adapters (same interface).
 */
class OpenAiCompatibleClient(
    private val provider: ProviderId,
    private val baseUrlOverride: String? = null
) : AiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun apiKey(): String {
        val key = AetherApp.get().keyStore.getKey(provider.name.lowercase())
            ?: throw IllegalStateException("No API key for ${provider.displayName}. Add it in Settings.")
        return key
    }

    private fun baseUrl(): String {
        if (!baseUrlOverride.isNullOrBlank()) return baseUrlOverride.trimEnd('/')
        return provider.defaultBaseUrl.trimEnd('/')
    }

    override fun supportsVision(model: String): Boolean {
        val m = model.lowercase()
        return m.contains("gpt-4o") || m.contains("gpt-4-turbo") || m.contains("gpt-4.1") ||
                m.contains("claude-3") || m.contains("gemini") || m.contains("grok") ||
                m.contains("vision")
    }

    override suspend fun chat(request: ChatRequest, onEvent: (ChatEvent) -> Unit) =
        withContext(Dispatchers.IO) {
            try {
                val body = buildRequestBody(request)
                val httpRequest = Request.Builder()
                    .url("${baseUrl()}/chat/completions")
                    .addHeader("Authorization", "Bearer ${apiKey()}")
                    .addHeader("Content-Type", "application/json")
                    .apply {
                        if (provider == ProviderId.OPENROUTER) {
                            addHeader("HTTP-Referer", "https://aether.agent")
                            addHeader("X-Title", "Aether")
                        }
                    }
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string() ?: "HTTP ${response.code}"
                        onEvent(ChatEvent.Error("Provider error: $err"))
                        return@withContext
                    }

                    val source = response.body?.source() ?: run {
                        onEvent(ChatEvent.Error("Empty response body"))
                        return@withContext
                    }

                    val full = StringBuilder()
                    if (request.stream) {
                        while (!source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data: ")) continue
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") break
                            try {
                                val json = JSONObject(data)
                                val delta = json.optJSONArray("choices")
                                    ?.optJSONObject(0)
                                    ?.optJSONObject("delta")
                                    ?.optString("content")
                                if (!delta.isNullOrEmpty()) {
                                    full.append(delta)
                                    onEvent(ChatEvent.Delta(delta))
                                }
                            } catch (_: Exception) { /* ignore partial JSON */ }
                        }
                        onEvent(ChatEvent.Done(full.toString()))
                    } else {
                        val text = response.body?.string() ?: ""
                        val content = JSONObject(text)
                            .optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content") ?: text
                        onEvent(ChatEvent.Done(content))
                    }
                }
            } catch (e: Exception) {
                onEvent(ChatEvent.Error(e.message ?: "Unknown error"))
            }
        }

    private fun buildRequestBody(request: ChatRequest): String {
        val messages = JSONArray()
        for (m in request.messages) {
            val msg = JSONObject().put("role", m.role)
            if (m.imagesBase64.isEmpty()) {
                msg.put("content", m.content)
            } else {
                // Vision content array
                val parts = JSONArray()
                parts.put(JSONObject().put("type", "text").put("text", m.content))
                for (img in m.imagesBase64) {
                    parts.put(
                        JSONObject()
                            .put("type", "image_url")
                            .put(
                                "image_url",
                                JSONObject().put("url", "data:image/jpeg;base64,$img")
                            )
                    )
                }
                msg.put("content", parts)
            }
            messages.put(msg)
        }

        val root = JSONObject()
            .put("model", request.model)
            .put("messages", messages)
            .put("stream", request.stream)
            .put("temperature", request.temperature)
        request.maxTokens?.let { root.put("max_tokens", it) }
        return root.toString()
    }
}

object AiClientFactory {
    fun create(provider: ProviderId, customBaseUrl: String? = null): AiClient {
        return when (provider) {
            ProviderId.ANTHROPIC -> OpenAiCompatibleClient(provider, customBaseUrl) // swap later for native Anthropic
            ProviderId.GEMINI -> OpenAiCompatibleClient(provider, customBaseUrl)     // swap later for native Gemini
            else -> OpenAiCompatibleClient(provider, customBaseUrl)
        }
    }
}
