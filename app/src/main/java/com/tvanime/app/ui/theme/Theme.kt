package com.tvanime.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleLight = Color(0xFF7C3AED)
private val PurpleDark  = Color(0xFF6D28D9)
private val Background  = Color(0xFF08080E)
private val Surface     = Color(0xFF141420)

private val LightColors = lightColorScheme(
    primary   = PurpleLight,
    secondary = PurpleLight,
    background = Color(0xFFF5F3FF),
    surface   = Surface,
    onPrimary   = Color.White,
    onSecondary = Color.White,
    onBackground= Color(0xFF1A1A2E),
    onSurface   = Color.White
)

private val DarkColors = darkColorScheme(
    primary   = PurpleDark,
    secondary = PurpleDark,
    background = Background,
    surface   = Surface,
    onPrimary   = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE5E3FF),
    onSurface   = Color.White
)

@Composable
fun TVAnimeTheme(content: @Composable () -> Unit) {
    val darkMode = isSystemInDarkTheme()
    val colors = if (darkMode) DarkColors else LightColors
    androidx.compose.material3.MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
