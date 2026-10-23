package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    onPrimaryContainer = BlueDark,
    secondary = Navy800,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Navy900,
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    tertiaryContainer = SuccessGreenBg,
    onTertiaryContainer = SuccessGreen,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRed,
    background = Color.White,
    onBackground = Navy900,
    surface = Color.White,
    onSurface = Navy900,
    surfaceVariant = Slate50,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate100
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueLight,
    onPrimary = Color.White,
    primaryContainer = Navy800,
    onPrimaryContainer = Blue50,
    secondary = Slate200,
    onSecondary = Navy900,
    secondaryContainer = Navy700,
    onSecondaryContainer = Slate50,
    tertiary = SuccessGreenLight,
    onTertiary = Navy900,
    tertiaryContainer = Navy800,
    onTertiaryContainer = SuccessGreenLight,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Navy800,
    onErrorContainer = ErrorRed,
    background = Navy900,
    onBackground = Slate50,
    surface = Navy800,
    onSurface = Slate50,
    surfaceVariant = Navy700,
    onSurfaceVariant = Slate400,
    outline = Navy700,
    outlineVariant = Navy800
)

@Composable
fun MFMPanelTheme(
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

// Retain alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MFMPanelTheme(darkTheme = darkTheme, content = content)
}
