package com.tvanime.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Cinematic Vision palette ──
private val Surface         = Color(0xFF101415)
private val SurfaceDim      = Color(0xFF101415)
private val SurfaceBright   = Color(0xFF363A3B)
private val SurfaceLowest   = Color(0xFF0B0F10)
private val SurfaceLow      = Color(0xFF191C1E)
private val SurfaceContainer = Color(0xFF1D2022)
private val SurfaceHigh     = Color(0xFF272A2C)
private val SurfaceHighest  = Color(0xFF323537)
private val OnSurface       = Color(0xFFE0E3E5)
private val OnSurfaceVariant= Color(0xFFBAC9C9)
private val Primary         = Color(0xFF47EAED)
private val OnPrimary       = Color(0xFF003738)
private val PrimaryContainer = Color(0xFF00CED1)
private val OnPrimaryContainer = Color(0xFF005354)
private val Secondary       = Color(0xFFFFB59C)
private val OnSecondary     = Color(0xFF5C1A00)
private val SecondaryContainer = Color(0xFF8E2C01)
private val Outline         = Color(0xFF859493)
private val OutlineVariant  = Color(0xFF3B4949)
private val Error           = Color(0xFFFFB4AB)
private val OnError         = Color(0xFF690005)
private val ErrorContainer  = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val CinematicColors = darkColorScheme(
    primary             = Primary,
    onPrimary           = OnPrimary,
    primaryContainer    = PrimaryContainer,
    onPrimaryContainer  = OnPrimaryContainer,
    secondary           = Secondary,
    onSecondary         = OnSecondary,
    secondaryContainer  = SecondaryContainer,
    onSecondaryContainer = Color(0xFFFFAA8D),
    background          = Surface,
    onBackground        = OnSurface,
    surface             = SurfaceDim,
    onSurface           = OnSurface,
    surfaceVariant      = SurfaceHighest,
    onSurfaceVariant    = OnSurfaceVariant,
    outline             = Outline,
    outlineVariant      = OutlineVariant,
    error               = Error,
    onError             = OnError,
    errorContainer      = ErrorContainer,
    onErrorContainer    = OnErrorContainer,
    inverseSurface      = OnSurface,
    inverseOnSurface    = Color(0xFF2D3133),
    surfaceTint         = Color(0xFF2DDBDE)
)

@Composable
fun TVAnimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CinematicColors,
        content = content
    )
}
