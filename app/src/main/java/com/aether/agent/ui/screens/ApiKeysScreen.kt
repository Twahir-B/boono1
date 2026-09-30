package com.aether.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aether.agent.AetherApp
import com.aether.agent.ai.ProviderId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(nav: NavController) {
    val keyStore = AetherApp.get().keyStore

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Keys") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "Paste your own keys. They never leave this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProviderId.entries.filter { it != ProviderId.CUSTOM }.forEach { provider ->
                KeyField(
                    label = provider.displayName,
                    providerId = provider.name.lowercase(),
                    hasKey = keyStore.hasKey(provider.name.lowercase()),
                    onSave = { keyStore.saveKey(provider.name.lowercase(), it) },
                    onClear = { keyStore.removeKey(provider.name.lowercase()) }
                )
            }
        }
    }
}

@Composable
private fun KeyField(
    label: String,
    providerId: String,
    hasKey: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var value by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(hasKey) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (saved) "••••••••  (key saved)" else "sk-…") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (value.isNotBlank()) {
                        onSave(value)
                        value = ""
                        saved = true
                    }
                },
                enabled = value.isNotBlank()
            ) { Text("Save") }
            if (saved) {
                TextButton(onClick = {
                    onClear()
                    saved = false
                }) { Text("Clear") }
            }
        }
    }
}
