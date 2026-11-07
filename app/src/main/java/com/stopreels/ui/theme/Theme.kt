package com.stopreels.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF12304A),
    onPrimary = Color.White,
    secondary = Color(0xFFE57A44),
    onSecondary = Color.White,
    tertiary = Color(0xFF557A95),
    background = Color(0xFFF6F8FB),
    surface = Color(0xFFF6F8FB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C8E0),
    onPrimary = Color(0xFF0A2338),
    secondary = Color(0xFFFFB48F),
    onSecondary = Color(0xFF3C1608),
    tertiary = Color(0xFFB8D1E5),
)

@Composable
fun StopReelsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
