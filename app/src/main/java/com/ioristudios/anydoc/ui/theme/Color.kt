package com.ioristudios.anydoc.ui.theme

import androidx.compose.ui.graphics.Color

val SurfaceDark = Color(0xFF0A0A0F)
val SurfaceDarkElevated = Color(0xFF12121A)
val SurfaceDarkCard = Color(0xFF161622)

val NeonPurple = Color(0xFFBF00FF)
val NeonPurpleGlow = Color(0xFF9B30FF)
val NeonPurpleLight = Color(0xFFD455FF)

val CoreWhite = Color(0xFFFFFFFF)
val CoreWhiteDim = Color(0xFFEEEEFF)
val SecondaryText = Color(0xFF7A7A8E)
val MutedText = Color(0xFF4A4A5E)

val NeonRed = Color(0xFFFF3366)
val NeonGreen = Color(0xFF00FF88)
val NeonCyan = Color(0xFF36D7FF)
val NeonOrange = Color(0xFFFF9A3C)

val Background = SurfaceDark
val Surface = SurfaceDarkElevated
val SurfaceContainerLowest = SurfaceDark
val SurfaceContainerLow = SurfaceDarkElevated
val SurfaceContainer = SurfaceDarkCard
val SurfaceContainerHigh = Color(0xFF1B1B2A)
val SurfaceContainerHighest = Color(0xFF212132)

val OnSurface = CoreWhiteDim
val OnSurfaceVariant = SecondaryText
val Outline = NeonPurpleGlow.copy(alpha = 0.7f)
val OutlineVariant = MutedText

val Primary = NeonPurple
val PrimaryContainer = NeonPurpleGlow
val OnPrimaryContainer = CoreWhite

val SecondaryContainer = NeonGreen
val TertiaryContainer = NeonOrange

// Existing usages in other screens keep compiling through aliases.
val TextPrimary = CoreWhiteDim
val TextSecondary = SecondaryText
val TextMuted = MutedText
val NeonPurpleAbout = NeonPurple
val NeonPurpleSubtle = Color(0xFF251035)
val SuccessGreenAbout = NeonGreen
val SurfaceDarkSheet = SurfaceDarkElevated
val NeonPurpleFaint = Color(0xFF2D1940)



fun getAccentForExtension(extension: String): Color {
    return when (extension.lowercase()) {
        "pdf" -> NeonRed
        "doc", "docx", "rtf" -> NeonCyan
        "xls", "xlsx", "csv" -> NeonGreen
        "ppt", "pptx" -> NeonOrange
        "js", "kt", "java", "html", "css", "json", "xml" -> NeonPurple
        "png", "jpg", "jpeg", "webp" -> NeonCyan
        else -> Outline
    }
}
