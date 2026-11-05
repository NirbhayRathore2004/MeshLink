package com.meshlink.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MeshDarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = MeshNavyDark,
    primaryContainer = MeshSurfaceElevated,
    onPrimaryContainer = CyanNeon,
    secondary = EmeraldGreen,
    onSecondary = MeshNavyDark,
    tertiary = PurpleNeon,
    background = MeshNavyDark,
    onBackground = TextPrimary,
    surface = MeshSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = MeshSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = MeshBorder
)

@Composable
fun MeshLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = MeshDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
