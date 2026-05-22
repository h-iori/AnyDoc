package com.ioristudios.anydoc.util

import android.graphics.RectF
import com.ioristudios.anydoc.model.*
import org.apache.poi.sl.usermodel.*
import java.io.File

object PptxParser {

    fun parsePptx(path: String): PresentationContent {
        return parse(path, isPptx = true)
    }

    fun parsePpt(path: String): PresentationContent {
        return parse(path, isPptx = false)
    }

    private fun parse(path: String, isPptx: Boolean): PresentationContent {
        try {
            val file = File(path)
            val rawSlideshow = if (isPptx) {
                val pkg = org.apache.poi.openxml4j.opc.OPCPackage.open(file)
                try {
                    org.apache.poi.xslf.usermodel.XMLSlideShow(pkg)
                } catch (t: Throwable) {
                    runCatching { pkg.close() }
                    throw t
                }
            } else {
                val fs = org.apache.poi.poifs.filesystem.POIFSFileSystem(file)
                try {
                    org.apache.poi.hslf.usermodel.HSLFSlideShow(fs)
                } catch (t: Throwable) {
                    runCatching { fs.close() }
                    throw t
                }
            }
            rawSlideshow.use { slideshow ->
                val slideWidth = runCatching {
                    val pageSize = slideshow.javaClass.getMethod("getPageSize").invoke(slideshow)
                    (pageSize.javaClass.getMethod("getWidth").invoke(pageSize) as Number).toFloat()
                }.getOrDefault(720f)

                val slideHeight = runCatching {
                    val pageSize = slideshow.javaClass.getMethod("getPageSize").invoke(slideshow)
                    (pageSize.javaClass.getMethod("getHeight").invoke(pageSize) as Number).toFloat()
                }.getOrDefault(540f)

                val slideModels = slideshow.slides.mapIndexed { index, slide ->
                    val elements = mutableListOf<SlideElement>()
                    for (shape in slide.shapes) {
                        mapShape(shape)?.let { elements.add(it) }
                    }

                    val background = runCatching { mapPaintStyleToBackground(slide.background?.fillStyle?.paint) }.getOrDefault(SlideBackground.None)
                    val transition = getSlideTransition(slide)
                    val notes = getSlideNotes(slide)

                    SlideModel(
                        index = index,
                        elements = elements,
                        background = background,
                        transition = transition,
                        notes = notes
                    )
                }

                return PresentationContent(
                    slides = slideModels,
                    slideWidth = slideWidth,
                    slideHeight = slideHeight
                )
            }
        } catch (e: Throwable) {
            android.util.Log.e("PptxParser", "Failed to parse PPTX/PPT", e)
            throw e
        }
    }

    private fun mapRect(rect2d: Any?): SlideRect {
        if (rect2d == null) return SlideRect(0f, 0f, 0f, 0f)
        return try {
            val x = (rect2d.javaClass.getMethod("getX").invoke(rect2d) as Number).toFloat()
            val y = (rect2d.javaClass.getMethod("getY").invoke(rect2d) as Number).toFloat()
            val w = (rect2d.javaClass.getMethod("getWidth").invoke(rect2d) as Number).toFloat()
            val h = (rect2d.javaClass.getMethod("getHeight").invoke(rect2d) as Number).toFloat()
            SlideRect(x, y, w, h)
        } catch (e: Exception) {
            SlideRect(0f, 0f, 0f, 0f)
        }
    }

    private fun convertColor(colorStyle: ColorStyle?, defaultArgb: Long = 0xFF000000): Long {
        if (colorStyle == null) return defaultArgb
        return runCatching {
            val colorObj = colorStyle.javaClass.getMethod("getColor").invoke(colorStyle)
            val rgb = (colorObj.javaClass.getMethod("getRGB").invoke(colorObj) as Number).toInt()
            rgb.toLong() and 0xFFFFFFFFL
        }.getOrDefault(defaultArgb)
    }

