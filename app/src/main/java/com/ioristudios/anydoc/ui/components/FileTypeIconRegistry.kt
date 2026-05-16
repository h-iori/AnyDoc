package com.ioristudios.anydoc.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ioristudios.anydoc.R
import com.ioristudios.anydoc.ui.theme.AppColors

data class FileVisualSpec(
    @DrawableRes val iconRes: Int,
    val iconTint: Color,
    val containerColor: Color,
    val borderColor: Color,
    val glowColor: Color,
    val accentColor: Color,
    val label: String
)

object FileTypeIconRegistry {
    fun resolveFileVisual(fileName: String, mimeType: String? = null): FileVisualSpec {
        val cleanName = fileName.trim().lowercase()
        val extension = cleanName.substringAfterLast('.', missingDelimiterValue = "")
        val mime = mimeType?.lowercase()?.trim().orEmpty()

        val normalized = when {
            cleanName.endsWith(".folder") || cleanName == "folder" -> "folder"
            extension.isNotBlank() -> extension
            "pdf" in mime -> "pdf"
            "word" in mime || "document" in mime -> "docx"
            "excel" in mime || "sheet" in mime || "csv" in mime -> "xlsx"
            "powerpoint" in mime || "presentation" in mime -> "pptx"
            "text" in mime -> "txt"
            "json" in mime || "xml" in mime || "javascript" in mime || "kotlin" in mime -> "code"
            else -> "file"
        }

        return when (normalized) {
            "pdf" -> neonSpec(
                iconRes = R.drawable.ic_pdf,
                accent = Color(0xFFFF3366),
                label = "PDF"
            )
            "doc", "docx", "rtf" -> neonSpec(
                iconRes = R.drawable.ic_word,
                accent = Color(0xFF36A3FF),
                label = "Word"
            )
            "xls", "xlsx", "csv" -> neonSpec(
                iconRes = R.drawable.ic_excel,
                accent = Color(0xFF00FF88),
                label = "Excel"
            )
            "ppt", "pptx" -> neonSpec(
                iconRes = R.drawable.ic_ppt,
                accent = Color(0xFFFFA133),
                label = "PPT"
            )
            "txt", "md" -> neonSpec(
                iconRes = R.drawable.ic_txt,
                accent = Color(0xFFB0B3C8),
                label = "Text"
            )
            "js", "kt", "java", "py", "ts", "tsx", "jsx", "c", "cpp", "h", "cs", "go", "rs", "swift", "php", "rb", "scala",
            "json", "xml", "yaml", "yml", "toml", "ini", "gradle" -> neonSpec(
                iconRes = R.drawable.ic_code,
                accent = Color(0xFFBF66FF),
                label = "Code"
            )
            "folder" -> neonSpec(
                iconRes = R.drawable.ic_folder,
                accent = Color(0xFF6366F1),
                label = "Folder"
            )
            else -> neonSpec(
                iconRes = R.drawable.ic_txt,
                accent = AppColors.BorderStrong,
                label = "File"
            )
        }
    }

    private fun neonSpec(
        @DrawableRes iconRes: Int,
        accent: Color,
        label: String
    ): FileVisualSpec {
        return FileVisualSpec(
            iconRes = iconRes,
            iconTint = Color.White,
            containerColor = accent.copy(alpha = 0.14f),
            borderColor = accent.copy(alpha = 0.58f),
            glowColor = accent.copy(alpha = 0.45f),
            accentColor = accent,
            label = label
        )
    }
}
