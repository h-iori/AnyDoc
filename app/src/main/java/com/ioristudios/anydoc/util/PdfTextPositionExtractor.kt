package com.ioristudios.anydoc.util

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.IOException
import java.io.StringWriter

class PdfTextPositionExtractor : PDFTextStripper() {
    private val textBuilder = StringBuilder()
    private val charPositions = mutableListOf<TextPosition?>()

    init {
        sortByPosition = true
    }

    fun getPageText(): String = textBuilder.toString()
    fun getPositions(): List<TextPosition?> = charPositions

    @Throws(IOException::class)
    override fun writeString(text: String?, textPositions: List<TextPosition>?) {
        if (text != null && textPositions != null) {
            for (i in text.indices) {
                textBuilder.append(text[i])
                if (i < textPositions.size) {
                    charPositions.add(textPositions[i])
                } else {
                    charPositions.add(null)
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun writeWordSeparator() {
        val sep = wordSeparator
        textBuilder.append(sep)
        for (i in sep.indices) {
            charPositions.add(null)
        }
    }

    @Throws(IOException::class)
    override fun writeLineSeparator() {
        val sep = lineSeparator
        textBuilder.append(sep)
        for (i in sep.indices) {
            charPositions.add(null)
        }
    }

    companion object {
        data class PageTextData(
            val text: String,
            val positions: List<TextPosition?>
        )

        fun extractPageData(path: String): List<PageTextData> {
            val file = File(path)
            if (!file.exists()) return emptyList()

            return try {
                PDDocument.load(file).use { document ->
                    val pageCount = document.numberOfPages
                    val pages = mutableListOf<PageTextData>()
                    for (i in 1..pageCount) {
                        val extractor = PdfTextPositionExtractor()
                        extractor.startPage = i
                        extractor.endPage = i
                        extractor.writeText(document, StringWriter())
                        pages.add(PageTextData(extractor.getPageText(), extractor.getPositions()))
                    }
                    pages
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