    private fun convertPaintToColor(paintStyle: PaintStyle?, defaultColor: Long = 0xFF000000): Long {
        if (paintStyle == null) return defaultColor
        return runCatching {
            if (paintStyle is PaintStyle.SolidPaint) {
                convertColor(paintStyle.solidColor, defaultColor)
            } else {
                defaultColor
            }
        }.getOrDefault(defaultColor)
    }

    private fun mapPaintStyle(paintStyle: PaintStyle?): SlideFill {
        if (paintStyle == null) return SlideFill.None
        return try {
            when (paintStyle) {
                is PaintStyle.SolidPaint -> {
                    val color = convertColor(paintStyle.solidColor)
                    SlideFill.Solid(color)
                }
                is PaintStyle.GradientPaint -> {
                    val angle = paintStyle.gradientAngle.toFloat()
                    val stops = paintStyle.gradientColors.mapIndexed { idx, colorStyle ->
                        val color = convertColor(colorStyle, 0xFFFFFFFFL)
                        val pos = paintStyle.gradientFractions?.getOrNull(idx) ?: 0f
                        GradientStop(color, pos)
                    }
                    SlideFill.Gradient(stops, angle)
                }
                is PaintStyle.TexturePaint -> {
                    try {
                        val stream = paintStyle.getImageData()
                        val bytes = stream?.readBytes()
                        if (bytes != null) {
                            SlideFill.Image(bytes)
                        } else {
                            SlideFill.None
                        }
                    } catch (e: Exception) {
                        SlideFill.None
                    }
                }
                else -> SlideFill.None
            }
        } catch (t: Throwable) {
            SlideFill.None
        }
    }

    private fun mapPaintStyleToBackground(paintStyle: PaintStyle?): SlideBackground {
        if (paintStyle == null) return SlideBackground.None
        return try {
            when (paintStyle) {
                is PaintStyle.SolidPaint -> {
                    val color = convertColor(paintStyle.solidColor)
                    SlideBackground.SolidColor(color)
                }
                is PaintStyle.GradientPaint -> {
                    val angle = paintStyle.gradientAngle.toFloat()
                    val stops = paintStyle.gradientColors.mapIndexed { idx, colorStyle ->
                        val color = convertColor(colorStyle, 0xFFFFFFFFL)
                        val pos = paintStyle.gradientFractions?.getOrNull(idx) ?: 0f
                        GradientStop(color, pos)
                    }
                    SlideBackground.GradientFill(stops, angle)
                }
                is PaintStyle.TexturePaint -> {
                    try {
                        val stream = paintStyle.getImageData()
                        val bytes = stream?.readBytes()
                        if (bytes != null) {
                            SlideBackground.ImageFill(bytes)
                        } else {
                            SlideBackground.None
                        }
                    } catch (e: Exception) {
                        SlideBackground.None
                    }
                }
                else -> SlideBackground.None
            }
        } catch (t: Throwable) {
            SlideBackground.None
        }
    }

    private fun mapBorder(strokeStyle: StrokeStyle?): SlideBorder? {
        if (strokeStyle == null) return null
        return try {
            val paint = strokeStyle.paint
            if (paint == null || paint !is PaintStyle.SolidPaint) return null
            val color = convertColor(paint.solidColor)
            val widthPt = strokeStyle.lineWidth.toFloat()
            val dashStyle = when (strokeStyle.lineDash) {
                StrokeStyle.LineDash.DASH -> DashStyle.DASH
                StrokeStyle.LineDash.DOT -> DashStyle.DOT
                StrokeStyle.LineDash.DASH_DOT -> DashStyle.DASH_DOT
                else -> DashStyle.SOLID
            }
            SlideBorder(color, widthPt, dashStyle)
        } catch (t: Throwable) {
            null
        }
    }

