package com.realme.modxposed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkSlateBackground = Color(0xFF0F172A) // Slate 900
val DarkSlateSurface = Color(0xFF1E293B)    // Slate 800
val DarkSlateCard = Color(0xFF334155)       // Slate 700
val AccentIndigo = Color(0xFF818CF8)        // Indigo 400
val AccentCyan = Color(0xFF38BDF8)          // Cyan 400
val TextPrimary = Color(0xFFF8FAFC)         // Slate 50
val TextSecondary = Color(0xFF94A3B8)       // Slate 400

private val DarkColorScheme = darkColorScheme(
    primary = AccentIndigo,
    secondary = AccentCyan,
    background = DarkSlateBackground,
    surface = DarkSlateSurface,
    surfaceVariant = DarkSlateCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun RealmeModTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
