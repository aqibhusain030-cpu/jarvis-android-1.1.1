package com.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF7C4DFF),
    secondary = Color(0xFF00E5FF),
    background = Color(0xFF05070A),
    surface = Color(0xFF0B0F14),
    onPrimary = Color.White,
    onSurface = Color(0xFFDCE3EC)
)

@Composable
fun JarvisTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, typography = JarvisTypography, content = content)
}
