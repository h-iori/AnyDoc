package com.ioristudios.anydoc.ui.theme

import androidx.compose.ui.graphics.Color

// ── Surface Scale ──
val SurfaceDark = Color(0xFF0A0A0F)
val SurfaceDarkElevated = Color(0xFF12121A)
val SurfaceDarkCard = Color(0xFF161622)

// ── Enterprise Neon Primary ──
val NeonPurple = Color(0xFFA855F7)        // Purple-500 — refined violet
val NeonPurpleGlow = Color(0xFF7C3AED)    // Violet-600 — deeper glow layer
val NeonPurpleLight = Color(0xFFC084FC)   // Purple-400 — luminous highlight

// ── Text Scale ──
val CoreWhite = Color(0xFFFFFFFF)
val CoreWhiteDim = Color(0xFFEEEEFF)
val SecondaryText = Color(0xFF7A7A8E)
val MutedText = Color(0xFF4A4A5E)

// ── Enterprise Neon Accents ──
val NeonRed = Color(0xFFF43F5E)           // Rose-500 — vibrant but not aggressive
val NeonGreen = Color(0xFF34D399)         // Emerald-400 — de-saturated from raw lime
val NeonCyan = Color(0xFF22D3EE)          // Cyan-400 — tuned for dark backgrounds
val NeonOrange = Color(0xFFFB923C)        // Orange-400 — warm, not candy-like
val NeonIndigo = Color(0xFF818CF8)        // Indigo-400 — secondary accent for folders

// ── Material Surface Mapping ──
val Background = SurfaceDark
val Surface = SurfaceDarkElevated
val SurfaceContainerLowest = SurfaceDark
val SurfaceContainerLow = SurfaceDarkElevated
val SurfaceContainer = SurfaceDarkCard
val SurfaceContainerHigh = Color(0xFF1B1B2A)
val SurfaceContainerHighest = Color(0xFF212132)

val OnSurface = CoreWhiteDim
val OnSurfaceVariant = SecondaryText
val Outline = NeonPurpleGlow.copy(alpha = 0.6f)
val OutlineVariant = MutedText

val Primary = NeonPurple
val PrimaryContainer = NeonPurpleGlow
val OnPrimaryContainer = CoreWhite

val SecondaryContainer = NeonGreen
val TertiaryContainer = NeonOrange

// Aliases for backward-compatibility across screens.
val TextPrimary = CoreWhiteDim
val TextSecondary = SecondaryText
val TextMuted = MutedText
val NeonPurpleAbout = NeonPurple
val NeonPurpleSubtle = Color(0xFF1A1030)
val SuccessGreenAbout = NeonGreen
val SurfaceDarkSheet = SurfaceDarkElevated
val NeonPurpleFaint = Color(0xFF1E1534)


fun getAccentForExtension(extension: String): Color {
    return when (extension.lowercase()) {
        "pdf" -> NeonRed
        "doc", "docx", "rtf" -> NeonCyan
        "xls", "xlsx", "csv" -> NeonGreen
        "ppt", "pptx" -> NeonOrange
        "js", "kt", "java", "html", "css", "json", "xml", "py", "ts", "tsx",
        "jsx", "c", "cpp", "h", "cs", "go", "rs", "swift", "php", "rb",
        "scala", "yaml", "yml", "toml", "ini", "gradle", "log", "md" -> NeonPurple
        "png", "jpg", "jpeg", "webp" -> NeonCyan
        else -> Outline
    }
}
