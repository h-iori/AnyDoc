package com.ioristudios.anydoc.util

import android.os.Environment
import com.ioristudios.anydoc.model.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FileManager {

    private val documentExtensions = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf",
        "md", "xml", "log", "html", "htm", "py", "kt", "java", "json", "csv", "cpp", "c", "h", "js", "css"
    )

    fun scanFiles(): List<FileItem> {
        val root = Environment.getExternalStorageDirectory()
        val foundFiles = mutableListOf<FileItem>()
        scanDirectory(root, foundFiles)
        return foundFiles.sortedByDescending { File(it.path).lastModified() }
    }

    private fun scanDirectory(dir: File, resultList: MutableList<FileItem>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                // Ignore hidden directories and common cache dirs to speed up scan
                if (!file.name.startsWith(".") && file.name != "Android") {
                    scanDirectory(file, resultList)
                }
            } else {
                val extension = file.extension.lowercase()
                if (documentExtensions.contains(extension)) {
                    resultList.add(
                        FileItem(
                            id = UUID.randomUUID().toString(),
                            name = file.name,
                            extension = extension,
                            size = formatFileSize(file.length()),
                            metadata = formatDate(file.lastModified()),
                            path = file.absolutePath
                        )
                    )
                }
            }
        }
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
