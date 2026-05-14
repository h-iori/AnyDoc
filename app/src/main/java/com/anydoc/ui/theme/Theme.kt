package com.anydoc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val AnyDocColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = CoreWhite,
    primaryContainer = NeonPurpleGlow.copy(alpha = 0.24f),
    onPrimaryContainer = CoreWhite,
    secondary = NeonPurpleLight,
    onSecondary = CoreWhite,
    tertiary = SuccessNeon,
    onTertiary = SurfaceDark,
    background = SurfaceDark,
    onBackground = CoreWhite,
    surface = ElevatedSurface,
    onSurface = CoreWhite,
    surfaceVariant = CardSurface,
    onSurfaceVariant = CoreWhiteDim,
    surfaceTint = NeonPurple,
    error = ErrorNeon,
    onError = CoreWhite,
    outline = SecondaryText,
    outlineVariant = MutedDisabled,
    scrim = Color.Black.copy(alpha = 0.7f)
)

@Composable
fun AnyDocTheme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()

    SideEffect {
        systemUiController.setStatusBarColor(color = Color.Transparent, darkIcons = false)
        systemUiController.setNavigationBarColor(color = SurfaceDark, darkIcons = false)
    }

    MaterialTheme(
        colorScheme = AnyDocColorScheme,
        typography = AnyDocTypography,
        shapes = AnyDocShapes,
        content = content
    )
}