    private fun getCellBorder(cell: TableCell<*, *>, edge: TableCell.BorderEdge): SlideBorder? {
        return try {
            val stroke = cell.getBorderStyle(edge) ?: return null
            val paint = stroke.paint
            if (paint == null || paint !is PaintStyle.SolidPaint) return null
            val color = convertColor(paint.solidColor)
            val width = stroke.lineWidth.toFloat()
            if (width <= 0f) return null
            val lineDash = stroke.lineDash
            val dashStyle = when (lineDash) {
                StrokeStyle.LineDash.DASH -> DashStyle.DASH
                StrokeStyle.LineDash.DOT -> DashStyle.DOT
                StrokeStyle.LineDash.DASH_DOT -> DashStyle.DASH_DOT
                else -> DashStyle.SOLID
            }
            SlideBorder(color, width, dashStyle)
        } catch (t: Throwable) {
            null
        }
    }

    private fun mapShape(shape: Shape<*, *>) : SlideElement? {
        val anchor = runCatching { shape.javaClass.getMethod("getAnchor").invoke(shape) }.getOrNull()
        val bounds = mapRect(anchor)
        val rotation = if (shape is PlaceableShape<*, *>) {
            runCatching { shape.rotation.toFloat() }.getOrDefault(0f)
        } else 0f

        return try {
            when (shape) {
                is GroupShape<*, *> -> {
                    val subElements = mutableListOf<SlideElement>()
                    for (subShape in shape.shapes) {
                        mapShape(subShape)?.let { subElements.add(it) }
                    }
                    SlideElement.GroupBox(bounds, subElements, rotation)
                }
                is TableShape<*, *> -> {
                    val numRows = shape.numberOfRows
                    val numCols = shape.numberOfColumns
                    val colWidths = (0 until numCols).map { shape.getColumnWidth(it).toFloat() }
                    val rowHeights = (0 until numRows).map { shape.getRowHeight(it).toFloat() }

                    val rows = mutableListOf<SlideTableRow>()
                    for (r in 0 until numRows) {
                        val cells = mutableListOf<SlideTableCell>()
                        for (c in 0 until numCols) {
                            val cell = shape.getCell(r, c)
                            if (cell != null) {
                                val cellParas = mapTextParagraphs(cell.textParagraphs)
                                val cellFill = runCatching { mapPaintStyle(cell.fillStyle?.paint) }.getOrDefault(SlideFill.None)
                                
                                val borderTop = runCatching { getCellBorder(cell, TableCell.BorderEdge.top) }.getOrNull()
                                val borderBottom = runCatching { getCellBorder(cell, TableCell.BorderEdge.bottom) }.getOrNull()
                                val borderLeft = runCatching { getCellBorder(cell, TableCell.BorderEdge.left) }.getOrNull()
                                val borderRight = runCatching { getCellBorder(cell, TableCell.BorderEdge.right) }.getOrNull()
                                
                                val cellAnchor = when (runCatching { cell.verticalAlignment }.getOrNull()) {
                                    org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE -> VerticalAnchor.MIDDLE
                                    org.apache.poi.sl.usermodel.VerticalAlignment.BOTTOM -> VerticalAnchor.BOTTOM
                                    else -> VerticalAnchor.TOP
                                }
                                
                                cells.add(SlideTableCell(cellParas, cellFill, borderTop, borderBottom, borderLeft, borderRight, cellAnchor))
                            } else {
                                cells.add(SlideTableCell(emptyList()))
                            }
                        }
                        rows.add(SlideTableRow(cells))
                    }
                    SlideElement.TableBox(bounds, rows, colWidths, rowHeights)
                }
                is PictureShape<*, *> -> {
                    try {
                        val pictureData = shape.pictureData
                        val bytes = pictureData.data
                        SlideElement.ImageBox(bounds, bytes, rotation, null)
                    } catch (e: Exception) {
                        null
                    }
                }
                is AutoShape<*, *> -> {
                    val paragraphs = mapTextParagraphs(shape.textParagraphs)
                    val fill = runCatching { mapPaintStyle(shape.fillStyle?.paint) }.getOrDefault(SlideFill.None)
                    val border = runCatching { mapBorder(shape.strokeStyle) }.getOrNull()
                    val shapeType = runCatching { shape.shapeType?.name?.lowercase() }.getOrNull() ?: "rect"
                    
                    SlideElement.ShapeBox(bounds, shapeType, fill, border, rotation, paragraphs)
                }
                is TextShape<*, *> -> {
                    val paragraphs = mapTextParagraphs(shape.textParagraphs)
                    val fill = runCatching { mapPaintStyle(shape.fillStyle?.paint) }.getOrDefault(SlideFill.None)
                    val border = runCatching { mapBorder(shape.strokeStyle) }.getOrNull()
                    val verticalAnchor = when (runCatching { shape.verticalAlignment }.getOrNull()) {
                        org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE -> VerticalAnchor.MIDDLE
                        org.apache.poi.sl.usermodel.VerticalAlignment.BOTTOM -> VerticalAnchor.BOTTOM
                        else -> VerticalAnchor.TOP
                    }
                    SlideElement.TextBox(bounds, paragraphs, verticalAnchor, fill, border, rotation)
                }
                else -> null
            }
        } catch (t: Throwable) {
            null
        }
    }

