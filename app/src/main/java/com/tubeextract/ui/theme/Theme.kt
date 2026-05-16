package com.tubeextract.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RedYT = Color(0xFFFF0000)
val DarkBg = Color(0xFF0F0F0F)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceVariant = Color(0xFF282828)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFAAAAAA)

private val DarkColorScheme = darkColorScheme(
    primary = RedYT,
    secondary = Color(0xFFFF4444),
    background = DarkBg,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariant,
    onPrimary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun TubeExtractTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
