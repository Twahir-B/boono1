package com.aether.agent.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(nav: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Aether needs a few special permissions. Each is used only for the feature described.",
                style = MaterialTheme.typography.bodyMedium
            )

            PermissionItem(
                title = "Accessibility",
                body = "Notch gestures, Quick Cursor, and agent taps/swipes. Does not read passwords or log keystrokes. On Android 13+, first open App info → ⋮ menu → Allow restricted settings, or this toggle stays greyed out.",
                onOpen = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            PermissionItem(
                title = "Display over other apps",
                body = "Shows the Dynamic Island / notch touch zone on top of other apps.",
                onOpen = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )
            PermissionItem(
                title = "Notification access (for Island)",
                body = "Required for the Island to show notifications and now-playing media. Same restricted-settings step as Accessibility on Android 13+.",
                onOpen = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )
            PermissionItem(
                title = "Microphone",
                body = "Real-time voice chat with the AI.",
                onOpen = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            )
            PermissionItem(
                title = "Screen capture",
                body = "One-shot screenshots so the AI can see what’s on screen. You approve each session.",
                onOpen = { /* launched from agent when needed */ }
            )
        }
    }
}

@Composable
private fun PermissionItem(title: String, body: String, onOpen: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpen) { Text("Open settings") }
        }
    }
}
