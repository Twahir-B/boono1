package com.aether.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aether.agent.AetherApp
import com.aether.agent.agent.AgentLoop
import com.aether.agent.ai.*
import com.aether.agent.service.AetherAccessibilityService
import com.aether.agent.service.ScreenCaptureService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class UiMessage(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = AetherApp.get().settings

    val providerId by settings.defaultProvider.collectAsState(initial = "openai")
    val modelName by settings.defaultModel.collectAsState(initial = "gpt-4o")

    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<UiMessage>()) }
    var streaming by remember { mutableStateOf(false) }
    var agentMode by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<AgentLoop.AgentAction?>(null) }
    var pendingGoal by remember { mutableStateOf<String?>(null) }
    var voiceHelper by remember { mutableStateOf<VoiceHelper?>(null) }
    var listening by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        val vh = VoiceHelper(context)
        vh.init()
        voiceHelper = vh
        onDispose { vh.shutdown() }
    }

    val provider = remember(providerId) {
        ProviderId.entries.find { it.name.equals(providerId, true) } ?: ProviderId.OPENAI
    }

    if (pendingAction != null) {
        val action = pendingAction!!
        AlertDialog(
            onDismissRequest = {
                pendingAction = null
                pendingGoal = null
                streaming = false
            },
            title = { Text("Approve agent action?") },
            text = {
                Text(
                    buildString {
                        append("Type: ${action.type}")
                        action.elementId?.let { append("\nElement id: $it") }
                        action.x?.let { append("\nx: $it") }
                        action.y?.let { append("\ny: $it") }
                        action.text?.let { append("\nText: $it") }
                        action.message?.let { append("\n$it") }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val act = action
                    pendingAction = null
                    pendingGoal = null
                    scope.launch {
                        executeApproved(act)
                        messages = messages + UiMessage("assistant", "Approved and executed: ${act.type}")
                        streaming = false
                    }
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    messages = messages + UiMessage("assistant", "Action cancelled.")
                    pendingAction = null
                    pendingGoal = null
                    streaming = false
                }) { Text("Deny") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (agentMode) "Agent" else "Chat")
                        Text(
                            "${provider.displayName} · $modelName",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { ScreenCaptureActivity.request(context) }) {
                        Icon(Icons.Default.Visibility, "Screen vision")
                    }
                    FilterChip(
                        selected = agentMode,
                        onClick = { agentMode = !agentMode },
                        label = { Text("Agent") },
                        leadingIcon = {
                            Icon(Icons.Default.SmartToy, null, Modifier.size(16.dp))
                        }
                    )
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (listening) {
                        voiceHelper?.stopListening()
                        listening = false
                    } else {
                        listening = true
                        voiceHelper?.listen(
                            onResult = { text ->
                                listening = false
                                input = text
                            },
                            onError = { err ->
                                listening = false
                                messages = messages + UiMessage("assistant", err)
                            }
                        )
                    }
                }) {
                    Icon(
                        Icons.Default.Mic, "Voice",
                        tint = if (listening) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            when {
                                listening -> "Listening…"
                                agentMode -> "Goal for the agent…"
                                else -> "Message…"
                            }
                        )
                    },
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (input.isBlank() || streaming) return@FilledIconButton
                        val text = input.trim()
                        input = ""
                        messages = messages + UiMessage("user", text)
                        streaming = true
                        scope.launch {
                            try {
                                if (agentMode) {
                                    if (!AetherAccessibilityService.isEnabled()) {
                                        messages += UiMessage("assistant", "Enable Accessibility first (Home).")
                                        streaming = false
                                        return@launch
                                    }
                                    val loop = AgentLoop(provider, modelName)
                                    val shot = withContext(Dispatchers.IO) { captureScreenshot() }
                                    val result = loop.runStep(text, screenshotJpeg = shot) { action ->
                                        if (action.type in listOf("done", "speak")) true
                                        else {
                                            pendingAction = action
                                            pendingGoal = text
                                            false
                                        }
                                    }
                                    if (pendingAction == null) {
                                        messages += UiMessage("assistant", result.assistantText)
                                        result.action?.message?.let { voiceHelper?.speak(it) }
                                        streaming = false
                                    }
                                } else {
                                    val reply = runChat(provider, modelName, text, messages)
                                    messages += UiMessage("assistant", reply)
                                    voiceHelper?.speak(reply.take(400))
                                    streaming = false
                                }
                            } catch (e: Exception) {
                                messages += UiMessage("assistant", "Error: ${e.message}")
                                streaming = false
                            }
                            listState.animateScrollToItem(messages.lastIndex.coerceAtLeast(0))
                        }
                    },
                    enabled = !streaming && input.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 320.dp)
                    ) {
                        Text(msg.text, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (streaming && pendingAction == null) {
                item {
                    Text("Thinking…", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private suspend fun runChat(
    provider: ProviderId,
    model: String,
    userText: String,
    history: List<UiMessage>
): String = withContext(Dispatchers.IO) {
    val client = AiClientFactory.create(provider)
    val chatMessages = history.dropLast(1).map {
        ChatMessage(if (it.role == "user") "user" else "assistant", it.text)
    } + ChatMessage("user", userText)

    suspendCancellableCoroutine { cont ->
        var full = ""
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            client.chat(ChatRequest(provider, model, chatMessages)) { event ->
                when (event) {
                    is ChatEvent.Delta -> full += event.text
                    is ChatEvent.Done -> if (cont.isActive) cont.resume(event.fullText.ifBlank { full })
                    is ChatEvent.Error -> if (cont.isActive) cont.resume("Error: ${event.message}")
                }
            }
        }
    }
}

private fun captureScreenshot(): ByteArray? = try {
    ScreenCaptureService.instance?.captureJpeg(65)
} catch (_: Exception) { null }

private fun executeApproved(action: AgentLoop.AgentAction) {
    val service = AetherAccessibilityService.instance ?: return
    val tree = service.captureUiTree()
    when (action.type) {
        "tap" -> {
            val el = action.elementId?.let { id -> tree.elements.find { it.id == id } }
            val x = el?.centerX()?.toFloat() ?: action.x?.toFloat()
            val y = el?.centerY()?.toFloat() ?: action.y?.toFloat()
            if (x != null && y != null) service.tap(x, y)
        }
        "swipe" -> {
            val x = action.x?.toFloat() ?: 500f
            val y1 = action.y?.toFloat() ?: 1200f
            service.swipe(x, y1, x, y1 - 600f)
        }
        "back" -> service.globalBack()
        "home" -> service.globalHome()
    }
}
