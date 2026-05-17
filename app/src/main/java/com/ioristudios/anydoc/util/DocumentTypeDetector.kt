package com.ioristudios.anydoc.util

import android.webkit.MimeTypeMap
import com.ioristudios.anydoc.model.DocumentKind
import com.ioristudios.anydoc.model.DocumentOpenRequest
import java.io.File

object DocumentTypeDetector {
    private val editableTextExtensions = setOf(
        "txt", "md", "xml", "log", "html", "htm", "py", "kt", "java", "json",
        "cpp", "c", "h", "js", "css", "ts", "tsx", "jsx", "cs", "go",
        "rs", "swift", "php", "rb", "scala", "yaml", "yml", "toml", "ini",
        "gradle", "sql", "sh", "bat", "ps1", "r", "lua", "dart", "vue",
        "svelte", "env", "cfg", "conf", "properties", "makefile"
    )

    fun detect(path: String): DocumentOpenRequest {
        val file = File(path)
        val extension = file.extension.lowercase()
        val mimeType = detectMime(file, extension)
        val kind = when (extension) {
            "pdf" -> DocumentKind.Pdf
            "ppt", "pptx" -> DocumentKind.Presentation
            "doc", "docx", "rtf" -> DocumentKind.Word
            "xls", "xlsx" -> DocumentKind.Spreadsheet
            "csv" -> DocumentKind.Csv
            in editableTextExtensions -> DocumentKind.Text
            else -> DocumentKind.Unsupported
        }
        val canEdit = when (extension) {
            "pdf", "ppt", "pptx", "doc", "xls", "rtf" -> false
            "docx", "xlsx", "csv" -> true
            in editableTextExtensions -> true
            else -> false
        }

        return DocumentOpenRequest(
            path = file.absolutePath,
            displayName = file.name.ifBlank { "Document" },
            extension = extension,
            mimeType = mimeType,
            sizeBytes = file.length(),
            lastModified = file.lastModified(),
            kind = kind,
            canEdit = canEdit
        )
    }

    private fun detectMime(file: File, extension: String): String {
        runCatching {
            val tikaClass = Class.forName("org.apache.tika.Tika")
            val tika = tikaClass.getDeclaredConstructor().newInstance()
            val detect = tikaClass.getMethod("detect", File::class.java)
            return detect.invoke(tika, file) as String
        }

        return MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
            ?: when (extension) {
                "md" -> "text/markdown"
                "csv" -> "text/csv"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                else -> "application/octet-stream"
            }
    }
}
