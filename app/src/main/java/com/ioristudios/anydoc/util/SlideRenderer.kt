package com.ioristudios.anydoc.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import com.ioristudios.anydoc.model.*

object SlideRenderer {

    fun render(
        slide: SlideModel,
        slideWidth: Float,
        slideHeight: Float,
        targetWidthPx: Int
    ): Bitmap {
        val targetHeightPx = (targetWidthPx * slideHeight / slideWidth).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scale = targetWidthPx / slideWidth

        // 1. Draw background
        drawBackground(canvas, slide.background, targetWidthPx.toFloat(), targetHeightPx.toFloat(), scale)

        // 2. Draw elements in z-order
        slide.elements.forEach { element ->
            renderElement(canvas, element, scale)
        }

        return bitmap
    }

    private fun drawBackground(
        canvas: Canvas,
        background: SlideBackground,
        width: Float,
        height: Float,
        @Suppress("UNUSED_PARAMETER") scale: Float
    ) {
        val bounds = RectF(0f, 0f, width, height)
        when (background) {
            is SlideBackground.SolidColor -> {
                canvas.drawColor(background.color.toInt())
            }
            is SlideBackground.GradientFill -> {
                val paint = Paint().apply {
                    style = Paint.Style.FILL
                    val stops = background.stops.sortedBy { it.position }
                    val colors = stops.map { it.color.toInt() }.toIntArray()
                    val positions = stops.map { it.position }.toFloatArray()

                    val angleRad = Math.toRadians(background.angle.toDouble())
                    val dx = Math.cos(angleRad).toFloat()
                    val dy = Math.sin(angleRad).toFloat()

                    val cx = bounds.centerX()
                    val cy = bounds.centerY()

                    val halfW = bounds.width() / 2f
                    val halfH = bounds.height() / 2f
                    val extent = Math.abs(halfW * dx) + Math.abs(halfH * dy)

                    val x0 = cx - extent * dx
                    val y0 = cy - extent * dy
                    val x1 = cx + extent * dx
                    val y1 = cy + extent * dy

                    shader = LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
                }
                canvas.drawRect(bounds, paint)
            }
            is SlideBackground.ImageFill -> {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(background.bytes, 0, background.bytes.size)
                    if (bitmap != null) {
                        canvas.drawBitmap(bitmap, null, bounds, Paint(Paint.FILTER_BITMAP_FLAG))
                        bitmap.recycle()
                    } else {
                        canvas.drawColor(Color.WHITE)
                    }
                } catch (e: Exception) {
                    canvas.drawColor(Color.WHITE)
                }
            }
            is SlideBackground.ThemeBackground -> {
                // Fallback theme color parsing (simplifies to solid/gradient or generic gray/white)
                if (background.schemeColor.contains("dark", ignoreCase = true)) {
                    canvas.drawColor(0xFF1E1E1E.toInt())
                } else {
                    canvas.drawColor(Color.WHITE)
                }
            }
            is SlideBackground.None -> {
                canvas.drawColor(Color.WHITE)
            }
        }
    }

    private fun renderElement(canvas: Canvas, element: SlideElement, scale: Float) {
        when (element) {
            is SlideElement.TextBox -> {
                canvas.save()
                val bounds = RectF(
                    element.bounds.left * scale,
                    element.bounds.top * scale,
                    (element.bounds.left + element.bounds.width) * scale,
                    (element.bounds.top + element.bounds.height) * scale
                )
                val centerX = bounds.centerX()
                val centerY = bounds.centerY()
                canvas.rotate(element.rotation, centerX, centerY)

                // Fill TextBox background if any
                if (element.fill != null) {
                    val fillPaint = getPaintForFill(element.fill, bounds, scale)
                    if (fillPaint != null) {
                        canvas.drawRect(bounds, fillPaint)
                    }
                }

                // Border TextBox if any
                if (element.border != null) {
                    canvas.drawRect(bounds, getPaintForBorder(element.border, scale))
                }

                // Inner text flow
                drawTextFlow(canvas, element.paragraphs, bounds, element.verticalAnchor, scale)

                canvas.restore()
            }

            is SlideElement.ShapeBox -> {
                canvas.save()
                val bounds = RectF(
                    element.bounds.left * scale,
                    element.bounds.top * scale,
                    (element.bounds.left + element.bounds.width) * scale,
                    (element.bounds.top + element.bounds.height) * scale
                )
                val centerX = bounds.centerX()
                val centerY = bounds.centerY()
                canvas.rotate(element.rotation, centerX, centerY)

                val path = ShapeDrawer.getPath(element.shapeType, bounds, scale)

                // Fill Shape
                if (element.fill != null) {
                    val fillPaint = getPaintForFill(element.fill, bounds, scale)
                    if (fillPaint != null) {
                        canvas.drawPath(path, fillPaint)
                    }
                }

                // Border Shape
                if (element.border != null) {
                    canvas.drawPath(path, getPaintForBorder(element.border, scale))
                }

                // Text Overlay
                if (element.paragraphs.isNotEmpty()) {
                    drawTextFlow(canvas, element.paragraphs, bounds, VerticalAnchor.MIDDLE, scale)
                }

                canvas.restore()
            }

            is SlideElement.ImageBox -> {
                canvas.save()
                val bounds = RectF(
                    element.bounds.left * scale,
                    element.bounds.top * scale,
                    (element.bounds.left + element.bounds.width) * scale,
                    (element.bounds.top + element.bounds.height) * scale
                )
                val centerX = bounds.centerX()
                val centerY = bounds.centerY()
                canvas.rotate(element.rotation, centerX, centerY)

                try {
                    val bitmap = BitmapFactory.decodeByteArray(element.bytes, 0, element.bytes.size)
                    if (bitmap != null) {
                        if (element.cropFraction != null) {
                            val crop = element.cropFraction
                            val srcRect = Rect(
                                (crop.left * bitmap.width).toInt(),
                                (crop.top * bitmap.height).toInt(),
                                (crop.right * bitmap.width).toInt(),
                                (crop.bottom * bitmap.height).toInt()
                            )
                            canvas.drawBitmap(bitmap, srcRect, bounds, Paint(Paint.FILTER_BITMAP_FLAG))
                        } else {
                            canvas.drawBitmap(bitmap, null, bounds, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                        bitmap.recycle()
                    } else {
                        drawPlaceholder(canvas, bounds, scale)
                    }
                } catch (e: Exception) {
                    drawPlaceholder(canvas, bounds, scale)
                }

                canvas.restore()
            }

            is SlideElement.TableBox -> {
                canvas.save()
                val bounds = element.bounds
                canvas.translate(bounds.left * scale, bounds.top * scale)

                var currentY = 0f
                element.rows.forEachIndexed { rowIndex, row ->
                    val rowHeight = element.rowHeights.getOrNull(rowIndex) ?: 20f
                    var currentX = 0f

                    row.cells.forEachIndexed { cellIndex, cell ->
                        val colWidth = element.colWidths.getOrNull(cellIndex) ?: 80f
                        val cellRect = RectF(
                            currentX * scale,
                            currentY * scale,
                            (currentX + colWidth) * scale,
                            (currentY + rowHeight) * scale
                        )

                        // Cell Fill
                        if (cell.fill != null) {
                            val fillPaint = getPaintForFill(cell.fill, cellRect, scale)
                            if (fillPaint != null) {
                                canvas.drawRect(cellRect, fillPaint)
                            }
                        }

                        // Cell Borders
                        cell.borderTop?.let {
                            canvas.drawLine(cellRect.left, cellRect.top, cellRect.right, cellRect.top, getPaintForBorder(it, scale))
                        }
                        cell.borderBottom?.let {
                            canvas.drawLine(cellRect.left, cellRect.bottom, cellRect.right, cellRect.bottom, getPaintForBorder(it, scale))
                        }
                        cell.borderLeft?.let {
                            canvas.drawLine(cellRect.left, cellRect.top, cellRect.left, cellRect.bottom, getPaintForBorder(it, scale))
                        }
                        cell.borderRight?.let {
                            canvas.drawLine(cellRect.right, cellRect.top, cellRect.right, cellRect.bottom, getPaintForBorder(it, scale))
                        }

                        // Cell text content
                        if (cell.paragraphs.isNotEmpty()) {
                            canvas.save()
                            canvas.clipRect(cellRect)
                            val padding = 4f * scale
                            val innerRect = RectF(
                                cellRect.left + padding,
                                cellRect.top + padding,
                                cellRect.right - padding,
                                cellRect.bottom - padding
                            )
                            drawTextFlow(canvas, cell.paragraphs, innerRect, cell.verticalAnchor, scale)
                            canvas.restore()
                        }

                        currentX += colWidth
                    }
                    currentY += rowHeight
                }
                canvas.restore()
            }

            is SlideElement.GroupBox -> {
                canvas.save()
                val bounds = element.bounds
                val centerX = bounds.left * scale + (bounds.width * scale) / 2f
                val centerY = bounds.top * scale + (bounds.height * scale) / 2f
                canvas.rotate(element.rotation, centerX, centerY)

                element.elements.forEach { subEl ->
                    renderElement(canvas, subEl, scale)
                }
                canvas.restore()
            }
        }
    }

    private fun drawPlaceholder(canvas: Canvas, bounds: RectF, scale: Float) {
        val paint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }
        canvas.drawRect(bounds, paint)
        val borderPaint = Paint().apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f * scale
        }
        canvas.drawRect(bounds, borderPaint)
    }

    private fun drawTextFlow(
        canvas: Canvas,
        paragraphs: List<SlideParagraph>,
        bounds: RectF,
        verticalAnchor: VerticalAnchor,
        scale: Float
    ) {
        if (paragraphs.isEmpty()) return

        val textPaint = TextPaint().apply {
            isAntiAlias = true
        }

        // Measure and build layouts for all paragraphs
        val layouts = paragraphs.map { para ->
            val scaleShift = para.indentLevel * 20f * scale
            val bulletExtraShift = if (para.bulletChar != null) 16f * scale else 0f
            val startX = scaleShift + bulletExtraShift
            val layoutWidthPx = (bounds.width() - startX).toInt().coerceAtLeast(1)

            val spannable = buildSpannable(para, scale)
            val builder = StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, layoutWidthPx)
                .setAlignment(getAndroidAlignment(para.alignment))
                .setIncludePad(false)
                .setLineSpacing(0f, para.lineSpacing)
            
            if (para.alignment == SlideTextAlign.JUSTIFY) {
                builder.setJustificationMode(android.graphics.text.LineBreaker.JUSTIFICATION_MODE_INTER_WORD)
            }
            builder.build()
        }

        // Total text block height
        val totalTextHeight = layouts.indices.sumOf { idx ->
            val para = paragraphs[idx]
            val layout = layouts[idx]
            val before = para.spaceBefore * scale
            val after = para.spaceAfter * scale
            (before + layout.height + after).toDouble()
        }.toFloat()

        // Vertical translation alignment shift
        val anchorShift = when (verticalAnchor) {
            VerticalAnchor.TOP -> 0f
            VerticalAnchor.MIDDLE -> ((bounds.height() - totalTextHeight) / 2f).coerceAtLeast(0f)
            VerticalAnchor.BOTTOM -> (bounds.height() - totalTextHeight).coerceAtLeast(0f)
        }

        canvas.save()
        canvas.translate(bounds.left, bounds.top + anchorShift)

        var currentParaY = 0f
        paragraphs.forEachIndexed { idx, para ->
            val layout = layouts[idx]
            val before = para.spaceBefore * scale
            val after = para.spaceAfter * scale
            
            val indentShift = para.indentLevel * 20f * scale
            val bulletExtraShift = if (para.bulletChar != null) 16f * scale else 0f
            
            canvas.save()
            canvas.translate(indentShift + bulletExtraShift, currentParaY + before)

            // Draw bullet
            if (para.bulletChar != null) {
                val bulletPaint = TextPaint().apply {
                    isAntiAlias = true
                    textSize = (para.bulletSize ?: para.runs.firstOrNull()?.fontSize ?: 14f) * scale
                    color = (para.bulletColor ?: para.runs.firstOrNull()?.color ?: 0xFF000000.toLong()).toInt()
                    val firstRun = para.runs.firstOrNull()
                    val typefaceStyle = when {
                        firstRun?.bold == true && firstRun.italic -> Typeface.BOLD_ITALIC
                        firstRun?.bold == true -> Typeface.BOLD
                        firstRun?.italic == true -> Typeface.ITALIC
                        else -> Typeface.NORMAL
                    }
                    val baseFamily = when (firstRun?.fontFamily?.lowercase()) {
                        "times new roman", "times", "serif", "georgia" -> Typeface.SERIF
                        "courier new", "courier", "monospace", "consolas", "lucida console" -> Typeface.MONOSPACE
                        else -> Typeface.SANS_SERIF
                    }
                    typeface = Typeface.create(baseFamily, typefaceStyle)
                }
                
                // Bullet centered horizontally in margin space
                canvas.drawText(para.bulletChar, -bulletExtraShift * 0.8f, layout.getLineBaseline(0).toFloat(), bulletPaint)
            }

            layout.draw(canvas)
            canvas.restore()

            currentParaY += before + layout.height + after
        }

        canvas.restore()
    }

    private fun buildSpannable(paragraph: SlideParagraph, scale: Float): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        paragraph.runs.forEach { run ->
            val start = builder.length
            builder.append(run.text)
            val end = builder.length

            val typefaceStyle = when {
                run.bold && run.italic -> Typeface.BOLD_ITALIC
                run.bold -> Typeface.BOLD
                run.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            if (typefaceStyle != Typeface.NORMAL) {
                builder.setSpan(
                    android.text.style.StyleSpan(typefaceStyle),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val baseFamily = when (run.fontFamily?.lowercase()) {
                "times new roman", "times", "serif", "georgia" -> Typeface.SERIF
                "courier new", "courier", "monospace", "consolas", "lucida console" -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
            val typeface = Typeface.create(baseFamily, typefaceStyle)
            builder.setSpan(
                android.text.style.TypefaceSpan(typeface),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            if (run.underline) {
                builder.setSpan(
                    android.text.style.UnderlineSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            if (run.strikethrough) {
                builder.setSpan(
                    android.text.style.StrikethroughSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            builder.setSpan(
                android.text.style.AbsoluteSizeSpan((run.fontSize * scale).toInt().coerceAtLeast(1)),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            builder.setSpan(
                android.text.style.ForegroundColorSpan(run.color.toInt()),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            when (run.baseline) {
                BaselineShift.SUPERSCRIPT -> {
                    builder.setSpan(
                        android.text.style.SuperscriptSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                BaselineShift.SUBSCRIPT -> {
                    builder.setSpan(
                        android.text.style.SubscriptSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                else -> {}
            }
        }
        return builder
    }

    private fun getAndroidAlignment(alignment: SlideTextAlign): Layout.Alignment {
        return when (alignment) {
            SlideTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
            SlideTextAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
            SlideTextAlign.START, SlideTextAlign.JUSTIFY -> Layout.Alignment.ALIGN_NORMAL
        }
    }

    private fun getPaintForFill(fill: SlideFill, bounds: RectF, @Suppress("UNUSED_PARAMETER") scale: Float): Paint? {
        return when (fill) {
            is SlideFill.Solid -> Paint().apply {
                color = fill.color.toInt()
                style = Paint.Style.FILL
            }
            is SlideFill.Gradient -> Paint().apply {
                style = Paint.Style.FILL
                val stops = fill.stops.sortedBy { it.position }
                val colors = stops.map { it.color.toInt() }.toIntArray()
                val positions = stops.map { it.position }.toFloatArray()

                val angleRad = Math.toRadians(fill.angle.toDouble())
                val dx = Math.cos(angleRad).toFloat()
                val dy = Math.sin(angleRad).toFloat()

                val cx = bounds.centerX()
                val cy = bounds.centerY()

                val halfW = bounds.width() / 2f
                val halfH = bounds.height() / 2f
                val extent = Math.abs(halfW * dx) + Math.abs(halfH * dy)

                val x0 = cx - extent * dx
                val y0 = cy - extent * dy
                val x1 = cx + extent * dx
                val y1 = cy + extent * dy

                shader = LinearGradient(x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP)
            }
            is SlideFill.Image -> {
                try {
                    val bitmap = BitmapFactory.decodeByteArray(fill.bytes, 0, fill.bytes.size)
                    if (bitmap != null) {
                        Paint().apply {
                            style = Paint.Style.FILL
                            shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                                val matrix = Matrix()
                                if (fill.stretch) {
                                    val scaleX = bounds.width() / bitmap.width
                                    val scaleY = bounds.height() / bitmap.height
                                    matrix.postScale(scaleX, scaleY)
                                }
                                matrix.postTranslate(bounds.left, bounds.top)
                                setLocalMatrix(matrix)
                            }
                        }
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            is SlideFill.None -> null
        }
    }

    private fun getPaintForBorder(border: SlideBorder, scale: Float): Paint {
        return Paint().apply {
            color = border.color.toInt()
            style = Paint.Style.STROKE
            strokeWidth = border.widthPt * scale
            isAntiAlias = true
            when (border.dashStyle) {
                DashStyle.DASH -> {
                    pathEffect = DashPathEffect(floatArrayOf(10f * scale, 5f * scale), 0f)
                }
                DashStyle.DOT -> {
                    pathEffect = DashPathEffect(floatArrayOf(2f * scale, 2f * scale), 0f)
                }
                DashStyle.DASH_DOT -> {
                    pathEffect = DashPathEffect(floatArrayOf(10f * scale, 5f * scale, 2f * scale, 5f * scale), 0f)
                }
                DashStyle.SOLID -> {}
            }
        }
    }

    private object ShapeDrawer {
        fun getPath(shapeType: String, bounds: RectF, scale: Float): Path {
            val path = Path()
            when (shapeType.lowercase()) {
                "rect", "rectangle" -> {
                    path.addRect(bounds, Path.Direction.CW)
                }
                "roundrect", "roundrectangle" -> {
                    val radius = (bounds.width().coerceAtMost(bounds.height()) * 0.1f).coerceAtLeast(4f * scale)
                    path.addRoundRect(bounds, radius, radius, Path.Direction.CW)
                }
                "ellipse", "oval" -> {
                    path.addOval(bounds, Path.Direction.CW)
                }
                "triangle" -> {
                    path.moveTo(bounds.left + bounds.width() / 2f, bounds.top)
                    path.lineTo(bounds.right, bounds.bottom)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.close()
                }
                "righttriangle" -> {
                    path.moveTo(bounds.left, bounds.top)
                    path.lineTo(bounds.right, bounds.bottom)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.close()
                }
                "diamond" -> {
                    path.moveTo(bounds.left + bounds.width() / 2f, bounds.top)
                    path.lineTo(bounds.right, bounds.top + bounds.height() / 2f)
                    path.lineTo(bounds.left + bounds.width() / 2f, bounds.bottom)
                    path.lineTo(bounds.left, bounds.top + bounds.height() / 2f)
                    path.close()
                }
                "parallelogram" -> {
                    val shift = bounds.width() * 0.2f
                    path.moveTo(bounds.left + shift, bounds.top)
                    path.lineTo(bounds.right, bounds.top)
                    path.lineTo(bounds.right - shift, bounds.bottom)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.close()
                }
                "trapezoid" -> {
                    val shift = bounds.width() * 0.15f
                    path.moveTo(bounds.left + shift, bounds.top)
                    path.lineTo(bounds.right - shift, bounds.top)
                    path.lineTo(bounds.right, bounds.bottom)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.close()
                }
                "pentagon" -> {
                    path.moveTo(bounds.left + bounds.width() / 2f, bounds.top)
                    path.lineTo(bounds.right, bounds.top + bounds.height() * 0.38f)
                    path.lineTo(bounds.left + bounds.width() * 0.81f, bounds.bottom)
                    path.lineTo(bounds.left + bounds.width() * 0.19f, bounds.bottom)
                    path.lineTo(bounds.left, bounds.top + bounds.height() * 0.38f)
                    path.close()
                }
                "hexagon" -> {
                    path.moveTo(bounds.left + bounds.width() * 0.25f, bounds.top)
                    path.lineTo(bounds.left + bounds.width() * 0.75f, bounds.top)
                    path.lineTo(bounds.right, bounds.top + bounds.height() / 2f)
                    path.lineTo(bounds.left + bounds.width() * 0.75f, bounds.bottom)
                    path.lineTo(bounds.left + bounds.width() * 0.25f, bounds.bottom)
                    path.lineTo(bounds.left, bounds.top + bounds.height() / 2f)
                    path.close()
                }
                "chevron" -> {
                    path.moveTo(bounds.left, bounds.top)
                    path.lineTo(bounds.right - bounds.width() * 0.25f, bounds.top)
                    path.lineTo(bounds.right, bounds.top + bounds.height() / 2f)
                    path.lineTo(bounds.right - bounds.width() * 0.25f, bounds.bottom)
                    path.lineTo(bounds.left, bounds.bottom)
                    path.lineTo(bounds.left + bounds.width() * 0.25f, bounds.top + bounds.height() / 2f)
                    path.close()
                }
                "star5", "star" -> {
                    val cx = bounds.centerX()
                    val cy = bounds.centerY()
                    val rx = bounds.width() / 2f
                    val ry = bounds.height() / 2f
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) 1f else 0.4f
                        val angle = i * Math.PI / 5 - Math.PI / 2
                        val x = cx + rx * r * Math.cos(angle).toFloat()
                        val y = cy + ry * r * Math.sin(angle).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()
                }
                else -> {
                    // Default to simple Rectangle shape
                    path.addRect(bounds, Path.Direction.CW)
                }
            }
            return path
        }
    }
}
