package com.nyxiaglow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Coral = Color(0xFFFF9A8B)
val CoralDeep = Color(0xFF96463B)
val CoralSoft = Color(0xFFFFC2B9)
val Mist = Color(0xFFF5F1F0)
val Ink = Color(0xFF111111)
val SurfaceDark = Color(0xFF171515)
val SurfaceRaised = Color(0xFF242020)
val TextMuted = Color(0xFFDAC1BD)
val MutedInk = Color(0xFF595F65)

private val NyxiaColors = darkColorScheme(
    primary = Coral,
    onPrimary = CoralDeep,
    secondary = CoralSoft,
    onSecondary = Color(0xFF3B1A16),
    background = Ink,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextMuted,
    error = Color(0xFFD96868),
    onError = Color.White
)

@Composable
fun NyxiaGlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NyxiaColors,
        content = content
    )
}
