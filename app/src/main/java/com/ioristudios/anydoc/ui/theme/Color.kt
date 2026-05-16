package com.ioristudios.anydoc.ui.theme

import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0E1218)
val Surface = Color(0xFF111722)
val SurfaceContainerLowest = Color(0xFF131A27)
val SurfaceContainerLow = Color(0xFF182133)
val SurfaceContainer = Color(0xFF1E293B)
val SurfaceContainerHigh = Color(0xFF243246)
val SurfaceContainerHighest = Color(0xFF2A3A52)

val OnSurface = Color(0xFFE8EEF8)
val OnSurfaceVariant = Color(0xFFB8C4D8)

val Outline = Color(0xFF8BA0BF)
val OutlineVariant = Color(0xFF4B5F7D)

val Primary = Color(0xFF5EA8FF)
val PrimaryContainer = Color(0xFF2A7DE1)
val OnPrimaryContainer = Color(0xFFFFFFFF)

val SecondaryContainer = Color(0xFF3DDC97)
val TertiaryContainer = Color(0xFFFF9F43)

val NeonRed = Color(0xFFE85D75)
val NeonCyan = Color(0xFF5EC9FF)
val NeonGreen = Color(0xFF4FD8A4)
val NeonOrange = Color(0xFFFFA94D)
val NeonPurple = Color(0xFF7C8CFF)

// About Screen Specific Colors
val SurfaceDark = Color(0xFF0A0E14)
val SurfaceDarkElevated = Color(0xFF121721)
val SurfaceDarkCard = Color(0xFF161C27)
val TextPrimary = Color(0xFFE2E8F0)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val NeonPurpleAbout = Color(0xFF8B5CF6)
val NeonPurpleLight = Color(0xFFA78BFA)
val NeonPurpleSubtle = Color(0xFF2D2159)
val NeonPurpleGlow = Color(0xFF6366F1)
val SuccessGreenAbout = Color(0xFF10B981)

// Sidebar Custom Design Colors
val SurfaceDarkSheet = Color(0xFF0F141C)
val NeonPurpleFaint = Color(0xFF1E1E3F)



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
