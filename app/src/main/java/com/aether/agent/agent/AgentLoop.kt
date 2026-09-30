package com.aether.agent.agent

import android.util.Base64
import com.aether.agent.ai.*
import com.aether.agent.service.AetherAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Lightweight agent harness:
 * 1. Observe (UI tree + optional screenshot)
 * 2. Ask model for next action (JSON)
 * 3. Execute (with optional user approval)
 * 4. Repeat until done
 */
class AgentLoop(
    private val provider: ProviderId,
    private val model: String
) {
    data class AgentAction(
        val type: String,          // tap | swipe | back | home | type | done | speak
        val elementId: Int? = null,
        val x: Int? = null,
        val y: Int? = null,
        val text: String? = null,
        val message: String? = null
    )

    data class StepResult(
        val observation: String,
        val action: AgentAction?,
        val assistantText: String
    )

    suspend fun runStep(
        userGoal: String,
        screenshotJpeg: ByteArray? = null,
        requireApproval: (AgentAction) -> Boolean = { true }
    ): StepResult = withContext(Dispatchers.Default) {
        val service = AetherAccessibilityService.instance
            ?: return@withContext StepResult("Accessibility service not enabled", null, "")

        val tree = service.captureUiTree()
        val observation = buildString {
            appendLine("Current screen package: ${tree.packageName}")
            appendLine("UI elements (id, type, text, clickable):")
            append(tree.toCompactJson())
        }

        val images = if (screenshotJpeg != null) {
            listOf(Base64.encodeToString(screenshotJpeg, Base64.NO_WRAP))
        } else emptyList()

        val system = """
            You are Aether, an Android agent. You see the UI tree and optional screenshot.
            Reply with a short reasoning, then a single JSON action on its own line:
            {"type":"tap","elementId":5} or {"type":"tap","x":100,"y":200}
            or {"type":"swipe","x":100,"y":800,"x2":100,"y2":200}
            or {"type":"back"} | {"type":"home"} | {"type":"type","text":"..."}
            or {"type":"done","message":"summary"} | {"type":"speak","message":"..."}
            Prefer elementId over coordinates when possible. Never invent element ids.
        """.trimIndent()

        val messages = listOf(
            ChatMessage("system", system),
            ChatMessage("user", "Goal: $userGoal\n\n$observation", images)
        )

        val client = AiClientFactory.create(provider)
        var full = ""
        client.chat(
            ChatRequest(provider, model, messages, temperature = 0.2, stream = true)
        ) { event ->
            when (event) {
                is ChatEvent.Delta -> full += event.text
                is ChatEvent.Done -> full = event.fullText.ifBlank { full }
                is ChatEvent.Error -> full = "Error: ${event.message}"
            }
        }

        val action = parseAction(full)
        if (action != null && action.type != "done" && action.type != "speak") {
            if (requireApproval(action)) {
                execute(service, tree, action)
            }
        }

        StepResult(observation, action, full)
    }

    private fun parseAction(text: String): AgentAction? {
        val start = text.lastIndexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return try {
            val json = org.json.JSONObject(text.substring(start, end + 1))
            AgentAction(
                type = json.optString("type", ""),
                elementId = if (json.has("elementId")) json.getInt("elementId") else null,
                x = if (json.has("x")) json.getInt("x") else null,
                y = if (json.has("y")) json.getInt("y") else null,
                text = json.optString("text").ifBlank { null },
                message = json.optString("message").ifBlank { null }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun execute(service: AetherAccessibilityService, tree: UiTree, action: AgentAction) {
        when (action.type) {
            "tap" -> {
                val el = action.elementId?.let { id -> tree.elements.find { it.id == id } }
                val x = el?.centerX()?.toFloat() ?: action.x?.toFloat()
                val y = el?.centerY()?.toFloat() ?: action.y?.toFloat()
                if (x != null && y != null) service.tap(x, y)
            }
            "swipe" -> {
                // Simple vertical swipe helper; extend as needed
                val x = action.x?.toFloat() ?: 500f
                val y1 = action.y?.toFloat() ?: 1200f
                service.swipe(x, y1, x, y1 - 600f)
            }
            "back" -> service.globalBack()
            "home" -> service.globalHome()
            "type" -> {
                // Typing requires focus + AccessibilityNodeInfo.performAction; simplified here
            }
        }
    }
}
