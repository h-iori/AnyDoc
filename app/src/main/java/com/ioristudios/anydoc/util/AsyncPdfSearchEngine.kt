package com.ioristudios.anydoc.util

import android.graphics.RectF
import com.ioristudios.anydoc.model.SearchMatch
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Asynchronously searches PDF documents page-by-page on a background thread.
 * Emits matches progressively as they are discovered so the UI remains 100% responsive.
 */
object AsyncPdfSearchEngine {

    fun search(filePath: String, query: String): Flow<List<SearchMatch>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val file = File(filePath)
        if (!file.exists()) {
            emit(emptyList())
            return@flow
        }

        val allMatches = mutableListOf<SearchMatch>()

        try {
            PDDocument.load(file).use { doc ->
                val totalPages = doc.numberOfPages
                for (pageIdx in 0 until totalPages) {
                    if (!coroutineContext.isActive) break

                    val pageData = PdfTextPositionExtractor.extractSinglePage(doc, pageIdx + 1)
                    val text = pageData.text
                    val positions = pageData.positions

                    var start = 0
                    var pageFoundCount = 0
                    while (true) {
                        val index = text.indexOf(query, startIndex = start, ignoreCase = true)
                        if (index < 0) break
                        val previewStart = (index - 36).coerceAtLeast(0)
                        val previewEnd = (index + query.length + 36).coerceAtMost(text.length)
                        val pdfRects = getRectsForMatch(index, index + query.length, positions)

                        allMatches.add(
                            SearchMatch(
                                index = index,
                                preview = text.substring(previewStart, previewEnd).replace('\n', ' '),
                                pageIndex = pageIdx,
                                pdfRects = pdfRects
                            )
                        )
                        pageFoundCount++
                        start = index + query.length
                    }

                    // Progressively emit if matches were found on this page or periodically
                    if (pageFoundCount > 0 || (pageIdx + 1) % 10 == 0) {
                        emit(allMatches.toList())
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emit(allMatches.toList())
    }.flowOn(Dispatchers.IO)

    fun getRectsForMatch(
        startIndex: Int,
        endIndex: Int,
        positions: List<TextPosition?>
    ): List<RectF> {
        val safeEnd = endIndex.coerceAtMost(positions.size)
        val safeStart = startIndex.coerceAtMost(safeEnd)
        if (safeStart >= safeEnd) return emptyList()

        val glyphs = positions.subList(safeStart, safeEnd).filterNotNull()
        if (glyphs.isEmpty()) return emptyList()

        val rects = mutableListOf<RectF>()
        var currentLineGlyphs = mutableListOf<TextPosition>()

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

    private fun computeUnionRect(glyphs: List<TextPosition>): RectF {
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

        return RectF(minX, minY, maxX, maxY)
    }
}
