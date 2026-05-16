package com.ioristudios.anydoc.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.ioristudios.anydoc.ui.theme.AppColors

data class FileVisualSpec(
    val icon: ImageVector,
    val tint: Color,
    val label: String
)

object FileTypeIconRegistry {
    fun resolveFileVisual(fileName: String, mimeType: String? = null): FileVisualSpec {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = fileName)
            .lowercase()
            .trim()

        val normalized = when {
            extension.isNotBlank() && extension != fileName.lowercase() -> extension
            mimeType?.contains("pdf") == true -> "pdf"
            mimeType?.contains("word") == true -> "docx"
            mimeType?.contains("sheet") == true -> "xlsx"
            mimeType?.contains("presentation") == true -> "pptx"
            mimeType?.contains("image") == true -> "png"
            else -> extension
        }

        return when (normalized) {
            "pdf" -> FileVisualSpec(Icons.Default.PictureAsPdf, AppColors.Danger, "PDF")
            "doc", "docx", "rtf" -> FileVisualSpec(Icons.Default.Description, AppColors.Info, "Word")
            "xls", "xlsx", "csv" -> FileVisualSpec(Icons.Default.TableChart, AppColors.Success, "Spreadsheet")
            "ppt", "pptx" -> FileVisualSpec(Icons.Default.Slideshow, AppColors.Warning, "Presentation")
            "txt", "md" -> FileVisualSpec(Icons.Default.TextSnippet, AppColors.BorderStrong, "Text")
            "json" -> FileVisualSpec(Icons.Default.DataObject, AppColors.Info, "JSON")
            "xml" -> FileVisualSpec(Icons.Default.Dataset, AppColors.Info, "XML")
            "html" -> FileVisualSpec(Icons.Default.Language, AppColors.Brand, "HTML")
            "js", "kt", "java" -> FileVisualSpec(Icons.Default.Code, AppColors.BrandStrong, "Code")
            "png", "jpg", "jpeg", "webp" -> FileVisualSpec(Icons.Default.Image, AppColors.Info, "Image")
            "zip", "rar", "7z" -> FileVisualSpec(Icons.Default.FolderZip, AppColors.Warning, "Archive")
            "folder" -> FileVisualSpec(Icons.Default.Folder, AppColors.BorderStrong, "Folder")
            else -> FileVisualSpec(Icons.AutoMirrored.Filled.InsertDriveFile, AppColors.BorderStrong, "File")
        }
    }
}
