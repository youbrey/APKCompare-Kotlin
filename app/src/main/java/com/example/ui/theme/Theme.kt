package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ProfessionalPolishColorScheme = lightColorScheme(
    primary = ProBlue600,
    onPrimary = ProWhiteSurface,
    primaryContainer = ProBlue100,
    onPrimaryContainer = ProBlue700,
    secondary = ProEmerald,
    onSecondary = ProWhiteSurface,
    tertiary = ProAmber,
    background = ProLightBackground,
    onBackground = ProSlate900,
    surface = ProWhiteSurface,
    onSurface = ProSlate900,
    surfaceVariant = ProWhiteSurface,
    onSurfaceVariant = ProSlate700,
    outline = ProBorder,
    error = ProRose
)

@Composable
fun APKComparatorTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = ProfessionalPolishColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ProWhiteSurface.toArgb()
            window.navigationBarColor = ProLightBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

