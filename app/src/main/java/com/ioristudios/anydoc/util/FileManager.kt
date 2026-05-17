package com.ioristudios.anydoc.util

import android.os.Environment
import com.ioristudios.anydoc.model.BrowseItem
import com.ioristudios.anydoc.model.FileItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FileManager {

    val documentExtensions = setOf(
        // Documents
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv",
        // Code & markup
        "md", "xml", "log", "html", "htm", "py", "kt", "java", "json",
        "cpp", "c", "h", "js", "css", "ts", "tsx", "jsx", "cs", "go",
        "rs", "swift", "php", "rb", "scala", "yaml", "yml", "toml", "ini", "gradle",
        // Additional
        "sql", "sh", "bat", "ps1", "r", "lua", "dart", "vue", "svelte", "env",
        "cfg", "conf", "properties", "makefile"
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

    /**
     * Lists the immediate children of [directoryPath], returning only:
     * - Non-hidden folders (excluding "Android" system folder)
     * - Files whose extension is in [documentExtensions]
     *
     * Folders are sorted first (alphabetically), then files (alphabetically).
     */
    fun listDirectory(directoryPath: String): List<BrowseItem> {
        val dir = File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val children = dir.listFiles() ?: return emptyList()
        val items = mutableListOf<BrowseItem>()

        for (child in children) {
            if (child.name.startsWith(".")) continue // skip hidden

            if (child.isDirectory) {
                if (child.name == "Android") continue // skip system dir
                val supportedCount = countSupportedItems(child)
                items.add(
                    BrowseItem(
                        name = child.name,
                        path = child.absolutePath,
                        isDirectory = true,
                        extension = "",
                        size = "$supportedCount items",
                        lastModified = formatDate(child.lastModified()),
                        childCount = supportedCount
                    )
                )
            } else {
                val ext = child.extension.lowercase()
                if (documentExtensions.contains(ext)) {
                    items.add(
                        BrowseItem(
                            name = child.name,
                            path = child.absolutePath,
                            isDirectory = false,
                            extension = ext,
                            size = formatFileSize(child.length()),
                            lastModified = formatDate(child.lastModified())
                        )
                    )
                }
            }
        }

        // Folders first (alphabetical), then files (alphabetical)
        return items.sortedWith(
            compareByDescending<BrowseItem> { it.isDirectory }
                .thenBy { it.name.lowercase() }
        )
    }

    /**
     * Counts how many supported items (folders + matching files) exist
     * as immediate children of [dir].
     */
    private fun countSupportedItems(dir: File): Int {
        val children = dir.listFiles() ?: return 0
        return children.count { child ->
            if (child.name.startsWith(".")) false
            else if (child.isDirectory) child.name != "Android"
            else documentExtensions.contains(child.extension.lowercase())
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

