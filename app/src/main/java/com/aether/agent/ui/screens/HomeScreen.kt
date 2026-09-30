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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.aether.agent.service.AetherAccessibilityService
import com.aether.agent.service.CursorTriggerService
import com.aether.agent.service.IslandService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    var accessibilityOn by remember { mutableStateOf(AetherAccessibilityService.isEnabled()) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    // Refresh when user returns from Settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityOn = AetherAccessibilityService.isEnabled()
                overlayGranted = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Do these steps in order (Android 10)",
                style = MaterialTheme.typography.titleMedium
            )

            Text("1. Permissions", style = MaterialTheme.typography.titleSmall)

            StatusCard(
                title = "Accessibility",
                subtitle = if (accessibilityOn) "ON — gestures & agent can tap"
                else "OFF — required for Cursor taps & agent",
                ok = accessibilityOn,
                actionLabel = if (accessibilityOn) "Open" else "Enable now",
                onAction = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )

            StatusCard(
                title = "Display over other apps",
                subtitle = if (overlayGranted) "ON — Island can show"
                else "OFF — required for Island & Cursor",
                ok = overlayGranted,
                actionLabel = if (overlayGranted) "Open" else "Grant now",
                onAction = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )

            HorizontalDivider()
            Text("2. Start features", style = MaterialTheme.typography.titleSmall)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { IslandService.start(context) },
                    modifier = Modifier.weight(1f),
                    enabled = overlayGranted
                ) {
                    Icon(Icons.Default.CropFree, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start Island")
                }
                OutlinedButton(
                    onClick = { IslandService.stop(context) },
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
            }

            Text(
                "After Start Island: look at the TOP of the screen for a black pill. Tap it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = { CursorTriggerService.start(context) },
                    modifier = Modifier.weight(1f),
                    enabled = overlayGranted
                ) {
                    Icon(Icons.Default.PanTool, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Start Cursor")
                }
                OutlinedButton(
                    onClick = { CursorTriggerService.stop(context) },
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
            }

            Text(
                "After Start Cursor: swipe inward from the bottom-left or bottom-right edge.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()
            Text("3. AI", style = MaterialTheme.typography.titleSmall)

            Button(
                onClick = { nav.navigate("keys") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Key, null)
                Spacer(Modifier.width(8.dp))
                Text("API Keys (required for Chat)")
            }

            Button(
                onClick = { nav.navigate("chat") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Chat, null)
                Spacer(Modifier.width(8.dp))
                Text("Open Chat / Agent")
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
                "If Island still does not appear: disable battery optimization for Aether in system Settings.",
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
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
