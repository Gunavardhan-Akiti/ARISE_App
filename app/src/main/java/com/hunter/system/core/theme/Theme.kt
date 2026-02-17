package com.hunter.system.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import androidx.core.view.WindowCompat

/**
 * Solo Leveling SYSTEM Theme
 *
 * Always dark — the System aesthetic demands it.
 * Uses custom dark color scheme with cyan-blue glow palette.
 * Edge-to-edge is enforced by Android 16 by default.
 */

private val SystemDarkColorScheme = darkColorScheme(
    primary = SystemPrimary,
    onPrimary = SystemBackground,
    primaryContainer = SystemPrimaryDark,
    onPrimaryContainer = SystemTextPrimary,
    secondary = SystemAccent,
    onSecondary = SystemBackground,
    secondaryContainer = SystemSurfaceVariant,
    onSecondaryContainer = SystemTextPrimary,
    tertiary = SystemSuccess,
    onTertiary = SystemBackground,
    error = SystemError,
    onError = SystemTextPrimary,
    background = SystemBackground,
    onBackground = SystemTextPrimary,
    surface = SystemSurface,
    onSurface = SystemTextPrimary,
    surfaceVariant = SystemSurfaceVariant,
    onSurfaceVariant = SystemTextSecondary,
    outline = SystemBorder,
    outlineVariant = SystemBorderLight,
    inverseSurface = SystemTextPrimary,
    inverseOnSurface = SystemBackground,
    inversePrimary = SystemPrimaryDark,
    surfaceTint = SystemGlow,
    scrim = Color(0xCC000000)
)

@Composable
fun SoloLevelingSystemTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = SystemDarkColorScheme

    // Edge-to-edge is enforced on Android 16, but we still configure
    // the status/nav bar appearance for proper contrast
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as Activity
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            val controller = WindowCompat.getInsetsController(activity.window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SystemTypography,
        content = content
    )
}
