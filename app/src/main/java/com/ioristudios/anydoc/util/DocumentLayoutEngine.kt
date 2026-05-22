package com.ioristudios.anydoc.util

import com.ioristudios.anydoc.model.*

interface DocumentLayoutEngine {
    fun layoutDocument(
        elements: List<DocxElement>,
        config: LayoutConfig
    ): List<LayoutPage>
}

class MeasurementLayoutEngine : DocumentLayoutEngine {
    
    override fun layoutDocument(
        elements: List<DocxElement>,
        config: LayoutConfig
    ): List<LayoutPage> {
        val pages = mutableListOf<LayoutPage>()
        var currentPageElements = mutableListOf<PositionedElement>()
        var pageNum = 1
        var currentY = 0f // Relative to config.marginTopPoints
        
        fun startNewPage() {
            pages.add(LayoutPage(pageNum, currentPageElements.toList(), config))
            currentPageElements = mutableListOf()
            pageNum++
            currentY = 0f
        }
        
        val queue = java.util.ArrayDeque(elements)
        
        while (!queue.isEmpty()) {
            val element = queue.poll() ?: break
            
            when (element) {
                is DocxElement.Paragraph -> {
                    val para = element.para
                    
                    if (para.isPageBreak) {
                        if (currentPageElements.isNotEmpty() || currentY > 0f) {
                            startNewPage()
                        }
                        if (para.spans.isNotEmpty()) {
                            queue.addFirst(DocxElement.Paragraph(para.copy(isPageBreak = false)))
                        }
                        continue
                    }
                    
                    val leftIndent = if (para.style == DocxParagraphStyle.ListItem) {
                        (18f + para.listLevel * 18f) + (para.indentStartTwips / 40f)
                    } else {
                        para.indentStartTwips / 20f
                    }
                    val paraWidth = (config.contentWidthPoints - leftIndent).coerceAtLeast(10f)
                    
                    val measured = LayoutMetrics.measureParagraph(para, paraWidth, config.scaleFactor)
                    
                    val spaceBefore = measured.spacingBefore
                    val spaceAfter = measured.spacingAfter
                    val layoutHeight = measured.layoutHeight
                    val totalElementHeight = spaceBefore + layoutHeight + spaceAfter
                    
                    if (currentY + spaceBefore + layoutHeight <= config.contentHeightPoints) {
                        currentPageElements.add(
                            PositionedElement.Paragraph(
                                para = para,
                                x = leftIndent,
                                y = currentY + spaceBefore,
                                width = paraWidth,
                                height = layoutHeight
                            )
                        )
                        currentY += totalElementHeight
                    } else {
                        val availableHeightForText = config.contentHeightPoints - currentY - spaceBefore
                        
                        var fittingLineCount = 0
                        if (availableHeightForText > 0f) {
                            val layout = measured.layout
                            val maxHeightPx = availableHeightForText * config.scaleFactor
                            for (i in 0 until layout.lineCount) {
                                if (layout.getLineBottom(i) <= maxHeightPx) {
                                    fittingLineCount++
                                } else {
                                    break
                                }
                            }
                        }
                        
                        if (fittingLineCount > 0) {
                            val splitCharIndex = measured.layout.getLineStart(fittingLineCount)
                            val totalTextLength = para.spans.sumOf { it.text.length }
                            
                            if (splitCharIndex in 1 until totalTextLength) {
                                val (part1Spans, part2Spans) = LayoutMetrics.splitSpans(para.spans, splitCharIndex)
                                
                                val part1Para = para.copy(spans = part1Spans, spacingAfterTwips = 0)
                                val part1Height = measured.layout.getLineBottom(fittingLineCount - 1) / config.scaleFactor
                                
                                currentPageElements.add(
                                    PositionedElement.Paragraph(
                                        para = part1Para,
                                        x = leftIndent,
                                        y = currentY + spaceBefore,
                                        width = paraWidth,
                                        height = part1Height
                                    )
                                )
                                
                                val part2Para = para.copy(spans = part2Spans, spacingBeforeTwips = 0)
                                queue.addFirst(DocxElement.Paragraph(part2Para))
                            } else {
                                if (currentPageElements.isEmpty() && currentY == 0f) {
                                    currentPageElements.add(
                                        PositionedElement.Paragraph(
                                            para = para,
                                            x = leftIndent,
                                            y = currentY + spaceBefore,
                                            width = paraWidth,
                                            height = layoutHeight
                                        )
                                    )
                                    currentY += totalElementHeight
                                } else {
                                    startNewPage()
                                    queue.addFirst(element)
                                }
                            }
                        } else {
                            if (currentPageElements.isEmpty() && currentY == 0f) {
                                currentPageElements.add(
                                    PositionedElement.Paragraph(
                                        para = para,
                                        x = leftIndent,
                                        y = currentY + spaceBefore,
                                        width = paraWidth,
                                        height = layoutHeight
                                    )
                                )
                                currentY += totalElementHeight
                            } else {
                                startNewPage()
                                queue.addFirst(element)
                            }
                        }
                    }
                }
                
                is DocxElement.Image -> {
                    val img = element.img
                    val measured = LayoutMetrics.measureImage(img, config.contentWidthPoints)
                    val imgHeight = measured.height
                    val imgWidth = measured.width
                    val imageX = (config.contentWidthPoints - imgWidth) / 2f
                    
                    if (currentY + imgHeight <= config.contentHeightPoints) {
                        currentPageElements.add(
                            PositionedElement.Image(
                                img = img,
                                x = imageX,
                                y = currentY,
                                width = imgWidth,
                                height = imgHeight
                            )
                        )
                        currentY += imgHeight + 12f
                    } else {
                        if (currentPageElements.isEmpty() && currentY == 0f) {
                            currentPageElements.add(
                                PositionedElement.Image(
                                    img = img,
                                    x = imageX,
                                    y = currentY,
                                    width = imgWidth,
                                    height = imgHeight
                                )
                            )
                            currentY += imgHeight + 12f
                        } else {
                            startNewPage()
                            queue.addFirst(element)
                        }
                    }
                }
                
                is DocxElement.Table -> {
                    val table = element.table
                    val colWidths = LayoutMetrics.determineTableColumnWidths(table, config.contentWidthPoints)
                    val rowsToProcess = table.rows
                    
                    var currentTableRows = mutableListOf<DocxTableRow>()
                    var currentTableRowHeights = mutableListOf<Float>()
                    var currentTableCellElements = mutableListOf<List<List<PositionedElement>>>()
                    var tableY = currentY
                    
                    var rowIdx = 0
                    while (rowIdx < rowsToProcess.size) {
                        val row = rowsToProcess[rowIdx]
                        val (rowHeight, cellLayouts) = LayoutMetrics.layoutTableRow(row, colWidths, config.scaleFactor)
                        
                        if (tableY + rowHeight <= config.contentHeightPoints) {
                            currentTableRows.add(row)
                            currentTableRowHeights.add(rowHeight)
                            currentTableCellElements.add(cellLayouts)
                            tableY += rowHeight
                            rowIdx++
                        } else {
                            if (currentTableRows.isNotEmpty()) {
                                currentPageElements.add(
                                    PositionedElement.Table(
                                        table = DocxTable(currentTableRows.toList(), table.widthTwips),
                                        x = 0f,
                                        y = currentY,
                                        width = config.contentWidthPoints,
                                        height = tableY - currentY,
                                        rowHeights = currentTableRowHeights.toList(),
                                        cellWidths = colWidths,
                                        cellElements = currentTableCellElements.toList()
                                    )
                                )
                                currentY = tableY + 12f
                                
                                val remainingRows = rowsToProcess.subList(rowIdx, rowsToProcess.size)
                                queue.addFirst(DocxElement.Table(DocxTable(remainingRows, table.widthTwips)))
                                currentTableRows = mutableListOf()
                                break
                            } else {
                                if (currentPageElements.isEmpty() && currentY == 0f) {
                                    currentTableRows.add(row)
                                    currentTableRowHeights.add(rowHeight)
                                    currentTableCellElements.add(cellLayouts)
                                    tableY += rowHeight
                                    
                                    currentPageElements.add(
                                        PositionedElement.Table(
                                            table = DocxTable(currentTableRows.toList(), table.widthTwips),
                                            x = 0f,
                                            y = currentY,
                                            width = config.contentWidthPoints,
                                            height = tableY - currentY,
                                            rowHeights = currentTableRowHeights.toList(),
                                            cellWidths = colWidths,
                                            cellElements = currentTableCellElements.toList()
                                        )
                                    )
                                    currentY = tableY + 12f
                                    val remainingRows = rowsToProcess.subList(rowIdx + 1, rowsToProcess.size)
                                    if (remainingRows.isNotEmpty()) {
                                        queue.addFirst(DocxElement.Table(DocxTable(remainingRows, table.widthTwips)))
                                    }
                                    currentTableRows = mutableListOf()
                                    break
                                } else {
                                    startNewPage()
                                    val remainingRows = rowsToProcess.subList(rowIdx, rowsToProcess.size)
                                    queue.addFirst(DocxElement.Table(DocxTable(remainingRows, table.widthTwips)))
                                    break
                                }
                            }
                        }
                    }
                    
                    if (currentTableRows.isNotEmpty()) {
                        currentPageElements.add(
                            PositionedElement.Table(
                                table = DocxTable(currentTableRows.toList(), table.widthTwips),
                                x = 0f,
                                y = currentY,
                                width = config.contentWidthPoints,
                                height = tableY - currentY,
                                rowHeights = currentTableRowHeights.toList(),
                                cellWidths = colWidths,
                                cellElements = currentTableCellElements.toList()
                            )
                        )
                        currentY = tableY + 12f
                    }
                }
            }
        }
        
        if (currentPageElements.isNotEmpty() || pages.isEmpty()) {
            pages.add(LayoutPage(pageNum, currentPageElements.toList(), config))
        }
        
        return pages
    }
}

