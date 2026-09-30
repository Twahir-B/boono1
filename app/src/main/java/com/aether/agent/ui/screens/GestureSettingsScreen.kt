package com.aether.agent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aether.agent.gestures.GestureSettings
import com.aether.agent.gestures.NotchAction
import com.aether.agent.gestures.NotchGesture
import com.aether.agent.service.IslandService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(nav: NavController) {
    val context = LocalContext.current
    val gs = remember { GestureSettings(context) }
    val scope = rememberCoroutineScope()

    val single by gs.singleTap.collectAsState(NotchAction.OPEN_AETHER)
    val double by gs.doubleTap.collectAsState(NotchAction.NOTIFICATIONS)
    val long by gs.longPress.collectAsState(NotchAction.OPEN_CHAT)
    val left by gs.swipeLeft.collectAsState(NotchAction.MEDIA_PREV)
    val right by gs.swipeRight.collectAsState(NotchAction.MEDIA_NEXT)
    val down by gs.swipeDown.collectAsState(NotchAction.NOTIFICATIONS)
    val up by gs.swipeUp.collectAsState(NotchAction.HOME)

    val widthFrac by gs.islandWidthFrac.collectAsState(0.40f)
    val heightDp by gs.islandHeightDp.collectAsState(40f)
    val offsetY by gs.islandOffsetYDp.collectAsState(8f)
    val offsetX by gs.islandOffsetXDp.collectAsState(0f)
    val visible by gs.islandVisible.collectAsState(true)

    fun applyAndReload() {
        IslandService.reload(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notch & Island") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { applyAndReload() }) { Text("Apply") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Gestures — pick an action for each", style = MaterialTheme.typography.titleSmall)

            GestureRow("Single tap", single) {
                scope.launch { gs.setGesture(NotchGesture.SINGLE_TAP, it); applyAndReload() }
            }
            GestureRow("Double tap", double) {
                scope.launch { gs.setGesture(NotchGesture.DOUBLE_TAP, it); applyAndReload() }
            }
            GestureRow("Long press", long) {
                scope.launch { gs.setGesture(NotchGesture.LONG_PRESS, it); applyAndReload() }
            }
            GestureRow("Swipe left", left) {
                scope.launch { gs.setGesture(NotchGesture.SWIPE_LEFT, it); applyAndReload() }
            }
            GestureRow("Swipe right", right) {
                scope.launch { gs.setGesture(NotchGesture.SWIPE_RIGHT, it); applyAndReload() }
            }
            GestureRow("Swipe down", down) {
                scope.launch { gs.setGesture(NotchGesture.SWIPE_DOWN, it); applyAndReload() }
            }
            GestureRow("Swipe up", up) {
                scope.launch { gs.setGesture(NotchGesture.SWIPE_UP, it); applyAndReload() }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("Island shape & position", style = MaterialTheme.typography.titleSmall)

            SwitchRow("Show black pill", visible) {
                scope.launch { gs.setIslandVisible(it); applyAndReload() }
            }

            Text("Width: ${(widthFrac * 100).toInt()}% of screen")
            Slider(
                value = widthFrac,
                onValueChange = { scope.launch { gs.setIslandWidthFrac(it) } },
                onValueChangeFinished = { applyAndReload() },
                valueRange = 0.2f..0.85f
            )

            Text("Height: ${heightDp.toInt()} dp")
            Slider(
                value = heightDp,
                onValueChange = { scope.launch { gs.setIslandHeightDp(it) } },
                onValueChangeFinished = { applyAndReload() },
                valueRange = 28f..80f
            )

            Text("Move down from top: ${offsetY.toInt()} dp")
            Slider(
                value = offsetY,
                onValueChange = { scope.launch { gs.setIslandOffsetYDp(it) } },
                onValueChangeFinished = { applyAndReload() },
                valueRange = 0f..80f
            )

            Text("Move left / right: ${offsetX.toInt()} dp")
            Slider(
                value = offsetX,
                onValueChange = { scope.launch { gs.setIslandOffsetXDp(it) } },
                onValueChangeFinished = { applyAndReload() },
                valueRange = -80f..80f
            )

            Text(
                "After changing size/position, tap Apply or restart Island from Home.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureRow(title: String, current: NotchAction, onPick: (NotchAction) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = current.label,
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) }
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            NotchAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    onClick = {
                        onPick(action)
                        open = false
                    }
                )
            }
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
