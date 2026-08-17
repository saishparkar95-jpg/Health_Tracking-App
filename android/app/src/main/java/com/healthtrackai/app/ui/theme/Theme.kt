package com.healthtrackai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.healthtrackai.app.data.models.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = BackgroundDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = CyanAccent,
    onSecondary = BackgroundDark,
    secondaryContainer = CyanDark,
    onSecondaryContainer = CyanLight,
    tertiary = PurpleAccent,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSubtleDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfaceLight,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = EmeraldDark,
    secondary = CyanAccent,
    onSecondary = SurfaceLight,
    secondaryContainer = CyanLight,
    onSecondaryContainer = CyanDark,
    tertiary = PurpleAccent,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderSubtleLight
)

@Composable
fun HealthTrackAITheme(
    themeMode: AppThemeMode,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    HealthTrackAIThemeInternal(isDark, dynamicColor, content)
}

@Composable
fun HealthTrackAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    HealthTrackAIThemeInternal(darkTheme, dynamicColor, content)
}

private fun android.content.Context.findActivity(): Activity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
private fun HealthTrackAIThemeInternal(
    isDark: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            if (activity != null) {
                try {
                    val window = activity.window
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                } catch (e: Throwable) {
                    // Gracefully ignore window inset exceptions
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