    private fun mapTextParagraphs(paragraphs: List<TextParagraph<*, *, *>>): List<SlideParagraph> {
        return paragraphs.map { p ->
            val runs = p.textRuns.map { r ->
                val fontColor = runCatching { convertPaintToColor(r.fontColor) }.getOrDefault(0xFF000000L)
                val fontSize = runCatching { r.fontSize?.toFloat() }.getOrNull() ?: 14f
                val bold = runCatching { r.isBold }.getOrElse { false }
                val italic = runCatching { r.isItalic }.getOrElse { false }
                val underline = runCatching { r.isUnderlined }.getOrElse { false }
                val strikethrough = runCatching { r.isStrikethrough }.getOrElse { false }
                val fontFamily = runCatching { r.fontFamily }.getOrNull()

                val hyperlink = runCatching { r.hyperlink?.address }.getOrNull()
                val baseline = BaselineShift.NORMAL

                val rawTextVal = runCatching {
                    r.javaClass.getMethod("getRawText").invoke(r) as String
                }.getOrElse {
                    runCatching {
                        r.javaClass.getMethod("getText").invoke(r) as String
                    }.getOrDefault("")
                }

                SlideRun(
                    text = rawTextVal,
                    fontSize = fontSize,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    strikethrough = strikethrough,
                    color = fontColor,
                    fontFamily = fontFamily,
                    baseline = baseline,
                    hyperlink = hyperlink
                )
            }

            val alignment = when (runCatching { p.textAlign }.getOrNull()) {
                TextParagraph.TextAlign.CENTER -> SlideTextAlign.CENTER
                TextParagraph.TextAlign.RIGHT -> SlideTextAlign.END
                TextParagraph.TextAlign.JUSTIFY -> SlideTextAlign.JUSTIFY
                else -> SlideTextAlign.START
            }

            val spaceBefore = runCatching { p.spaceBefore?.toFloat() }.getOrNull() ?: 0f
            val spaceAfter = runCatching { p.spaceAfter?.toFloat() }.getOrNull() ?: 0f
            val lineSpacing = runCatching { p.lineSpacing?.toFloat() }.getOrNull() ?: 1.0f
            val indentLevel = runCatching { p.indentLevel }.getOrDefault(0)

            val bulletStyle = runCatching { p.bulletStyle }.getOrNull()
            val bulletChar = runCatching { bulletStyle?.bulletCharacter }.getOrNull()
            val bulletColor = runCatching { bulletStyle?.bulletFontColor?.let { convertPaintToColor(it) } }.getOrNull()
            val bulletSize = runCatching {
                bulletStyle?.bulletFontSize?.let {
                    if (it > 0) {
                        val pct = it.toFloat() / 100f
                        val baseSize = p.textRuns.firstOrNull()?.fontSize?.toFloat() ?: 14f
                        baseSize * pct
                    } else {
                        null
                    }
                }
            }.getOrNull()

            SlideParagraph(
                runs = runs,
                alignment = alignment,
                spaceBefore = spaceBefore,
                spaceAfter = spaceAfter,
                lineSpacing = lineSpacing,
                indentLevel = indentLevel,
                bulletChar = bulletChar,
                bulletColor = bulletColor,
                bulletSize = bulletSize
            )
        }
    }

