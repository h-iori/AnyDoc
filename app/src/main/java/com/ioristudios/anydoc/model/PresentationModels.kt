package com.ioristudios.anydoc.model

import android.graphics.RectF

data class PresentationContent(
    val slides: List<SlideModel>,
    val slideWidth: Float = 720f,
    val slideHeight: Float = 540f
)

data class SlideModel(
    val index: Int,
    val elements: List<SlideElement>,
    val background: SlideBackground,
    val transition: SlideTransition? = null,
    val notes: String? = null
)

sealed class SlideBackground {
    data class SolidColor(val color: Long) : SlideBackground()
    data class GradientFill(val stops: List<GradientStop>, val angle: Float) : SlideBackground()
    data class ImageFill(val bytes: ByteArray) : SlideBackground() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ImageFill
            return bytes.contentEquals(other.bytes)
        }
        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
    data class ThemeBackground(val schemeColor: String) : SlideBackground()
    object None : SlideBackground()
}

sealed class SlideElement {
    data class TextBox(
        val bounds: SlideRect,
        val paragraphs: List<SlideParagraph>,
        val verticalAnchor: VerticalAnchor = VerticalAnchor.TOP,
        val fill: SlideFill? = null,
        val border: SlideBorder? = null,
        val rotation: Float = 0f
    ) : SlideElement()

    data class ShapeBox(
        val bounds: SlideRect,
        val shapeType: String, // "rect", "ellipse", "triangle", etc.
        val fill: SlideFill? = null,
        val border: SlideBorder? = null,
        val rotation: Float = 0f,
        val paragraphs: List<SlideParagraph> = emptyList()
    ) : SlideElement()

    data class ImageBox(
        val bounds: SlideRect,
        val bytes: ByteArray,
        val rotation: Float = 0f,
        val cropFraction: RectF? = null
    ) : SlideElement() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ImageBox
            if (bounds != other.bounds) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (rotation != other.rotation) return false
            if (cropFraction != other.cropFraction) return false
            return true
        }
        override fun hashCode(): Int {
            var result = bounds.hashCode()
            result = 31 * result + bytes.contentHashCode()
            result = 31 * result + rotation.hashCode()
            result = 31 * result + (cropFraction?.hashCode() ?: 0)
            return result
        }
    }

    data class TableBox(
        val bounds: SlideRect,
        val rows: List<SlideTableRow>,
        val colWidths: List<Float>,
        val rowHeights: List<Float>
    ) : SlideElement()

    data class GroupBox(
        val bounds: SlideRect,
        val elements: List<SlideElement>,
        val rotation: Float = 0f
    ) : SlideElement()
}

data class SlideTableRow(
    val cells: List<SlideTableCell>
)

data class SlideTableCell(
    val paragraphs: List<SlideParagraph>,
    val fill: SlideFill? = null,
    val borderTop: SlideBorder? = null,
    val borderBottom: SlideBorder? = null,
    val borderLeft: SlideBorder? = null,
    val borderRight: SlideBorder? = null,
    val verticalAnchor: VerticalAnchor = VerticalAnchor.TOP
)

data class SlideParagraph(
    val runs: List<SlideRun>,
    val alignment: SlideTextAlign = SlideTextAlign.START,
    val spaceBefore: Float = 0f,
    val spaceAfter: Float = 0f,
    val lineSpacing: Float = 1.0f,
    val indentLevel: Int = 0,
    val bulletChar: String? = null,
    val bulletColor: Long? = null,
    val bulletSize: Float? = null
)

data class SlideRun(
    val text: String,
    val fontSize: Float = 14f,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val color: Long = 0xFF000000.toLong(),
    val fontFamily: String? = null,
    val baseline: BaselineShift = BaselineShift.NORMAL,
    val hyperlink: String? = null
)

data class SlideRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

sealed class SlideFill {
    data class Solid(val color: Long) : SlideFill()
    data class Gradient(val stops: List<GradientStop>, val angle: Float) : SlideFill()
    data class Image(val bytes: ByteArray, val stretch: Boolean = true) : SlideFill() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Image
            if (!bytes.contentEquals(other.bytes)) return false
            if (stretch != other.stretch) return false
            return true
        }
        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + stretch.hashCode()
            return result
        }
    }
    object None : SlideFill()
}

data class SlideBorder(
    val color: Long,
    val widthPt: Float,
    val dashStyle: DashStyle = DashStyle.SOLID
)

sealed class SlideTransition {
    object None : SlideTransition()
    data class Fade(val durationMs: Int = 300) : SlideTransition()
    data class Push(val direction: Direction, val durationMs: Int = 300) : SlideTransition()
    data class Wipe(val direction: Direction, val durationMs: Int = 300) : SlideTransition()
    data class Cover(val direction: Direction, val durationMs: Int = 300) : SlideTransition()
    data class Dissolve(val durationMs: Int = 300) : SlideTransition()
    data class Split(val orientation: Orientation, val durationMs: Int = 300) : SlideTransition()
    data class Reveal(val direction: Direction, val durationMs: Int = 300) : SlideTransition()
    data class Wheel(val spokes: Int = 1, val durationMs: Int = 300) : SlideTransition()
}

enum class SlideTextAlign { START, CENTER, END, JUSTIFY }
enum class VerticalAnchor { TOP, MIDDLE, BOTTOM }
enum class DashStyle { SOLID, DASH, DOT, DASH_DOT }
enum class Direction { LEFT, RIGHT, UP, DOWN }
enum class Orientation { HORIZONTAL, VERTICAL }
enum class BaselineShift { NORMAL, SUPERSCRIPT, SUBSCRIPT }
data class GradientStop(val color: Long, val position: Float)

data class PresentationUiState(
    val currentSlide: Int = 0,
    val isSlideshowActive: Boolean = false,
    val slideshowIntervalMs: Long = 3000L,
    val isFullscreen: Boolean = false,
    val parsedContent: PresentationContent? = null
)
