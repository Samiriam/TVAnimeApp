package com.tvanime.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleDark  = Color(0xFF6D28D9)
private val Background  = Color(0xFF08080E)
private val Surface     = Color(0xFF141420)

private val DarkColors = darkColorScheme(
    primary   = PurpleDark,
    secondary = PurpleDark,
    background = Background,
    surface   = Surface,
    onPrimary   = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE5E3FF),
    onSurface   = Color.White,
    surfaceVariant = Color(0xFF202033),
    onSurfaceVariant = Color(0xFFE5E3FF),
    outline = Color(0xFF77709A)
)

@Composable
fun TVAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
