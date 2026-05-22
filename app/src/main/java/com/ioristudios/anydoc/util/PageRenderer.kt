package com.ioristudios.anydoc.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ioristudios.anydoc.model.*

object PageRenderer {

    fun render(
        page: LayoutPage,
        filePath: String,
        searchQuery: String = "",
        activeMatchIndex: Int = -1,
        matchesBeforePage: Int = 0,
        hideParagraphText: Boolean = false
    ): Bitmap {
        val config = page.config
        val scale = config.scaleFactor
        val widthPx = (config.pageWidthPoints * scale).toInt().coerceAtLeast(1)
        val heightPx = (config.pageHeightPoints * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw white background
        canvas.drawColor(Color.WHITE)

        var currentMatchesCount = matchesBeforePage

        canvas.save()
        // Offset content area by margins
        canvas.translate(config.marginLeftPoints * scale, config.marginTopPoints * scale)

        for (element in page.elements) {
            renderElement(canvas, element, filePath, scale, searchQuery, activeMatchIndex, currentMatchesCount, hideParagraphText)
            currentMatchesCount += countMatchesInElement(element, searchQuery)
        }

        canvas.restore()
        return bitmap
    }

    private fun renderElement(
        canvas: Canvas,
        element: PositionedElement,
        filePath: String,
        scale: Float,
        searchQuery: String,
        activeMatchIndex: Int,
        matchesBeforeElement: Int,
        hideParagraphText: Boolean
    ) {
        when (element) {
            is PositionedElement.Paragraph -> {
                renderParagraph(canvas, element, scale, searchQuery, activeMatchIndex, matchesBeforeElement, hideParagraphText)
            }
            is PositionedElement.Image -> {
                renderImage(canvas, element, filePath, scale)
            }
            is PositionedElement.Table -> {
                renderTable(canvas, element, filePath, scale, searchQuery, activeMatchIndex, matchesBeforeElement, hideParagraphText)
            }
        }
    }

    private fun renderParagraph(
        canvas: Canvas,
        element: PositionedElement.Paragraph,
        scale: Float,
        searchQuery: String,
        activeMatchIndex: Int,
        matchesBeforeElement: Int,
        hideParagraphText: Boolean
    ) {
        val para = element.para
        val spannable = LayoutMetrics.buildSpannable(para, scale, searchQuery, activeMatchIndex, matchesBeforeElement)
        val textPaint = TextPaint().apply {
            isAntiAlias = true
        }
        val widthPx = (element.width * scale).toInt().coerceAtLeast(1)
        val builder = StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, widthPx)
            .setAlignment(LayoutMetrics.getAndroidAlignment(para.alignment))
            .setIncludePad(false)

        if (para.alignment == DocxTextAlignment.Justify) {
            builder.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
        }
        val layout = builder.build()

        canvas.save()
        canvas.translate(element.x * scale, element.y * scale)

        // Draw list item bullet/number if style is ListItem
        if (para.style == DocxParagraphStyle.ListItem) {
            val bulletText = if (para.isNumbered) "1. " else "\u2022 "
            val bulletPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = (para.spans.firstOrNull()?.fontSize ?: 14f) * scale
                color = Color.BLACK
                typeface = LayoutMetrics.getAndroidTypeface(
                    para.spans.firstOrNull()?.fontFamily,
                    para.spans.firstOrNull()?.bold ?: false,
                    para.spans.firstOrNull()?.italic ?: false
                )
            }
            canvas.drawText(bulletText, -14f * scale, layout.getLineBaseline(0).toFloat(), bulletPaint)
        }

        if (!hideParagraphText) {
            layout.draw(canvas)
        }
        canvas.restore()
    }

    private fun renderImage(
        canvas: Canvas,
        element: PositionedElement.Image,
        filePath: String,
        scale: Float
    ) {
        val img = element.img
        var bitmap: Bitmap? = null
        try {
            if (img.bytes != null) {
                bitmap = android.graphics.BitmapFactory.decodeByteArray(img.bytes, 0, img.bytes.size)
            } else {
                java.util.zip.ZipFile(filePath).use { zip ->
                    val entry = zip.getEntry(img.entryName)
                    if (entry != null) {
                        zip.getInputStream(entry).use { stream ->
                            val bytes = stream.readBytes()
                            bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val destRect = RectF(
            element.x * scale,
            element.y * scale,
            (element.x + element.width) * scale,
            (element.y + element.height) * scale
        )

        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
            bitmap!!.recycle()
        } else {
            val paint = Paint().apply {
                color = Color.LTGRAY
                style = Paint.Style.FILL
            }
            canvas.drawRect(destRect, paint)
            val borderPaint = Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f * scale
            }
            canvas.drawRect(destRect, borderPaint)
        }
    }

    private fun renderTable(
        canvas: Canvas,
        element: PositionedElement.Table,
        filePath: String,
        scale: Float,
        searchQuery: String,
        activeMatchIndex: Int,
        matchesBeforeTable: Int,
        hideParagraphText: Boolean
    ) {
        canvas.save()
        canvas.translate(element.x * scale, element.y * scale)

        val cellWidths = element.cellWidths
        val rowHeights = element.rowHeights

        val borderPaint = Paint().apply {
            color = 0xFFD2D0CE.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.8f * scale
        }

        var currentY = 0f
        var currentMatchesCount = matchesBeforeTable

        element.table.rows.forEachIndexed { rowIndex, row ->
            val rowHeight = rowHeights.getOrNull(rowIndex) ?: 20f
            var currentX = 0f

            row.cells.forEachIndexed { cellIndex, _ ->
                val cellWidth = cellWidths.getOrNull(cellIndex) ?: 80f
                val cellRect = RectF(
                    currentX * scale,
                    currentY * scale,
                    (currentX + cellWidth) * scale,
                    (currentY + rowHeight) * scale
                )

                if (rowIndex == 0) {
                    val bgPaint = Paint().apply {
                        color = 0xFFFAFAFA.toInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(cellRect, bgPaint)
                }

                canvas.drawRect(cellRect, borderPaint)

                val cellContent = element.cellElements.getOrNull(rowIndex)?.getOrNull(cellIndex) ?: emptyList()
                canvas.save()
                canvas.clipRect(cellRect)
                canvas.translate(currentX * scale, currentY * scale)

                for (subElement in cellContent) {
                    renderElement(canvas, subElement, filePath, scale, searchQuery, activeMatchIndex, currentMatchesCount, hideParagraphText)
                    currentMatchesCount += countMatchesInElement(subElement, searchQuery)
                }

                canvas.restore()
                currentX += cellWidth
            }
            currentY += rowHeight
        }

        canvas.restore()
    }

    private fun countMatchesInElement(element: PositionedElement, query: String): Int {
        if (query.isBlank()) return 0
        return when (element) {
            is PositionedElement.Paragraph -> countOccurrences(element.para.spans.joinToString("") { it.text }, query)
            is PositionedElement.Table -> {
                var sum = 0
                element.cellElements.forEach { row ->
                    row.forEach { cell ->
                        cell.forEach { subEl ->
                            sum += countMatchesInElement(subEl, query)
                        }
                    }
                }
                sum
            }
            is PositionedElement.Image -> 0
        }
    }

    private fun countOccurrences(text: String, query: String): Int {
        var count = 0
        var start = 0
        while (true) {
            val i = text.indexOf(query, start, ignoreCase = true)
            if (i < 0) break
            count++
            start = i + query.length
        }
        return count
    }
}
