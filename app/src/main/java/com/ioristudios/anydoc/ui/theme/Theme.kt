package com.ioristudios.anydoc.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant,
    primary = Primary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondaryContainer = SecondaryContainer,
    tertiaryContainer = TertiaryContainer
)

private val LightColorScheme = lightColorScheme(
    background = SurfaceDark,
    surface = SurfaceDarkElevated,
    surfaceContainerLowest = SurfaceDark,
    surfaceContainerLow = SurfaceDarkElevated,
    surfaceContainer = SurfaceDarkCard,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurface = CoreWhiteDim,
    onSurfaceVariant = SecondaryText,
    outline = Outline,
    outlineVariant = OutlineVariant,
    primary = NeonPurple,
    primaryContainer = NeonPurpleGlow,
    onPrimaryContainer = CoreWhite,
    secondaryContainer = NeonGreen,
    tertiaryContainer = NeonOrange
)

@Composable
fun AnyDocTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

fun Modifier.neonGlow(
    color: Color,
    radius: Dp = AppGlow.Md,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp)
) = this.shadow(
    elevation = radius,
    shape = shape,
    ambientColor = color.copy(alpha = AppGlow.SubtleAlpha),
    spotColor = color.copy(alpha = AppGlow.StrongAlpha),
    clip = false
)
