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
