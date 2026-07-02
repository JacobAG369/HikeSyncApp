package com.example.hykesync.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = CharcoalText,
    primaryContainer = ForestGreenDeep,
    onPrimaryContainer = SoftText,
    secondary = TrailOrangeLight,
    onSecondary = CharcoalText,
    secondaryContainer = TrailOrangeDeep,
    onSecondaryContainer = SoftText,
    tertiary = TrailOrange,
    background = RockSurface,
    onBackground = SoftText,
    surface = RockSurface,
    onSurface = SoftText,
    surfaceVariant = Color(0xFF1B211B),
    onSurfaceVariant = Color(0xFFC5CEC0),
    outline = Color(0xFF879380),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SoftText,
    primaryContainer = ForestGreenLight,
    onPrimaryContainer = ForestGreenDeep,
    secondary = TrailOrange,
    onSecondary = SoftText,
    secondaryContainer = TrailOrangeLight,
    onSecondaryContainer = TrailOrangeDeep,
    tertiary = TrailOrangeDeep,
    background = MossSurface,
    onBackground = CharcoalText,
    surface = Color.White,
    onSurface = CharcoalText,
    surfaceVariant = MistSurface,
    onSurfaceVariant = SlateOutline,
    outline = SlateOutline,
    error = Color(0xFFB3261E)
)

@Composable
fun HykeSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
