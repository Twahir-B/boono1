package com.aether.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aether.agent.AetherApp
import com.aether.agent.ai.ProviderId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val settings = AetherApp.get().settings
    val scope = rememberCoroutineScope()

    val notch by settings.notchEnabled.collectAsState(initial = true)
    val cursor by settings.cursorEnabled.collectAsState(initial = false)
    val island by settings.islandEnabled.collectAsState(initial = true)
    val voice by settings.voiceEnabled.collectAsState(initial = true)
    val autoSafe by settings.agentAutoApproveSafe.collectAsState(initial = false)
    val provider by settings.defaultProvider.collectAsState(initial = "openai")
    val model by settings.defaultModel.collectAsState(initial = "gpt-4o")

    var providerExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("AI provider", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it }
            ) {
                OutlinedTextField(
                    value = ProviderId.entries.find { it.name.equals(provider, true) }?.displayName ?: provider,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) }
                )
                ExposedDropdownMenu(expanded = providerExpanded, onDismissRequest = { providerExpanded = false }) {
                    ProviderId.entries.filter { it != ProviderId.CUSTOM }.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.displayName) },
                            onClick = {
                                scope.launch { settings.setDefaultProvider(p.name.lowercase()) }
                                providerExpanded = false
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = model,
                onValueChange = { scope.launch { settings.setDefaultModel(it) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model") },
                placeholder = { Text("gpt-4o, claude-3-5-sonnet, gemini-2.0-flash…") },
                singleLine = true
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Features", style = MaterialTheme.typography.titleSmall)
            SwitchRow("Notch gestures", notch) { scope.launch { settings.setNotchEnabled(it) } }
            SwitchRow("Dynamic Island", island) { scope.launch { settings.setIslandEnabled(it) } }
            SwitchRow("Quick Cursor", cursor) { scope.launch { settings.setCursorEnabled(it) } }
            SwitchRow("Voice chat", voice) { scope.launch { settings.setVoiceEnabled(it) } }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Agent", style = MaterialTheme.typography.titleSmall)
            SwitchRow("Auto-approve safe actions", autoSafe) {
                scope.launch { settings.setAgentAutoApproveSafe(it) }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            TextButton(onClick = { nav.navigate("keys") }) { Text("Manage API keys") }
            Text(
                "Keys stay on device (Android Keystore). Android 10 supported.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
