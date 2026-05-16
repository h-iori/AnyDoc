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
    background = Color(0xFFF4F7FC),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F4FA),
    surfaceContainer = Color(0xFFE8EFF9),
    surfaceContainerHigh = Color(0xFFDCE7F6),
    surfaceContainerHighest = Color(0xFFCCD9EE),
    onSurface = Color(0xFF1A2638),
    onSurfaceVariant = Color(0xFF455A78),
    outline = Color(0xFF667FA3),
    outlineVariant = Color(0xFFB0C0D7),
    primary = Color(0xFF236AC4),
    primaryContainer = Color(0xFF2D7DE0),
    onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF39B883),
    tertiaryContainer = Color(0xFFE8922D)
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
    radius: Dp = 8.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) = this.shadow(
    elevation = radius,
    shape = shape,
    ambientColor = color.copy(alpha = 0.28f),
    spotColor = color.copy(alpha = 0.32f),
    clip = false
)
