package com.example.triada.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AulaNexoColors = lightColorScheme(
    primary = Color(0xFFB52D38), // Accessible foreground tone; filled actions retain the brand coral.
    onPrimary = Color.White,
    primaryContainer = WarmSurface,
    onPrimaryContainer = Ink,
    secondary = Sunshine,
    onSecondary = Ink,
    tertiary = Sky,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = WarmSurface,
    onSurfaceVariant = Ink,
    outline = Muted,
    outlineVariant = SoftShadow,
)

@Composable
fun AulaNexoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AulaNexoColors,
        typography = Typography,
        shapes = Shapes(
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
