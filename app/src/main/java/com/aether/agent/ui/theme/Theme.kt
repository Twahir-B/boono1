package com.aether.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7C9CFF),
    onPrimary = Color(0xFF002B75),
    secondary = Color(0xFFB0C6FF),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF151922),
    onBackground = Color(0xFFE8EAED),
    onSurface = Color(0xFFE8EAED)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color.White,
    secondary = Color(0xFF5C7CFA),
    background = Color(0xFFF8F9FC),
    surface = Color.White,
    onBackground = Color(0xFF1A1C20),
    onSurface = Color(0xFF1A1C20)
)

@Composable
fun AetherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
