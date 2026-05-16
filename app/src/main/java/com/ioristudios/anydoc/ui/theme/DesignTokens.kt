package com.ioristudios.anydoc.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ioristudios.anydoc.ui.responsive.AppWindowClass
import com.ioristudios.anydoc.ui.responsive.rememberAppWindowClass

object AppColors {
    val Brand = Primary
    val BrandStrong = PrimaryContainer
    val SurfaceBase = SurfaceContainerLowest
    val Surface = SurfaceContainer
    val SurfaceElevated = SurfaceContainerHigh
    val SurfaceHighest = SurfaceContainerHighest
    val BorderSubtle = OutlineVariant
    val BorderStrong = Outline
    val Success = SecondaryContainer
    val Warning = NeonOrange
    val Danger = NeonRed
    val Info = NeonCyan
}

data class AppSpacing(
    val screenPadding: Dp,
    val sectionGap: Dp,
    val itemGap: Dp,
    val chipGap: Dp,
    val cardPadding: Dp
)

data class AppComponentSizes(
    val topBarHeight: Dp,
    val navBarHeight: Dp,
    val searchBarHeight: Dp,
    val fileCardMinHeight: Dp,
    val fileRowMinHeight: Dp,
    val fileIconContainer: Dp,
    val fileIcon: Dp
)

data class AppRadii(
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val pill: Dp
)

data class AppElevation(
    val level0: Dp,
    val level1: Dp,
    val level2: Dp,
    val level3: Dp
)

object AppMotion {
    const val Fast: Int = 120
    const val Normal: Int = 240
    const val Slow: Int = 420

    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val DecelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
}

fun adaptiveDp(windowClass: AppWindowClass, compact: Dp, medium: Dp, expanded: Dp): Dp {
    return when (windowClass) {
        AppWindowClass.Compact -> compact
        AppWindowClass.Medium -> medium
        AppWindowClass.Expanded -> expanded
    }
}

@Composable
fun rememberAppSpacing(): AppSpacing {
    val wc = rememberAppWindowClass()
    return AppSpacing(
        screenPadding = adaptiveDp(wc, 16.dp, 24.dp, 32.dp),
        sectionGap = adaptiveDp(wc, 20.dp, 24.dp, 28.dp),
        itemGap = adaptiveDp(wc, 10.dp, 12.dp, 14.dp),
        chipGap = adaptiveDp(wc, 8.dp, 10.dp, 12.dp),
        cardPadding = adaptiveDp(wc, 14.dp, 16.dp, 20.dp)
    )
}

@Composable
fun rememberAppSizes(): AppComponentSizes {
    val wc = rememberAppWindowClass()
    return AppComponentSizes(
        topBarHeight = adaptiveDp(wc, 64.dp, 72.dp, 80.dp),
        navBarHeight = adaptiveDp(wc, 72.dp, 76.dp, 80.dp),
        searchBarHeight = adaptiveDp(wc, 52.dp, 56.dp, 60.dp),
        fileCardMinHeight = adaptiveDp(wc, 100.dp, 120.dp, 136.dp),
        fileRowMinHeight = adaptiveDp(wc, 72.dp, 80.dp, 88.dp),
        fileIconContainer = adaptiveDp(wc, 38.dp, 42.dp, 46.dp),
        fileIcon = adaptiveDp(wc, 20.dp, 22.dp, 24.dp)
    )
}

fun rememberAppRadii(): AppRadii = AppRadii(
    xs = 6.dp,
    sm = 10.dp,
    md = 14.dp,
    lg = 18.dp,
    xl = 24.dp,
    pill = 999.dp
)

fun rememberAppElevation(): AppElevation = AppElevation(
    level0 = 0.dp,
    level1 = 2.dp,
    level2 = 6.dp,
    level3 = 12.dp
)

@Composable
fun scaledText(style: TextStyle, scale: Float): TextStyle {
    val density = LocalDensity.current
    return with(density) {
        style.copy(fontSize = style.fontSize * scale)
    }
}
