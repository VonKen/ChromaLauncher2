package com.chromalauncher.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Violet,
    onPrimary = OnSurfaceDark,
    primaryContainer = VioletDark,
    secondary = Cyan,
    onSecondary = OnSurfaceDark,
    secondaryContainer = CyanDark,
    tertiary = Magenta,
    onTertiary = OnSurfaceDark,
    tertiaryContainer = MagentaDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = ErrorRed,
    outline = OnSurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = VioletDark,
    onPrimary = OnSurfaceLight,
    primaryContainer = VioletLight,
    secondary = CyanDark,
    onSecondary = OnSurfaceLight,
    secondaryContainer = CyanLight,
    tertiary = MagentaDark,
    onTertiary = OnSurfaceLight,
    tertiaryContainer = MagentaLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    outline = OnSurfaceVariantLight,
)

@Composable
fun ChromaTheme(
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
