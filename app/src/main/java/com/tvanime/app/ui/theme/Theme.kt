package com.tvanime.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Primary    = Color(0xFF7C5CFC)
private val OnPrimary  = Color(0xFFF5F3FF)
private val Secondary  = Color(0xFF2DD4BF)
private val Background = Color(0xFF0B0B12)
private val Surface    = Color(0xFF151522)
private val SurfaceDim = Color(0xFF1E1E30)
private val OnBg       = Color(0xFFE4E2F0)
private val OnSurface  = Color(0xFFD4D2E8)
private val Outline    = Color(0xFF3A3858)
private val Error      = Color(0xFFEF4444)

private val TVColors = darkColorScheme(
    primary          = Primary,
    onPrimary        = OnPrimary,
    secondary        = Secondary,
    onSecondary      = Color(0xFF0D0D18),
    background       = Background,
    onBackground     = OnBg,
    surface          = Surface,
    onSurface        = OnSurface,
    surfaceVariant   = SurfaceDim,
    onSurfaceVariant = Color(0xFF9A98B4),
    outline          = Outline,
    outlineVariant   = Color(0xFF25253A),
    error            = Error,
    onError          = Color.White
)

@Composable
fun TVAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TVColors,
        content = content
    )
}
