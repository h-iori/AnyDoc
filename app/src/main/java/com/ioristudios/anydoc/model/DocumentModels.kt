package com.ioristudios.anydoc.model

enum class DocumentKind {
    Pdf,
    Presentation,
    Word,
    Spreadsheet,
    Csv,
    Text,
    Unsupported
}

data class DocumentOpenRequest(
    val path: String,
    val displayName: String,
    val extension: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val kind: DocumentKind,
    val canEdit: Boolean,
    val originalUri: String? = null
)

data class SearchMatch(
    val index: Int,
    val preview: String,
    val pageIndex: Int = 0    // For PDFs: which page (0-based) this match lives on
)

sealed class DocumentContent {
    data class TextContent(val text: String, val isCodeLike: Boolean) : DocumentContent()
    data class CsvContent(val rows: List<List<String>>) : DocumentContent()
    data class OfficeTextContent(val sections: List<String>) : DocumentContent()
    data class PdfContent(
        val path: String,
        /**
         * Extracted text for each page (index = page number - 1).
         * Empty list when extraction has not been attempted yet.
         */
        val pageTexts: List<String> = emptyList()
    ) : DocumentContent()
    data class UnsupportedContent(val message: String) : DocumentContent()
}

sealed class DocumentViewerState {
    object Loading : DocumentViewerState()
    data class Ready(
        val request: DocumentOpenRequest,
        val content: DocumentContent,
        val isEditing: Boolean = false,
        val editedText: String = "",
        val editedRows: List<List<String>> = emptyList(),
        val searchQuery: String = "",
        val searchMatches: List<SearchMatch> = emptyList(),
        val activeMatch: Int = -1,
        val message: String? = null,
        val isSaving: Boolean = false
    ) : DocumentViewerState()

    data class Error(
        val displayName: String,
        val message: String
    ) : DocumentViewerState()
}