    private fun getSlideTransition(slide: Slide<*, *>): SlideTransition {
        if (slide is org.apache.poi.xslf.usermodel.XSLFSlide) {
            return runCatching {
                val domNode = slide.xmlObject.domNode
                val children = domNode.childNodes
                var transitionNode: org.w3c.dom.Node? = null
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    val name = child.localName ?: child.nodeName.substringAfter(':')
                    if (name == "transition") {
                        transitionNode = child
                        break
                    }
                }
                if (transitionNode != null) {
                    val transitionChildren = transitionNode.childNodes
                    for (i in 0 until transitionChildren.length) {
                        val subChild = transitionChildren.item(i)
                        val subName = subChild.localName ?: subChild.nodeName.substringAfter(':')
                        when (subName) {
                            "fade" -> return SlideTransition.Fade()
                            "push" -> {
                                val dirAttr = (subChild as? org.w3c.dom.Element)?.getAttribute("dir") ?: "l"
                                val dir = when (dirAttr) {
                                    "l", "left" -> Direction.LEFT
                                    "r", "right" -> Direction.RIGHT
                                    "u", "up" -> Direction.UP
                                    "d", "down" -> Direction.DOWN
                                    else -> Direction.LEFT
                                }
                                return SlideTransition.Push(dir)
                            }
                            "wipe" -> {
                                val dirAttr = (subChild as? org.w3c.dom.Element)?.getAttribute("dir") ?: "l"
                                val dir = when (dirAttr) {
                                    "l", "left" -> Direction.LEFT
                                    "r", "right" -> Direction.RIGHT
                                    "u", "up" -> Direction.UP
                                    "d", "down" -> Direction.DOWN
                                    else -> Direction.LEFT
                                }
                                return SlideTransition.Wipe(dir)
                            }
                            "cover" -> {
                                val dirAttr = (subChild as? org.w3c.dom.Element)?.getAttribute("dir") ?: "l"
                                val dir = when (dirAttr) {
                                    "l", "left" -> Direction.LEFT
                                    "r", "right" -> Direction.RIGHT
                                    "u", "up" -> Direction.UP
                                    "d", "down" -> Direction.DOWN
                                    else -> Direction.LEFT
                                }
                                return SlideTransition.Cover(dir)
                            }
                            "dissolve" -> return SlideTransition.Dissolve()
                            "split" -> {
                                val orientAttr = (subChild as? org.w3c.dom.Element)?.getAttribute("orient") ?: "h"
                                val orient = if (orientAttr == "v" || orientAttr == "vertical") Orientation.VERTICAL else Orientation.HORIZONTAL
                                return SlideTransition.Split(orient)
                            }
                        }
                    }
                }
                SlideTransition.None
            }.getOrDefault(SlideTransition.None)
        }
        return SlideTransition.None
    }

    private fun getSlideNotes(slide: Slide<*, *>): String? {
        val notes = runCatching { slide.notes }.getOrNull() ?: return null
        val sb = StringBuilder()
        val shapes = runCatching { notes.shapes }.getOrNull() ?: return null
        for (shape in shapes) {
            if (shape is TextShape<*, *>) {
                val text = runCatching { shape.text }.getOrNull()
                if (!text.isNullOrBlank()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(text)
                }
            }
        }
        return if (sb.isEmpty()) null else sb.toString()
    }
}
