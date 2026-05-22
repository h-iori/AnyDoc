package com.ioristudios.anydoc.util

import com.ioristudios.anydoc.model.MergedRegion
import com.ioristudios.anydoc.model.SpreadsheetSheet

/**
 * Calculates concrete column widths and row heights for a [SpreadsheetSheet],
 * respecting custom widths from the XLSX, applying defaults, and handling
 * merged cell bounds.
 */
object SpreadsheetLayoutEngine {

    /** Default column width in dp */
    const val DEFAULT_COLUMN_WIDTH_DP = 88f

    /** Default row height in dp */
    const val DEFAULT_ROW_HEIGHT_DP = 32f

    /** Row number column width in dp */
    const val ROW_HEADER_WIDTH_DP = 48f

    /** Column header height in dp */
    const val COLUMN_HEADER_HEIGHT_DP = 28f

    /** Minimum column width in dp */
    const val MIN_COLUMN_WIDTH_DP = 40f

    /** Character-width to dp multiplier (Excel stores column widths in character-count) */
    const val CHAR_TO_DP = 8f

    data class GridLayout(
        val columnWidths: List<Float>,   // dp per column (0-based)
        val rowHeights: List<Float>,     // dp per row (0-based)
        val totalWidth: Float,           // sum of column widths
        val totalHeight: Float           // sum of row heights
    )

    /**
     * Compute the layout for a given sheet.
     *
     * @param sheet the parsed spreadsheet sheet
     * @param maxColumns cap for the number of columns to lay out (performance guard)
     * @param maxRows cap for the number of rows to lay out
     */
    fun computeLayout(
        sheet: SpreadsheetSheet,
        maxColumns: Int = sheet.columnCount.coerceAtLeast(1),
        maxRows: Int = sheet.rowCount.coerceAtLeast(1)
    ): GridLayout {
        val colCount = maxColumns.coerceAtLeast(1)
        val rowCount = maxRows.coerceAtLeast(1)

        // Column widths
        val columnWidths = (0 until colCount).map { colIdx ->
            val customWidth = sheet.columnWidths[colIdx]
            if (customWidth != null && customWidth > 0f) {
                (customWidth * CHAR_TO_DP).coerceAtLeast(MIN_COLUMN_WIDTH_DP)
            } else {
                DEFAULT_COLUMN_WIDTH_DP
            }
        }

        // Row heights
        val rowHeights = (0 until rowCount).map { rowIdx ->
            val customHeight = sheet.rowHeights[rowIdx]
            if (customHeight != null && customHeight > 0f) {
                customHeight // Already in points, close enough to dp for display
            } else {
                DEFAULT_ROW_HEIGHT_DP
            }
        }

        return GridLayout(
            columnWidths = columnWidths,
            rowHeights = rowHeights,
            totalWidth = columnWidths.sum(),
            totalHeight = rowHeights.sum()
        )
    }

    /**
     * Check if a cell at [row], [col] is the top-left origin of a merged region.
     * Returns the [MergedRegion] if so, null otherwise.
     */
    fun getMergedRegionAt(row: Int, col: Int, mergedRegions: List<MergedRegion>): MergedRegion? {
        return mergedRegions.firstOrNull { it.startRow == row && it.startCol == col }
    }

    /**
     * Check if a cell at [row], [col] is hidden by a merge (i.e., it's part of a
     * merged region but NOT the top-left origin).
     */
    fun isCellHiddenByMerge(row: Int, col: Int, mergedRegions: List<MergedRegion>): Boolean {
        return mergedRegions.any { region ->
            row in region.startRow..region.endRow &&
                col in region.startCol..region.endCol &&
                !(row == region.startRow && col == region.startCol)
        }
    }

    /**
     * Calculate the pixel width of a merged region.
     */
    fun mergedWidth(region: MergedRegion, columnWidths: List<Float>): Float {
        var w = 0f
        for (c in region.startCol..region.endCol) {
            w += columnWidths.getOrElse(c) { DEFAULT_COLUMN_WIDTH_DP }
        }
        return w
    }

    /**
     * Calculate the pixel height of a merged region.
     */
    fun mergedHeight(region: MergedRegion, rowHeights: List<Float>): Float {
        var h = 0f
        for (r in region.startRow..region.endRow) {
            h += rowHeights.getOrElse(r) { DEFAULT_ROW_HEIGHT_DP }
        }
        return h
    }
}
