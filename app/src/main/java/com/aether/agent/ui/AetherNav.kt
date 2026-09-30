package com.aether.agent.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aether.agent.ui.screens.*

@Composable
fun AetherNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("chat") { ChatScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
        composable("keys") { ApiKeysScreen(nav) }
        composable("permissions") { PermissionsScreen(nav) }
        composable("gestures") { GestureSettingsScreen(nav) }
    }
}
