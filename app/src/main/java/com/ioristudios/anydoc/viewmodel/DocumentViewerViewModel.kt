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
    private var slideshowJob: kotlinx.coroutines.Job? = null
    private var documentPassword: String? = null
    private var originalFilePath: String? = null
    private var decryptedTempFile: File? = null
    val uiState: StateFlow<DocumentViewerState> = _uiState.asStateFlow()

    init {
        // Initialize PDFBox resource loader so font/encoding tables are available.
        runCatching {
            val loaderClass = Class.forName("com.tom_roush.pdfbox.android.PDFBoxResourceLoader")
            val initMethod = loaderClass.getMethod("init", android.content.Context::class.java)
            initMethod.invoke(null, application.applicationContext)
        }
        // Pre-warm Apache POI XmlBeans schema type system in the background
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                org.apache.poi.xslf.usermodel.XMLSlideShow().close()
            }
        }
    }

    fun open(pathOrUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            slideshowJob?.cancel()
            _uiState.value = DocumentViewerState.Loading

            // Clean up previous decrypted file and state
            decryptedTempFile?.delete()
            decryptedTempFile = null
            documentPassword = null
            originalFilePath = null

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

            originalFilePath = targetPath

            runCatching {
                val request = DocumentTypeDetector.detect(targetPath, originalUriString)
                
                // Check if password protected
                if (DocumentDecryptor.isPasswordProtected(file, request.extension)) {
                    _uiState.value = DocumentViewerState.PasswordRequired(request)
                } else {
                    openWithRequest(request)
                }
            }.onFailure { error ->
                _uiState.value = DocumentViewerState.Error(displayName ?: file.name, error.localizedMessage ?: "Could not open document.")
            }
        }
    }

    private fun openWithRequest(request: DocumentOpenRequest) {
        val password = documentPassword
        if (password != null) {
            org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(password)
        }
        
        try {
            var parsedPresentation: PresentationContent? = null
            val content = when (request.kind) {
                DocumentKind.Pdf -> {
                    val pageData = PdfTextPositionExtractor.extractPageData(request.path)
                    pdfPageDataList = pageData
                    val pageTexts = pageData.map { it.text }
                    DocumentContent.PdfContent(request.path, pageTexts)
                }
                DocumentKind.Text -> DocumentContent.TextContent(
                    text = DocumentFileIo.readText(request.path),
                    isCodeLike = request.extension != "txt" && request.extension != "log"
                )
                DocumentKind.Markdown -> DocumentContent.TextContent(
                    text = DocumentFileIo.readText(request.path),
                    isCodeLike = false
                )
                DocumentKind.Csv -> XlsxParser.csvToSpreadsheet(DocumentFileIo.readCsv(request.path))
                DocumentKind.Word -> when (request.extension) {
                    "docx" -> DocxParser.parseDocx(request.path)
                    "doc" -> DocxParser.parseDoc(request.path)
                    "rtf" -> DocxParser.parseRtf(request.path)
                    else -> DocumentContent.UnsupportedContent("Unsupported Word format.")
                }
                DocumentKind.Spreadsheet -> when (request.extension) {
                    "xlsx" -> XlsxParser.parse(request.path)
                    "xls" -> XlsxParser.parseXls(request.path)
                    else -> DocumentContent.UnsupportedContent("Unsupported spreadsheet format.")
                }
                DocumentKind.Presentation -> when (request.extension) {
                    "pptx", "ppt" -> {
                        parsedPresentation = runCatching {
                            if (request.extension == "pptx") {
                                PptxParser.parsePptx(request.path)
                            } else {
                                PptxParser.parsePpt(request.path)
                            }
                        }.getOrThrow()
                        DocumentContent.PresentationFileContent(request.path, request.extension)
                    }
                    else -> DocumentContent.UnsupportedContent("Unsupported presentation format.")
                }
                DocumentKind.Unsupported -> DocumentContent.UnsupportedContent("This file type is not supported by AnyDoc yet.")
            }
            val wordLayoutPages = if (content is DocumentContent.WordDocumentContent) {
                MeasurementLayoutEngine().layoutDocument(content.elements, LayoutConfig())
            } else {
                emptyList()
            }
            val editedWordParasMap = mutableMapOf<Int, String>()
            if (content is DocumentContent.WordDocumentContent) {
                collectParagraphsFromElements(content.elements, editedWordParasMap)
            }
            _uiState.value = DocumentViewerState.Ready(
                request = request,
                content = content,
                editedText = initialEditableText(content),
                editedRows = (content as? DocumentContent.CsvContent)?.rows.orEmpty(),
                wordLayoutPages = wordLayoutPages,
                editedWordParagraphs = editedWordParasMap,
                activeSheetIndex = 0,
                presentationState = PresentationUiState(
                    parsedContent = parsedPresentation
                )
            )
        } catch (error: Throwable) {
            _uiState.value = DocumentViewerState.Error(
                request.displayName,
                error.localizedMessage ?: "Could not open document."
            )
        } finally {
            org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(null)
        }
    }

    fun unlock(password: String) {
        val current = _uiState.value
        val request = when (current) {
            is DocumentViewerState.PasswordRequired -> current.request
            else -> return
        }

        _uiState.value = DocumentViewerState.PasswordRequired(request, wrongPasswordAttempted = false)
        
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(originalFilePath ?: request.path)
            val extension = request.extension.lowercase()
            val tempFile = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}_${file.name}")
            
            val decryptedSuccessfully = DocumentDecryptor.decryptFile(file, extension, password, tempFile)
            if (decryptedSuccessfully) {
                documentPassword = password
                if (extension in listOf("pdf", "docx", "xlsx", "pptx")) {
                    decryptedTempFile = tempFile
                    val updatedRequest = request.copy(path = tempFile.absolutePath)
                    openWithRequest(updatedRequest)
                } else {
                    tempFile.delete()
                    openWithRequest(request)
                }
            } else {
                tempFile.delete()
                _uiState.value = DocumentViewerState.PasswordRequired(request, wrongPasswordAttempted = true)
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

    private var layoutDebounceJob: kotlinx.coroutines.Job? = null

    fun updateWordParagraphText(originalIndex: Int, newText: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val newMap = current.editedWordParagraphs.toMutableMap().apply {
            put(originalIndex, newText)
        }
        _uiState.value = current.copy(editedWordParagraphs = newMap)

        layoutDebounceJob?.cancel()
        layoutDebounceJob = viewModelScope.launch(Dispatchers.Default) {
            kotlinx.coroutines.delay(800)
            val docContent = current.content as? DocumentContent.WordDocumentContent ?: return@launch
            
            val updatedElements = updateElementsWithEdits(docContent.elements, newMap)
            val newPages = MeasurementLayoutEngine().layoutDocument(updatedElements, LayoutConfig())
            
            val stateAfterDelay = _uiState.value as? DocumentViewerState.Ready ?: return@launch
            _uiState.value = stateAfterDelay.copy(
                wordLayoutPages = newPages
            )
        }
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

    // ─── Spreadsheet-specific operations ─────────────────────────────────────

    fun switchSheet(sheetIndex: Int) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val content = current.content as? DocumentContent.SpreadsheetContent ?: return
        if (sheetIndex < 0 || sheetIndex >= content.sheets.size) return
        _uiState.value = current.copy(
            activeSheetIndex = sheetIndex,
            selectedCell = null,
            formulaBarText = ""
        )
    }

    fun selectCell(row: Int, col: Int) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val content = current.content as? DocumentContent.SpreadsheetContent ?: return
        val sheet = content.sheets.getOrNull(current.activeSheetIndex) ?: return
        val cell = sheet.rows.find { it.rowIndex == row }?.cells?.get(col)
        val editKey = "${current.activeSheetIndex}:$row:$col"
        val displayValue = current.editedCells[editKey] ?: cell?.rawValue ?: cell?.value ?: ""
        _uiState.value = current.copy(
            selectedCell = Pair(row, col),
            formulaBarText = displayValue
        )
    }

    fun updateSpreadsheetCell(row: Int, col: Int, value: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val editKey = "${current.activeSheetIndex}:$row:$col"
        val newEditedCells = current.editedCells.toMutableMap().apply {
            put(editKey, value)
        }
        _uiState.value = current.copy(editedCells = newEditedCells)
    }

    fun updateFormulaBar(text: String) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = current.copy(formulaBarText = text)
        // Also apply to selected cell
        current.selectedCell?.let { (row, col) ->
            val editKey = "${current.activeSheetIndex}:$row:$col"
            val newEditedCells = current.editedCells.toMutableMap().apply {
                put(editKey, text)
            }
            _uiState.value = (_uiState.value as? DocumentViewerState.Ready)?.copy(editedCells = newEditedCells) ?: return
        }
    }

    fun addSpreadsheetRow() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (!current.isEditing) return
        val content = current.content as? DocumentContent.SpreadsheetContent ?: return
        val sheet = content.sheets.getOrNull(current.activeSheetIndex) ?: return
        val newRow = SpreadsheetRow(
            rowIndex = sheet.rowCount,
            cells = emptyMap()
        )
        val updatedSheet = sheet.copy(
            rows = sheet.rows + newRow,
            rowCount = sheet.rowCount + 1
        )
        val updatedSheets = content.sheets.toMutableList().apply {
            set(current.activeSheetIndex, updatedSheet)
        }
        _uiState.value = current.copy(
            content = content.copy(sheets = updatedSheets)
        )
    }

    fun addSpreadsheetColumn() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        if (!current.isEditing) return
        val content = current.content as? DocumentContent.SpreadsheetContent ?: return
        val sheet = content.sheets.getOrNull(current.activeSheetIndex) ?: return
        val updatedSheet = sheet.copy(
            columnCount = sheet.columnCount + 1
        )
        val updatedSheets = content.sheets.toMutableList().apply {
            set(current.activeSheetIndex, updatedSheet)
        }
        _uiState.value = current.copy(
            content = content.copy(sheets = updatedSheets)
        )
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
                val password = documentPassword
                if (password != null) {
                    org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(password)
                }
                
                try {
                    when {
                        current.request.extension == "csv" && current.content is DocumentContent.SpreadsheetContent -> {
                            // Convert SpreadsheetContent back to CSV rows for saving
                            val sheet = current.content.sheets.getOrNull(current.activeSheetIndex)
                            if (sheet != null) {
                                val csvRows = (0 until sheet.rowCount).map { rowIdx ->
                                    val row = sheet.rows.find { it.rowIndex == rowIdx }
                                    (0 until sheet.columnCount).map { colIdx ->
                                        val editKey = "${current.activeSheetIndex}:$rowIdx:$colIdx"
                                        current.editedCells[editKey] ?: row?.cells?.get(colIdx)?.value ?: ""
                                    }
                                }
                                DocumentFileIo.writeCsv(localPath, csvRows)
                            }
                        }
                        (current.request.extension == "xlsx" || current.request.extension == "xls") && current.content is DocumentContent.SpreadsheetContent -> {
                            val sheetEditedCells = current.editedCells.filter { (key, _) ->
                                key.startsWith("${current.activeSheetIndex}:")
                            }.mapKeys { (key, _) ->
                                key.substringAfter(":")
                            }
                            val file = File(localPath)
                            val isZip = file.exists() && file.length() >= 4 && file.inputStream().use { fis ->
                                val b = ByteArray(2)
                                fis.read(b) == 2 && b[0] == 0x50.toByte() && b[1] == 0x4B.toByte()
                            }
                            if (isZip) {
                                DocumentFileIo.writeXlsxSheet(
                                    localPath,
                                    current.activeSheetIndex,
                                    sheetEditedCells,
                                    current.content
                                )
                            } else {
                                DocumentFileIo.writeXlsSheet(
                                    localPath,
                                    current.activeSheetIndex,
                                    sheetEditedCells
                                )
                            }
                        }
                        current.request.extension == "csv" -> DocumentFileIo.writeCsv(localPath, current.editedRows)
                        current.request.extension == "xlsx" -> DocumentFileIo.writeXlsxRows(localPath, current.editedRows)
                        current.request.extension == "docx" -> DocumentFileIo.writeDocxInPlace(localPath, current.editedWordParagraphs)
                        else -> DocumentFileIo.writeText(localPath, current.editedText)
                    }
                } finally {
                    org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(null)
                }

                // Copy back to original encrypted file/URI
                val origPath = originalFilePath
                val originalUri = current.request.originalUri
                
                if (password != null && origPath != null) {
                    val isLegacy = current.request.extension.lowercase() in listOf("doc", "xls", "ppt")
                    if (isLegacy) {
                        // Written directly to origPath already. Copy to content URI if needed.
                        if (!originalUri.isNullOrBlank()) {
                            val uri = Uri.parse(originalUri)
                            context.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                                File(localPath).inputStream().use { inputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                        }
                    } else {
                        // Re-encrypt the temp decrypted file back into original path
                        val encryptedTemp = File(context.cacheDir, "encrypted_${System.currentTimeMillis()}")
                        try {
                            DocumentDecryptor.encryptOffice(File(localPath), password, encryptedTemp)
                            encryptedTemp.copyTo(File(origPath), overwrite = true)
                            if (!originalUri.isNullOrBlank()) {
                                val uri = Uri.parse(originalUri)
                                context.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                                    encryptedTemp.inputStream().use { inputStream ->
                                        inputStream.copyTo(outputStream)
                                    }
                                }
                            }
                        } finally {
                            encryptedTemp.delete()
                        }
                    }
                } else {
                    if (!originalUri.isNullOrBlank() && localPath.startsWith(context.cacheDir.absolutePath)) {
                        val uri = Uri.parse(originalUri)
                        context.contentResolver.openOutputStream(uri, "rwt")?.use { outputStream ->
                            File(localPath).inputStream().use { inputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                }
            }

            result.onSuccess {
                val savedContent = when {
                    current.request.extension == "xlsx" -> XlsxParser.parse(localPath)
                    current.request.extension == "xls" -> XlsxParser.parseXls(localPath)
                    current.request.extension == "csv" -> XlsxParser.csvToSpreadsheet(DocumentFileIo.readCsv(localPath))
                    current.request.extension == "docx" -> DocxParser.parseDocx(localPath)
                    else -> DocumentContent.TextContent(
                        text = current.editedText,
                        isCodeLike = (current.content as? DocumentContent.TextContent)?.isCodeLike == true
                    )
                }
                val savedLayoutPages = if (savedContent is DocumentContent.WordDocumentContent) {
                    RenderCache.clear()
                    MeasurementLayoutEngine().layoutDocument(savedContent.elements, LayoutConfig())
                } else {
                    emptyList()
                }
                val editedWordParasMap = mutableMapOf<Int, String>()
                if (savedContent is DocumentContent.WordDocumentContent) {
                    collectParagraphsFromElements(savedContent.elements, editedWordParasMap)
                }
                _uiState.value = current.copy(
                    content = savedContent,
                    isSaving = false,
                    isEditing = false,
                    message = "Saved changes.",
                    wordLayoutPages = savedLayoutPages,
                    editedWordParagraphs = editedWordParasMap,
                    editedCells = emptyMap(),
                    selectedCell = null,
                    formulaBarText = ""
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
        is DocumentContent.SpreadsheetContent -> {
            content.sheets.firstOrNull()?.rows?.joinToString("\n") { row ->
                row.cells.values.joinToString("\t") { it.value }
            }.orEmpty()
        }
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
                val matches = mutableListOf<SearchMatch>()
                state.wordLayoutPages.forEachIndexed { pageIdx, page ->
                    val pageText = extractPageText(page)
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
            is DocumentContent.SpreadsheetContent -> {
                val matches = mutableListOf<SearchMatch>()
                content.sheets.forEachIndexed { sheetIdx, sheet ->
                    sheet.rows.forEach { row ->
                        row.cells.forEach { (colIdx, cell) ->
                            val editKey = "$sheetIdx:${row.rowIndex}:$colIdx"
                            val text = state.editedCells[editKey] ?: cell.value
                            var start = 0
                            while (true) {
                                val index = text.indexOf(query, start, ignoreCase = true)
                                if (index < 0) break
                                val cellRef = "${XlsxParser.columnName(colIdx)}${row.rowIndex + 1}"
                                matches += SearchMatch(
                                    index = index,
                                    preview = "[${sheet.name}] $cellRef: $text",
                                    pageIndex = 0,
                                    sheetIndex = sheetIdx,
                                    rowIndex = row.rowIndex,
                                    colIndex = colIdx
                                )
                                start = index + query.length
                            }
                        }
                    }
                }
                matches
            }
            is DocumentContent.PresentationFileContent -> {
                val matches = mutableListOf<SearchMatch>()
                val presentation = state.presentationState.parsedContent
                presentation?.slides?.forEachIndexed { slideIdx, slide ->
                    val slideTextBuilder = java.lang.StringBuilder()
                    fun extractText(element: SlideElement) {
                        when (element) {
                            is SlideElement.TextBox -> {
                                element.paragraphs.forEach { p ->
                                    p.runs.forEach { r -> slideTextBuilder.append(r.text) }
                                    slideTextBuilder.append("\n")
                                }
                            }
                            is SlideElement.ShapeBox -> {
                                element.paragraphs.forEach { p ->
                                    p.runs.forEach { r -> slideTextBuilder.append(r.text) }
                                    slideTextBuilder.append("\n")
                                }
                            }
                            is SlideElement.GroupBox -> {
                                element.elements.forEach { extractText(it) }
                            }
                            is SlideElement.TableBox -> {
                                element.rows.forEach { row ->
                                    row.cells.forEach { cell ->
                                        cell.paragraphs.forEach { p ->
                                            p.runs.forEach { r -> slideTextBuilder.append(r.text) }
                                            slideTextBuilder.append("\n")
                                        }
                                    }
                                }
                            }
                            is SlideElement.ImageBox -> {}
                        }
                    }
                    slide.elements.forEach { extractText(it) }
                    val slideText = slideTextBuilder.toString()
                    var start = 0
                    while (true) {
                        val index = slideText.indexOf(query, startIndex = start, ignoreCase = true)
                        if (index < 0) break
                        val previewStart = (index - 36).coerceAtLeast(0)
                        val previewEnd = (index + query.length + 36).coerceAtMost(slideText.length)
                        matches += SearchMatch(
                            index = index,
                            preview = "[Slide ${slideIdx + 1}] " + slideText.substring(previewStart, previewEnd).replace('\n', ' '),
                            pageIndex = slideIdx
                        )
                        start = index + query.length
                    }
                }
                matches
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

    private fun extractTextFromElement(element: PositionedElement): String {
        return when (element) {
            is PositionedElement.Paragraph -> {
                element.para.spans.joinToString("") { it.text } + "\n"
            }
            is PositionedElement.Table -> {
                val sb = java.lang.StringBuilder()
                element.cellElements.forEach { row ->
                    row.forEach { cell ->
                        cell.forEach { subEl ->
                            sb.append(extractTextFromElement(subEl))
                        }
                    }
                }
                sb.toString()
            }
            is PositionedElement.Image -> ""
        }
    }

    private fun extractPageText(page: LayoutPage): String {
        return page.elements.joinToString("") { extractTextFromElement(it) }
    }

    private fun collectParagraphsFromElements(elements: List<DocxElement>, map: MutableMap<Int, String>) {
        for (element in elements) {
            when (element) {
                is DocxElement.Paragraph -> {
                    val p = element.para
                    if (p.originalIndex != -1) {
                        map[p.originalIndex] = p.spans.joinToString("") { it.text }
                    }
                }
                is DocxElement.Table -> {
                    for (row in element.table.rows) {
                        for (cell in row.cells) {
                            collectParagraphsFromElements(cell.elements, map)
                        }
                    }
                }
                is DocxElement.Image -> {}
            }
        }
    }

    private fun updateElementsWithEdits(
        elements: List<DocxElement>,
        edits: Map<Int, String>
    ): List<DocxElement> {
        return elements.map { element ->
            when (element) {
                is DocxElement.Paragraph -> {
                    val p = element.para
                    if (p.originalIndex != -1 && edits.containsKey(p.originalIndex)) {
                        val newText = edits[p.originalIndex] ?: ""
                        val originalText = p.spans.joinToString("") { it.text }
                        if (newText != originalText) {
                            DocxElement.Paragraph(updateParagraphSpans(p, newText))
                        } else {
                            element
                        }
                    } else {
                        element
                    }
                }
                is DocxElement.Table -> {
                    val updatedRows = element.table.rows.map { row ->
                        val updatedCells = row.cells.map { cell ->
                            val updatedCellElements = updateElementsWithEdits(cell.elements, edits)
                            cell.copy(elements = updatedCellElements)
                        }
                        row.copy(cells = updatedCells)
                    }
                    DocxElement.Table(element.table.copy(rows = updatedRows))
                }
                is DocxElement.Image -> element
            }
        }
    }

    private fun updateParagraphSpans(para: DocxParagraph, newText: String): DocxParagraph {
        val firstSpan = para.spans.firstOrNull()
        val newSpans = if (firstSpan != null) {
            listOf(firstSpan.copy(text = newText))
        } else {
            listOf(DocxSpan(text = newText))
        }
        return para.copy(spans = newSpans)
    }

    // ─── Presentation Helper Methods ─────────────────────────────────────────

    fun goToSlide(index: Int) {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val presentation = current.presentationState.parsedContent ?: return
        if (index in 0 until presentation.slides.size) {
            _uiState.value = current.copy(
                presentationState = current.presentationState.copy(
                    currentSlide = index
                )
            )
        }
    }

    fun nextSlide() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val presentation = current.presentationState.parsedContent ?: return
        val nextIndex = current.presentationState.currentSlide + 1
        if (nextIndex < presentation.slides.size) {
            goToSlide(nextIndex)
        } else {
            if (current.presentationState.isSlideshowActive) {
                goToSlide(0)
            }
        }
    }

    fun previousSlide() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val prevIndex = current.presentationState.currentSlide - 1
        if (prevIndex >= 0) {
            goToSlide(prevIndex)
        }
    }

    fun startSlideshow() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        val presentation = current.presentationState.parsedContent ?: return
        if (presentation.slides.isEmpty()) return

        slideshowJob?.cancel()
        _uiState.value = current.copy(
            presentationState = current.presentationState.copy(
                isSlideshowActive = true,
                isFullscreen = true
            )
        )

        slideshowJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                val state = _uiState.value as? DocumentViewerState.Ready ?: break
                if (!state.presentationState.isSlideshowActive) break
                
                kotlinx.coroutines.delay(state.presentationState.slideshowIntervalMs)
                
                val latestState = _uiState.value as? DocumentViewerState.Ready ?: break
                val totalSlides = latestState.presentationState.parsedContent?.slides?.size ?: 0
                val currentIdx = latestState.presentationState.currentSlide
                
                if (totalSlides > 0) {
                    val nextIdx = (currentIdx + 1) % totalSlides
                    launch(Dispatchers.Main) {
                        goToSlide(nextIdx)
                    }
                }
            }
        }
    }

    fun stopSlideshow() {
        slideshowJob?.cancel()
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = current.copy(
            presentationState = current.presentationState.copy(
                isSlideshowActive = false
            )
        )
    }

    fun toggleFullscreen() {
        val current = _uiState.value as? DocumentViewerState.Ready ?: return
        _uiState.value = current.copy(
            presentationState = current.presentationState.copy(
                isFullscreen = !current.presentationState.isFullscreen
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        slideshowJob?.cancel()
        decryptedTempFile?.delete()
    }
}
