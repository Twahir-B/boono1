package com.aether.agent.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aether.agent.service.AetherAccessibilityService
import com.aether.agent.service.IslandService
import com.aether.agent.service.CursorTriggerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val accessibilityOn = AetherAccessibilityService.isEnabled()
    var overlayGranted by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aether") },
                actions = {
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Notch · Island · Cursor · AI Agent",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status cards
            StatusCard(
                title = "Accessibility",
                subtitle = if (accessibilityOn) "Enabled — gestures & agent ready"
                else "Required for notch, cursor & agent actions",
                ok = accessibilityOn,
                actionLabel = if (accessibilityOn) null else "Enable",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            StatusCard(
                title = "Display over other apps",
                subtitle = if (overlayGranted) "Granted — Island can appear"
                else "Required for Dynamic Island overlay",
                ok = overlayGranted,
                actionLabel = if (overlayGranted) null else "Grant",
                onAction = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                    overlayGranted = Settings.canDrawOverlays(context)
                }
            )

            HorizontalDivider()

            Text("Quick actions", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { IslandService.start(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CropFree, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Island")
                }
                OutlinedButton(
                    onClick = { IslandService.stop(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Island")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { CursorTriggerService.start(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PanTool, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Cursor")
                }
                OutlinedButton(
                    onClick = { CursorTriggerService.stop(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Cursor")
                }
            }

            Button(
                onClick = { nav.navigate("chat") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Chat, null)
                Spacer(Modifier.width(8.dp))
                Text("Open AI Chat / Agent")
            }

            OutlinedButton(
                onClick = { nav.navigate("keys") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, null)
                Spacer(Modifier.width(8.dp))
                Text("API Keys (BYOK)")
            }

            OutlinedButton(
                onClick = { nav.navigate("permissions") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, null)
                Spacer(Modifier.width(8.dp))
                Text("Permissions guide")
            }

            Text(
                "Your API keys stay on device. Aether never proxies them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String,
    ok: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ok)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            if (actionLabel != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
