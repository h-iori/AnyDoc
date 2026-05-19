package com.ioristudios.anydoc.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.anydoc.model.DocumentContent
import com.ioristudios.anydoc.model.DocumentKind
import com.ioristudios.anydoc.model.DocumentViewerState
import com.ioristudios.anydoc.model.SearchMatch
import com.ioristudios.anydoc.util.DocumentFileIo
import com.ioristudios.anydoc.util.DocumentTypeDetector
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val _uiState = MutableStateFlow<DocumentViewerState>(DocumentViewerState.Loading)
    val uiState: StateFlow<DocumentViewerState> = _uiState.asStateFlow()

    fun open(pathOrUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = DocumentViewerState.Loading
            
            var targetPath = pathOrUri
            var originalUriString: String? = null
            var displayName: String? = null
            
            if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                val uri = Uri.parse(pathOrUri)
                if (uri.scheme == "file") {
                    targetPath = uri.path ?: pathOrUri
                } else if (uri.scheme == "content") {
                    originalUriString = pathOrUri
                    
                    val resolvedPath = resolveExternalStorageUri(uri)
                    if (resolvedPath != null && File(resolvedPath).exists()) {
                        targetPath = resolvedPath
                    } else {
                        var name = "temp_document"
                        runCatching {
                            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1 && cursor.moveToFirst()) {
                                    name = cursor.getString(nameIndex)
                                }
                            }
                        }
                        displayName = name
                        
                        val tempFile = File(context.cacheDir, name)
                        val copyResult = runCatching {
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                tempFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                        if (copyResult.isFailure) {
                            _uiState.value = DocumentViewerState.Error(
                                name,
                                "Failed to read shared document: ${copyResult.exceptionOrNull()?.localizedMessage}"
                            )
                            return@launch
                        }
                        targetPath = tempFile.absolutePath
                    }
                }
            }

            val file = File(targetPath)
            if (!file.exists()) {
                _uiState.value = DocumentViewerState.Error(
                    displayName ?: file.name.ifBlank { "Document" },
                    "File does not exist."
                )
                return@launch
            }

            runCatching {
                val request = DocumentTypeDetector.detect(targetPath, originalUriString)
                val content = when (request.kind) {
                    DocumentKind.Pdf -> DocumentContent.PdfContent(request.path)
                    DocumentKind.Text -> DocumentContent.TextContent(
                        text = DocumentFileIo.readText(request.path),
                        isCodeLike = request.extension != "txt" && request.extension != "log"
                    )
                    DocumentKind.Csv -> DocumentContent.CsvContent(DocumentFileIo.readCsv(request.path))
                    DocumentKind.Word -> when (request.extension) {
                        "docx" -> DocumentContent.OfficeTextContent(DocumentFileIo.readDocxText(request.path))
                        else -> DocumentContent.UnsupportedContent("${request.extension.uppercase()} can be viewed only through extracted metadata in this version.")
                    }
                    DocumentKind.Spreadsheet -> when (request.extension) {
                        "xlsx" -> DocumentContent.CsvContent(DocumentFileIo.readXlsxRows(request.path))
                        else -> DocumentContent.UnsupportedContent("Legacy XLS editing is not supported. Use XLSX for offline editing.")
                    }
                    DocumentKind.Presentation -> when (request.extension) {
                        "pptx" -> DocumentContent.OfficeTextContent(DocumentFileIo.readPptxText(request.path))
                        else -> DocumentContent.UnsupportedContent("Legacy PPT slide rendering is not available in this build.")
                    }
                    DocumentKind.Unsupported -> DocumentContent.UnsupportedContent("This file type is not supported by AnyDoc yet.")
                }
                DocumentViewerState.Ready(
                    request = request,
                    content = content,
                    editedText = initialEditableText(content),
                    editedRows = (content as? DocumentContent.CsvContent)?.rows.orEmpty()
                )
            }.onSuccess { ready ->
                _uiState.value = ready
            }.onFailure { error ->
                _uiState.value = DocumentViewerState.Error(displayName ?: file.name, error.localizedMessage ?: "Could not open document.")
            }
        }
    }

    fun enterEditMode() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = if (current.request.canEdit) {
            current.copy(isEditing = true, message = null)
        } else {
            current.copy(message = "${current.request.extension.uppercase()} is read-only in AnyDoc.")
        }
    }

    fun exitEditMode() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = current.copy(isEditing = false, message = null)
    }

    fun updateEditedText(text: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = current.copy(editedText = text, message = null)
    }

    fun updateCell(rowIndex: Int, columnIndex: Int, value: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val source = current.editedRows.ifEmpty {
            (current.content as? DocumentContent.CsvContent)?.rows.orEmpty()
        }.ifEmpty {
            listOf(listOf(""))
        }
        val mutableRows = source.map { it.toMutableList() }.toMutableList()
        while (mutableRows.size <= rowIndex) mutableRows += mutableListOf("")
        while (mutableRows[rowIndex].size <= columnIndex) mutableRows[rowIndex] += ""
        mutableRows[rowIndex][columnIndex] = value
        val rows = mutableRows.map { it.toList() }
        _uiState.value = current.copy(editedRows = rows, message = null)
    }

    fun addRow() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (!current.isEditing) return
        val source = current.editedRows.ifEmpty {
            (current.content as? DocumentContent.CsvContent)?.rows.orEmpty()
        }
        val width = source.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1
        _uiState.value = current.copy(editedRows = source + listOf(List(width) { "" }))
    }

    fun addColumn() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (!current.isEditing) return
        val source = current.editedRows.ifEmpty {
            (current.content as? DocumentContent.CsvContent)?.rows.orEmpty()
        }.ifEmpty {
            listOf(emptyList())
        }
        _uiState.value = current.copy(editedRows = source.map { it + "" })
    }

    fun save() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (!current.request.canEdit) {
            _uiState.value = current.copy(message = "${current.request.extension.uppercase()} is read-only in AnyDoc.")
            return
        }

        _uiState.value = current.copy(isSaving = true, message = null)
        viewModelScope.launch(Dispatchers.IO) {
            val localPath = current.request.path
            val result = runCatching {
                when (current.request.extension) {
                    "csv" -> DocumentFileIo.writeCsv(localPath, current.editedRows)
                    "xlsx" -> DocumentFileIo.writeXlsxRows(localPath, current.editedRows)
                    "docx" -> DocumentFileIo.writeDocxText(localPath, current.editedText)
                    else -> DocumentFileIo.writeText(localPath, current.editedText)
                }

                val originalUri = current.request.originalUri
                if (!originalUri.isNullOrBlank() && localPath.startsWith(context.cacheDir.absolutePath)) {
                    val uri = Uri.parse(originalUri)
                    context.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                        File(localPath).inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }

            result.onSuccess {
                val savedContent = when (current.request.extension) {
                    "csv", "xlsx" -> DocumentContent.CsvContent(current.editedRows)
                    "docx" -> DocumentContent.OfficeTextContent(
                        current.editedText.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
                    )
                    else -> DocumentContent.TextContent(
                        text = current.editedText,
                        isCodeLike = (current.content as? DocumentContent.TextContent)?.isCodeLike == true
                    )
                }
                _uiState.value = current.copy(
                    content = savedContent,
                    isSaving = false,
                    isEditing = false,
                    message = "Saved changes."
                )
            }.onFailure { error ->
                _uiState.value = current.copy(
                    isSaving = false,
                    isEditing = true,
                    message = error.localizedMessage ?: "Save failed."
                )
            }
        }
    }

    fun updateSearch(query: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val matches = if (query.isBlank()) emptyList() else findMatches(searchableText(current), query)
        _uiState.value = current.copy(
            searchQuery = query,
            searchMatches = matches,
            activeMatch = if (matches.isEmpty()) -1 else 0
        )
    }

    fun nextMatch() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (current.searchMatches.isEmpty()) return
        _uiState.value = current.copy(activeMatch = (current.activeMatch + 1) % current.searchMatches.size)
    }

    fun prevMatch() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (current.searchMatches.isEmpty()) return
        val newIndex = if (current.activeMatch <= 0) current.searchMatches.size - 1 else current.activeMatch - 1
        _uiState.value = current.copy(activeMatch = newIndex)
    }

    fun clearMessage() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = current.copy(message = null)
    }

    private fun initialEditableText(content: DocumentContent): String = when (content) {
        is DocumentContent.TextContent -> content.text
        is DocumentContent.OfficeTextContent -> content.sections.joinToString("\n\n")
        is DocumentContent.CsvContent -> DocumentFileIo.flattenRows(content.rows)
        else -> ""
    }

    private fun searchableText(state: DocumentViewerState.Ready): String = when (val content = state.content) {
        is DocumentContent.TextContent -> if (state.isEditing) state.editedText else content.text
        is DocumentContent.OfficeTextContent -> if (state.isEditing) state.editedText else content.sections.joinToString("\n")
        is DocumentContent.CsvContent -> DocumentFileIo.flattenRows(if (state.isEditing) state.editedRows else content.rows)
        is DocumentContent.PdfContent -> state.request.displayName
        is DocumentContent.UnsupportedContent -> content.message
    }

    private fun findMatches(text: String, query: String): List<SearchMatch> {
        val matches = mutableListOf<SearchMatch>()
        var start = 0
        while (true) {
            val index = text.indexOf(query, startIndex = start, ignoreCase = true)
            if (index < 0) break
            val previewStart = (index - 36).coerceAtLeast(0)
            val previewEnd = (index + query.length + 36).coerceAtMost(text.length)
            matches += SearchMatch(index, text.substring(previewStart, previewEnd).replace('\n', ' '))
            start = index + query.length
        }
        return matches
    }

    private fun resolveExternalStorageUri(uri: Uri): String? {
        if (uri.authority != "com.android.externalstorage.documents") return null
        val path = uri.path ?: return null
        
        val docIndex = path.indexOf("/document/")
        val documentId = if (docIndex != -1) {
            path.substring(docIndex + "/document/".length)
        } else {
            val treeIndex = path.indexOf("/tree/")
            if (treeIndex != -1) {
                path.substring(treeIndex + "/tree/".length)
            } else {
                return null
            }
        }
        
        val decodedId = Uri.decode(documentId)
        val parts = decodedId.split(":", limit = 2)
        if (parts.size == 2) {
            val type = parts[0]
            val relativePath = parts[1]
            val baseDir = if ("primary".equals(type, ignoreCase = true)) {
                "/storage/emulated/0"
            } else {
                "/storage/$type"
            }
            val file = File(baseDir, relativePath)
            if (file.exists()) {
                return file.absolutePath
            }
        }
        return null
    }
}
