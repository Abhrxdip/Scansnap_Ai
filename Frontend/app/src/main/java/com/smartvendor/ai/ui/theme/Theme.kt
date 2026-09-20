package com.smartvendor.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NeuBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EEFF),
    onPrimaryContainer = NeuBlack,
    secondary = NeuYellow,
    onSecondary = NeuBlack,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = NeuBlack,
    tertiary = NeuPurple,
    onTertiary = NeuBlack,
    background = NeuBackground,
    onBackground = NeuBlack,
    surface = NeuSurface,
    onSurface = NeuBlack,
    surfaceVariant = Color(0xFFEDE8DE),
    onSurfaceVariant = NeuGray,
    error = NeuRed,
    onError = Color.White,
    outline = NeuBlack
)

private val DarkColorScheme = darkColorScheme(
    primary = NeuBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color.White,
    secondary = NeuYellow,
    onSecondary = NeuBlack,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,
    error = NeuRed,
    onError = Color.White,
    outline = Color(0xFF4B5563)
)

@Composable
fun SmartVendorAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
