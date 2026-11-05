package com.example.triada.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AulaNexoColors = lightColorScheme(
    primary = ForestText,
    onPrimary = Color.White,
    primaryContainer = ForestContainer,
    onPrimaryContainer = ForestText,
    secondary = HoneyYellow,
    onSecondary = Ink,
    secondaryContainer = ForestContainer,
    onSecondaryContainer = ForestText,
    tertiary = OceanBlue,
    onTertiary = OnOcean,
    tertiaryContainer = OceanContainer,
    onTertiaryContainer = OceanText,
    background = WarmSand,
    onBackground = Ink,
    surface = Color.White,
    surfaceTint = ForestGreen,
    onSurface = Ink,
    surfaceVariant = WarmSurface,
    surfaceDim = WarmSurface,
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = WarmSand,
    surfaceContainer = WarmSurface,
    surfaceContainerHigh = ForestContainer,
    surfaceContainerHighest = WarmSurface,
    onSurfaceVariant = Ink,
    outline = Muted,
    outlineVariant = SoftShadow,
    inverseSurface = Ink,
    inverseOnSurface = WarmSand,
    inversePrimary = ForestGreen,
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
