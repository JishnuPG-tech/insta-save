package com.instasave.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = PitchBlack,
    secondary = TextSecondary,
    onSecondary = PitchBlack,
    background = PitchBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = SubduedBorder,
    error = AccentError,
    onError = PitchBlack
)

@Composable
fun InstaSaveTheme(
    content: @Composable () -> Unit
) {
    // Dynamic color is explicitly disabled by design for the pitch-black OLED aesthetic
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
