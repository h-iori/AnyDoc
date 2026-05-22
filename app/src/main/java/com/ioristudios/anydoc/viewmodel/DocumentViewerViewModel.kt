package com.ioristudios.anydoc.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ioristudios.anydoc.model.*
import com.ioristudios.anydoc.util.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val _uiState = MutableStateFlow<DocumentViewerState>(DocumentViewerState.Loading)
    private var pdfPageDataList: List<PdfTextPositionExtractor.Companion.PageTextData> = emptyList()
    val uiState: StateFlow<DocumentViewerState> = _uiState.asStateFlow()

    init {
        // Initialize PDFBox resource loader so font/encoding tables are available.
        runCatching {
            val loaderClass = Class.forName("com.tom_roush.pdfbox.android.PDFBoxResourceLoader")
            val initMethod = loaderClass.getMethod("init", android.content.Context::class.java)
            initMethod.invoke(null, application.applicationContext)
        }
    }

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
                    DocumentKind.Pdf -> {
                        val pageData = PdfTextPositionExtractor.extractPageData(targetPath)
                        pdfPageDataList = pageData
                        val pageTexts = pageData.map { it.text }
                        DocumentContent.PdfContent(request.path, pageTexts)
                    }
                    DocumentKind.Text -> DocumentContent.TextContent(
                        text = DocumentFileIo.readText(request.path),
                        isCodeLike = request.extension != "txt" && request.extension != "log"
                    )
                    DocumentKind.Csv -> DocumentContent.CsvContent(DocumentFileIo.readCsv(request.path))
                    DocumentKind.Word -> when (request.extension) {
                        "docx" -> DocxParser.parseDocx(request.path)
                        "doc" -> DocxParser.parseDoc(request.path)
                        "rtf" -> DocxParser.parseRtf(request.path)
                        else -> DocumentContent.UnsupportedContent("Unsupported Word format.")
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

    private fun findMatchesForPdf(
        text: String,
        positions: List<com.tom_roush.pdfbox.text.TextPosition?>,
        query: String,
        pageIndex: Int
    ): List<SearchMatch> {
        val matches = mutableListOf<SearchMatch>()
        var start = 0
        while (true) {
            val index = text.indexOf(query, startIndex = start, ignoreCase = true)
            if (index < 0) break
            val previewStart = (index - 36).coerceAtLeast(0)
            val previewEnd = (index + query.length + 36).coerceAtMost(text.length)
            
            val pdfRects = getRectsForMatch(index, index + query.length, positions)

            matches += SearchMatch(
                index = index,
                preview = text.substring(previewStart, previewEnd).replace('\n', ' '),
                pageIndex = pageIndex,
                pdfRects = pdfRects
            )
            start = index + query.length
        }
        return matches
    }

    private fun getRectsForMatch(
        startIndex: Int,
        endIndex: Int,
        positions: List<com.tom_roush.pdfbox.text.TextPosition?>
    ): List<android.graphics.RectF> {
        val safeEnd = endIndex.coerceAtMost(positions.size)
        val safeStart = startIndex.coerceAtMost(safeEnd)
        if (safeStart >= safeEnd) return emptyList()

        val glyphs = positions.subList(safeStart, safeEnd).filterNotNull()
        if (glyphs.isEmpty()) return emptyList()

        val rects = mutableListOf<android.graphics.RectF>()
        var currentLineGlyphs = mutableListOf<com.tom_roush.pdfbox.text.TextPosition>()
        
        for (glyph in glyphs) {
            if (currentLineGlyphs.isEmpty()) {
                currentLineGlyphs.add(glyph)
            } else {
                val lastGlyph = currentLineGlyphs.last()
                if (Math.abs(glyph.yDirAdj - lastGlyph.yDirAdj) < 4f) {
                    currentLineGlyphs.add(glyph)
                } else {
                    rects.add(computeUnionRect(currentLineGlyphs))
                    currentLineGlyphs = mutableListOf(glyph)
                }
            }
        }
        if (currentLineGlyphs.isNotEmpty()) {
            rects.add(computeUnionRect(currentLineGlyphs))
        }
        return rects
    }

    private fun computeUnionRect(glyphs: List<com.tom_roush.pdfbox.text.TextPosition>): android.graphics.RectF {
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (glyph in glyphs) {
            val left = glyph.xDirAdj
            val right = glyph.xDirAdj + glyph.widthDirAdj
            val top = glyph.yDirAdj - glyph.heightDir
            val bottom = glyph.yDirAdj

            if (left < minX) minX = left
            if (right > maxX) maxX = right
            if (top < minY) minY = top
            if (bottom > maxY) maxY = bottom
        }

        return android.graphics.RectF(minX, minY, maxX, maxY)
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
                    "docx" -> DocxParser.parseDocx(localPath)
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

    /**
     * Rename the current document on disk.
     * Works for both local files and cached copies of content-URI documents.
     */
    fun renameFile(newBaseName: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (newBaseName.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val oldFile = File(current.request.path)
            val newName = if (newBaseName.endsWith(".${current.request.extension}")) {
                newBaseName
            } else {
                "$newBaseName.${current.request.extension}"
            }
            val newFile = File(oldFile.parent ?: return@launch, newName)

            val succeeded = runCatching { oldFile.renameTo(newFile) }.getOrElse { false }

            if (succeeded) {
                // Update recent files list
                runCatching {
                    RecentFilesManager.removeRecentFile(context, oldFile.absolutePath)
                    RecentFilesManager.addRecentFile(context, newFile.absolutePath)
                }
                // Reload with the new path so all state reflects the new name
                _uiState.value = DocumentViewerState.Loading
                open(newFile.absolutePath)
            } else {
                _uiState.value = current.copy(message = "Could not rename the file.")
            }
        }
    }

    fun updateSearch(query: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val matches = if (query.isBlank()) {
            emptyList()
        } else {
            findMatchesForState(current, query)
        }
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

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun initialEditableText(content: DocumentContent): String = when (content) {
        is DocumentContent.TextContent -> content.text
        is DocumentContent.OfficeTextContent -> content.sections.joinToString("\n\n")
        is DocumentContent.CsvContent -> DocumentFileIo.flattenRows(content.rows)
        is DocumentContent.WordDocumentContent -> content.plainText
        else -> ""
    }

    /**
     * Dispatch to the right search strategy depending on content type.
     * PDFs are searched per-page so we can tag each match with its page index.
     */
    private fun findMatchesForState(state: DocumentViewerState.Ready, query: String): List<SearchMatch> {
        return when (val content = state.content) {
            is DocumentContent.PdfContent -> {
                if (pdfPageDataList.isEmpty()) {
                    // No text extracted – fall back to a single match on the name
                    findMatches(state.request.displayName, query, pageIndex = 0)
                } else {
                    pdfPageDataList.flatMapIndexed { pageIdx, pageData ->
                        findMatchesForPdf(pageData.text, pageData.positions, query, pageIndex = pageIdx)
                    }
                }
            }
            is DocumentContent.TextContent -> {
                val text = if (state.isEditing) state.editedText else content.text
                findMatches(text, query, pageIndex = 0)
            }
            is DocumentContent.OfficeTextContent -> {
                val text = if (state.isEditing) state.editedText else content.sections.joinToString("\n")
                findMatches(text, query, pageIndex = 0)
            }
            is DocumentContent.CsvContent -> {
                val text = DocumentFileIo.flattenRows(if (state.isEditing) state.editedRows else content.rows)
                findMatches(text, query, pageIndex = 0)
            }
            is DocumentContent.WordDocumentContent -> {
                if (state.isEditing) {
                    findMatches(state.editedText, query, pageIndex = 0)
                } else {
                    val pages = DocxParser.paginateElements(content.elements)
                    val matches = mutableListOf<SearchMatch>()
                    pages.forEachIndexed { pageIdx, pageElements ->
                        val pageText = DocxParser.buildPlainText(pageElements)
                        var start = 0
                        while (true) {
                            val index = pageText.indexOf(query, startIndex = start, ignoreCase = true)
                            if (index < 0) break
                            val previewStart = (index - 36).coerceAtLeast(0)
                            val previewEnd = (index + query.length + 36).coerceAtMost(pageText.length)
                            matches += SearchMatch(
                                index = index,
                                preview = pageText.substring(previewStart, previewEnd).replace('\n', ' '),
                                pageIndex = pageIdx
                            )
                            start = index + query.length
                        }
                    }
                    matches
                }
            }
            is DocumentContent.UnsupportedContent -> findMatches(content.message, query, pageIndex = 0)
        }
    }

    private fun findMatches(text: String, query: String, pageIndex: Int): List<SearchMatch> {
        val matches = mutableListOf<SearchMatch>()
        var start = 0
        while (true) {
            val index = text.indexOf(query, startIndex = start, ignoreCase = true)
            if (index < 0) break
            val previewStart = (index - 36).coerceAtLeast(0)
            val previewEnd = (index + query.length + 36).coerceAtMost(text.length)
            matches += SearchMatch(
                index = index,
                preview = text.substring(previewStart, previewEnd).replace('\n', ' '),
                pageIndex = pageIndex
            )
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