object LayoutMetrics {
    
    fun getAndroidTypeface(fontFamily: String?, bold: Boolean, italic: Boolean): android.graphics.Typeface {
        val style = when {
            bold && italic -> android.graphics.Typeface.BOLD_ITALIC
            bold -> android.graphics.Typeface.BOLD
            italic -> android.graphics.Typeface.ITALIC
            else -> android.graphics.Typeface.NORMAL
        }
        val baseFamily = when (fontFamily?.lowercase()) {
            "times new roman", "times", "serif", "georgia" -> android.graphics.Typeface.SERIF
            "courier new", "courier", "monospace", "consolas", "lucida console" -> android.graphics.Typeface.MONOSPACE
            else -> android.graphics.Typeface.SANS_SERIF
        }
        return android.graphics.Typeface.create(baseFamily, style)
    }

    fun getAndroidAlignment(alignment: DocxTextAlignment): android.text.Layout.Alignment {
        return when (alignment) {
            DocxTextAlignment.Center -> android.text.Layout.Alignment.ALIGN_CENTER
            DocxTextAlignment.End -> android.text.Layout.Alignment.ALIGN_OPPOSITE
            DocxTextAlignment.Justify, DocxTextAlignment.Start -> android.text.Layout.Alignment.ALIGN_NORMAL
        }
    }

