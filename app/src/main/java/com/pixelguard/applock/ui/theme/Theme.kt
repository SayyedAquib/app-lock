package com.pixelguard.applock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PixelPrimaryDark,
    onPrimary = PixelOnPrimaryDark,
    primaryContainer = PixelPrimaryContainerDark,
    onPrimaryContainer = PixelOnPrimaryContainerDark,
    secondary = PixelSecondaryDark,
    onSecondary = PixelOnSecondaryDark,
    secondaryContainer = PixelSecondaryContainerDark,
    onSecondaryContainer = PixelOnSecondaryContainerDark,
    background = PixelBackgroundDark,
    surface = PixelSurfaceDark,
    surfaceVariant = PixelSurfaceVariantDark,
    onSurface = PixelOnSurfaceDark,
    onSurfaceVariant = PixelOnSurfaceVariantDark,
    outline = PixelOutlineDark,
    outlineVariant = PixelOutlineVariantDark,
    surfaceContainerLowest = PixelSurfaceContainerLowest,
    surfaceContainerLow = PixelSurfaceContainerLow,
    surfaceContainer = PixelSurfaceContainer,
    surfaceContainerHigh = PixelSurfaceContainerHigh,
    surfaceContainerHighest = PixelSurfaceContainerHighest
)

private val LightColorScheme = lightColorScheme(
    primary = PixelPrimaryLight,
    onPrimary = PixelOnPrimaryLight,
    primaryContainer = PixelPrimaryContainerLight,
    onPrimaryContainer = PixelOnPrimaryContainerLight,
    secondary = PixelSecondaryLight,
    onSecondary = PixelOnSecondaryLight,
    secondaryContainer = PixelSecondaryContainerLight,
    onSecondaryContainer = PixelOnSecondaryContainerLight,
    background = PixelBackgroundLight,
    surface = PixelSurfaceLight,
    surfaceVariant = PixelSurfaceVariantLight,
    onSurface = PixelOnSurfaceLight,
    onSurfaceVariant = PixelOnSurfaceVariantLight,
    outline = PixelOutlineLight,
    outlineVariant = PixelOutlineVariantLight,
    surfaceContainerLowest = PixelSurfaceContainerLowestLight,
    surfaceContainerLow = PixelSurfaceContainerLowLight,
    surfaceContainer = PixelSurfaceContainerLight,
    surfaceContainerHigh = PixelSurfaceContainerHighLight,
    surfaceContainerHighest = PixelSurfaceContainerHighestLight
)

@Composable
fun PixelAppLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
