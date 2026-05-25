package com.example.touchgrassirl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TouchGrassColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = LeafLight,
    onPrimaryContainer = DeepForest,
    secondary = MeadowGreen,
    onSecondary = Color.White,
    secondaryContainer = SkyMist,
    onSecondaryContainer = DeepForest,
    tertiary = SunGold,
    onTertiary = DeepForest,
    background = CreamBackground,
    onBackground = DeepForest,
    surface = Color.White,
    onSurface = DeepForest,
    surfaceVariant = SkyMist,
    onSurfaceVariant = EarthBrown,
)

@Composable
fun TouchGrassIrlTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TouchGrassColorScheme,
        typography = Typography,
        content = content,
    )
}
