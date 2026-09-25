package com.scorekeeper.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Matches the wireframe palette: warm cream surfaces, felt-green primary, amber accent.
val Green = Color(0xFF1F6F54)
val GreenDark = Color(0xFF164F3C)
val Amber = Color(0xFFE0A438)
val Cream = Color(0xFFFAF7F2)
val Border = Color(0xFFE8E3D9)
val Muted = Color(0xFF6B6660)
val Danger = Color(0xFFC0463C)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    secondary = Amber,
    background = Cream,
    surface = Color.White,
    error = Danger
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FC4AA),
    secondary = Amber,
    error = Danger
)

@Composable
fun ScoreKeeperTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