    fun buildSpannable(
        para: DocxParagraph,
        scaleFactor: Float,
        searchQuery: String = "",
        activeMatchIndex: Int = -1,
        matchesBeforeParagraph: Int = 0
    ): android.text.SpannableStringBuilder {
        val builder = android.text.SpannableStringBuilder()
        para.spans.forEach { span ->
            val start = builder.length
            builder.append(span.text)
            val end = builder.length

            val typefaceStyle = when {
                span.bold && span.italic -> android.graphics.Typeface.BOLD_ITALIC
                span.bold -> android.graphics.Typeface.BOLD
                span.italic -> android.graphics.Typeface.ITALIC
                else -> android.graphics.Typeface.NORMAL
            }
            if (typefaceStyle != android.graphics.Typeface.NORMAL) {
                builder.setSpan(
                    android.text.style.StyleSpan(typefaceStyle),
                    start,
                    end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val typeface = getAndroidTypeface(span.fontFamily, span.bold, span.italic)
            builder.setSpan(
                android.text.style.TypefaceSpan(typeface),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (span.underline) {
                builder.setSpan(
                    android.text.style.UnderlineSpan(),
                    start,
                    end,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val fontSizePoints = span.fontSize ?: 14f
            builder.setSpan(
                android.text.style.AbsoluteSizeSpan((fontSizePoints * scaleFactor).toInt()),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            val colorHex = span.color
            val colorInt = if (colorHex != null && colorHex != "auto") {
                runCatching { android.graphics.Color.parseColor("#$colorHex") }.getOrElse { android.graphics.Color.BLACK }
            } else {
                android.graphics.Color.BLACK
            }
            builder.setSpan(
                android.text.style.ForegroundColorSpan(colorInt),
                start,
                end,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        if (searchQuery.isNotEmpty()) {
            val fullText = builder.toString()
            var cursor = 0
            var localMatchIdx = 0
            while (cursor <= fullText.length) {
                val found = fullText.indexOf(searchQuery, cursor, ignoreCase = true)
                if (found == -1) break
                val globalMatchIdx = matchesBeforeParagraph + localMatchIdx
                val bg = if (globalMatchIdx == activeMatchIndex) 0xFFFF9800.toInt() else 0xFFFFE066.toInt()
                builder.setSpan(
                    android.text.style.BackgroundColorSpan(bg),
                    found,
                    found + searchQuery.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.setSpan(
                    android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                    found,
                    found + searchQuery.length,
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                cursor = found + searchQuery.length
                localMatchIdx++
            }
        }

        return builder
    }

    data class MeasuredParagraph(
        val layout: android.text.StaticLayout,
        val spacingBefore: Float,
        val spacingAfter: Float,
        val layoutHeight: Float,
        val totalHeight: Float
    )

    fun measureParagraph(
        para: DocxParagraph,
        widthPoints: Float,
        scaleFactor: Float
    ): MeasuredParagraph {
        val spannable = buildSpannable(para, scaleFactor)
        val textPaint = android.text.TextPaint().apply {
            isAntiAlias = true
        }
        val widthPx = (widthPoints * scaleFactor).toInt().coerceAtLeast(1)
        val builder = android.text.StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, widthPx)
            .setAlignment(getAndroidAlignment(para.alignment))
            .setIncludePad(false)
        
        if (para.alignment == DocxTextAlignment.Justify) {
            builder.setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD)
        }
        
        val layout = builder.build()
        
        val spacingBefore = para.spacingBeforeTwips / 20f
        val spacingAfter = para.spacingAfterTwips / 20f
        val layoutHeight = layout.height / scaleFactor
        
        return MeasuredParagraph(
            layout = layout,
            spacingBefore = spacingBefore,
            spacingAfter = spacingAfter,
            layoutHeight = layoutHeight,
            totalHeight = spacingBefore + layoutHeight + spacingAfter
        )
    }

    fun splitSpans(spans: List<DocxSpan>, splitOffset: Int): Pair<List<DocxSpan>, List<DocxSpan>> {
        val before = mutableListOf<DocxSpan>()
        val after = mutableListOf<DocxSpan>()
        var currentOffset = 0
        for (span in spans) {
            val spanEnd = currentOffset + span.text.length
            if (spanEnd <= splitOffset) {
                before.add(span)
            } else if (currentOffset >= splitOffset) {
                after.add(span)
            } else {
                val splitIdx = splitOffset - currentOffset
                before.add(span.copy(text = span.text.substring(0, splitIdx)))
                after.add(span.copy(text = span.text.substring(splitIdx)))
            }
            currentOffset = spanEnd
        }
        return Pair(before, after)
    }

    data class MeasuredImage(val width: Float, val height: Float)

    fun measureImage(img: DocxImage, maxContentWidth: Float): MeasuredImage {
        val widthPoints = if (img.widthEmu != null && img.widthEmu > 0) img.widthEmu / 12700f else 200f
        val heightPoints = if (img.heightEmu != null && img.heightEmu > 0) img.heightEmu / 12700f else 200f
        
        return if (widthPoints > maxContentWidth) {
            val scale = maxContentWidth / widthPoints
            MeasuredImage(maxContentWidth, heightPoints * scale)
        } else {
            MeasuredImage(widthPoints, heightPoints)
        }
    }

    fun determineTableColumnWidths(table: DocxTable, contentWidth: Float): List<Float> {
        val maxCols = table.rows.maxOfOrNull { it.cells.size } ?: 1
        if (maxCols == 0) return listOf(contentWidth)
        
        val colWidths = MutableList(maxCols) { 0f }
        val cellCount = MutableList(maxCols) { 0 }
        
        for (row in table.rows) {
            row.cells.forEachIndexed { idx, cell ->
                if (idx < maxCols) {
                    val cellW = cell.widthTwips?.let { it / 20f } ?: 0f
                    if (cellW > 0f) {
                        colWidths[idx] += cellW
                        cellCount[idx]++
                    }
                }
            }
        }
        
        val finalWidths = colWidths.mapIndexed { idx, sum ->
            val count = cellCount[idx]
            if (count > 0) sum / count else 0f
        }.toMutableList()
        
        val knownWidth = finalWidths.sum()
        val unknownCount = finalWidths.count { it == 0f }
        if (unknownCount > 0) {
            val remaining = (contentWidth - knownWidth).coerceAtLeast(0f)
            val defaultWidth = remaining / unknownCount
            for (i in 0 until finalWidths.size) {
                if (finalWidths[i] == 0f) {
                    finalWidths[i] = if (defaultWidth > 0f) defaultWidth else 80f
                }
            }
        }
        
        val totalSum = finalWidths.sum()
        if (totalSum > contentWidth && totalSum > 0f) {
            val ratio = contentWidth / totalSum
            for (i in 0 until finalWidths.size) {
                finalWidths[i] *= ratio
            }
        }
        
        return finalWidths
    }

    fun layoutTableRow(
        row: DocxTableRow,
        colWidths: List<Float>,
        scaleFactor: Float
    ): Pair<Float, List<List<PositionedElement>>> {
        val cellLayouts = mutableListOf<List<PositionedElement>>()
        var maxRowHeight = 0f
        
        row.cells.forEachIndexed { cellIdx, cell ->
            val colWidth = colWidths.getOrNull(cellIdx) ?: 80f
            val cellElements = mutableListOf<PositionedElement>()
            var cellY = 0f
            
            for (element in cell.elements) {
                when (element) {
                    is DocxElement.Paragraph -> {
                        val para = element.para
                        val leftIndent = if (para.style == DocxParagraphStyle.ListItem) {
                            (12f + para.listLevel * 12f) + (para.indentStartTwips / 40f)
                        } else {
                            para.indentStartTwips / 20f
                        }
                        val paraWidth = (colWidth - leftIndent).coerceAtLeast(10f)
                        val measured = measureParagraph(para, paraWidth, scaleFactor)
                        
                        cellY += measured.spacingBefore
                        cellElements.add(
                            PositionedElement.Paragraph(
                                para = para,
                                x = leftIndent,
                                y = cellY,
                                width = paraWidth,
                                height = measured.layoutHeight
                            )
                        )
                        cellY += measured.layoutHeight + measured.spacingAfter
                    }
                    is DocxElement.Image -> {
                        val img = element.img
                        val measured = measureImage(img, colWidth)
                        val imageX = (colWidth - measured.width) / 2f
                        cellElements.add(
                            PositionedElement.Image(
                                img = img,
                                x = imageX,
                                y = cellY,
                                width = measured.width,
                                height = measured.height
                            )
                        )
                        cellY += measured.height + 6f
                    }
                    is DocxElement.Table -> {
                        val subColWidths = determineTableColumnWidths(element.table, colWidth)
                        val subCellLayouts = mutableListOf<List<PositionedElement>>()
                        var subTableHeight = 0f
                        val subTableRowHeights = mutableListOf<Float>()
                        
                        element.table.rows.forEach { subRow ->
                            val (subRowH, subCellL) = layoutTableRow(subRow, subColWidths, scaleFactor)
                            subCellLayouts.add(subCellL.flatten())
                            subTableRowHeights.add(subRowH)
                            subTableHeight += subRowH
                        }
                        
                        cellElements.add(
                            PositionedElement.Table(
                                table = element.table,
                                x = 0f,
                                y = cellY,
                                width = colWidth,
                                height = subTableHeight,
                                rowHeights = subTableRowHeights,
                                cellWidths = subColWidths,
                                cellElements = listOf(subCellLayouts)
                            )
                        )
                        cellY += subTableHeight + 6f
                    }
                }
            }
            
            cellLayouts.add(cellElements)
            maxRowHeight = maxOf(maxRowHeight, cellY)
        }
        
        return Pair(maxRowHeight.coerceAtLeast(20f), cellLayouts)
    }
}
