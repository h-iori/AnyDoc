package com.ioristudios.anydoc.model

import android.graphics.RectF

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
    val pageIndex: Int = 0,    // For PDFs: which page (0-based) this match lives on
    val pdfRects: List<RectF> = emptyList()
)


sealed class DocumentContent {
    data class TextContent(val text: String, val isCodeLike: Boolean) : DocumentContent()
    data class CsvContent(val rows: List<List<String>>) : DocumentContent()
    data class OfficeTextContent(val sections: List<String>) : DocumentContent()
    data class WordDocumentContent(
        val elements: List<DocxElement>,
        val plainText: String
    ) : DocumentContent()
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

data class DocxSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSize: Float? = null,
    val color: String? = null,
    val fontFamily: String? = null
)

enum class DocxParagraphStyle { Heading1, Heading2, Heading3, Heading4, Body, ListItem }
enum class DocxTextAlignment { Start, Center, End, Justify }

data class DocxParagraph(
    val spans: List<DocxSpan>,
    val style: DocxParagraphStyle = DocxParagraphStyle.Body,
    val isPageBreak: Boolean = false,
    val alignment: DocxTextAlignment = DocxTextAlignment.Start,
    val indentStartTwips: Int = 0,
    val hangingTwips: Int = 0,
    val spacingBeforeTwips: Int = 0,
    val spacingAfterTwips: Int = 120,
    val lineSpacingTwips: Int? = null,
    val listLevel: Int = 0,
    val isNumbered: Boolean = false,
    val originalIndex: Int = -1
)

data class DocxTableCell(
    val elements: List<DocxElement>,
    val widthTwips: Int? = null
)
data class DocxTableRow(val cells: List<DocxTableCell>)
data class DocxTable(
    val rows: List<DocxTableRow>,
    val widthTwips: Int? = null
)

data class DocxImage(
    val entryName: String,
    val bytes: ByteArray? = null,
    val widthEmu: Long? = null,
    val heightEmu: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DocxImage
        if (entryName != other.entryName) return false
        if (widthEmu != other.widthEmu) return false
        if (heightEmu != other.heightEmu) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = entryName.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + (widthEmu?.hashCode() ?: 0)
        result = 31 * result + (heightEmu?.hashCode() ?: 0)
        return result
    }
}

sealed class DocxElement {
    data class Paragraph(val para: DocxParagraph) : DocxElement()
    data class Table(val table: DocxTable) : DocxElement()
    data class Image(val img: DocxImage) : DocxElement()
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
        val isSaving: Boolean = false,
        val wordLayoutPages: List<com.ioristudios.anydoc.model.LayoutPage> = emptyList(),
        val editedWordParagraphs: Map<Int, String> = emptyMap()
    ) : DocumentViewerState()

    data class Error(
        val displayName: String,
        val message: String
    ) : DocumentViewerState()
}
