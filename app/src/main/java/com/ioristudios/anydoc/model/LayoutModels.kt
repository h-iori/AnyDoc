package com.ioristudios.anydoc.model

import com.ioristudios.anydoc.model.DocxParagraph
import com.ioristudios.anydoc.model.DocxTable
import com.ioristudios.anydoc.model.DocxImage

data class LayoutConfig(
    val pageWidthPoints: Float = 612f,   // US Letter Width (8.5 inches * 72 pt/inch)
    val pageHeightPoints: Float = 792f,  // US Letter Height (11 inches * 72 pt/inch)
    val marginLeftPoints: Float = 72f,   // 1 inch margins
    val marginTopPoints: Float = 72f,
    val marginRightPoints: Float = 72f,
    val marginBottomPoints: Float = 72f,
    val scaleFactor: Float = 2.0f        // High-fidelity scale factor for text measurement & bitmap rendering
) {
    val contentWidthPoints: Float
        get() = pageWidthPoints - marginLeftPoints - marginRightPoints

    val contentHeightPoints: Float
        get() = pageHeightPoints - marginTopPoints - marginBottomPoints
}

data class LayoutPage(
    val pageNumber: Int,
    val elements: List<PositionedElement>,
    val config: LayoutConfig
)

sealed class PositionedElement {
    abstract val x: Float      // Relative to top-left of the page content area (or cell area)
    abstract val y: Float
    abstract val width: Float
    abstract val height: Float

    data class Paragraph(
        val para: DocxParagraph,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float
    ) : PositionedElement()

    data class Table(
        val table: DocxTable,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float,
        val rowHeights: List<Float>,
        val cellWidths: List<Float>,
        // List of rows, where each row is a list of cells, where each cell is a list of positioned sub-elements
        val cellElements: List<List<List<PositionedElement>>>
    ) : PositionedElement()

    data class Image(
        val img: DocxImage,
        override val x: Float,
        override val y: Float,
        override val width: Float,
        override val height: Float
    ) : PositionedElement()
}
