package com.ioristudios.anydoc.util

import com.ioristudios.anydoc.model.*
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFFont
import org.apache.poi.hssf.usermodel.HSSFPalette
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.BorderStyle

/**
 * Offline OOXML XLSX parser.
 *
 * Opens `.xlsx` as a ZIP and parses workbook, shared strings, styles, and
 * each worksheet to produce a [DocumentContent.SpreadsheetContent].
 */
object XlsxParser {

    fun parse(path: String): DocumentContent.SpreadsheetContent {
        return try {
            parseXlsxInternal(path)
        } catch (e: Exception) {
            try {
                parseXlsInternal(path)
            } catch (fallbackEx: Exception) {
                throw e
            }
        }
    }

    private fun parseXlsxInternal(path: String): DocumentContent.SpreadsheetContent {
        ZipFile(path).use { zip ->
            // 1. Parse shared strings
            val sharedStrings = parseSharedStrings(zip)

            // 2. Parse styles
            val styles = parseStyles(zip)

            // 3. Parse workbook to get sheet names & ordering
            val sheetInfos = parseWorkbook(zip)

            // 4. Parse workbook relationships to map rId → sheet file path
            val relsMap = parseWorkbookRels(zip)

            // 5. Parse each sheet
            val sheets = sheetInfos.map { (name, rId) ->
                val sheetPath = relsMap[rId] ?: "xl/worksheets/sheet1.xml"
                val normalizedPath = if (sheetPath.startsWith("/")) {
                    sheetPath.substring(1)
                } else if (!sheetPath.startsWith("xl/")) {
                    "xl/$sheetPath"
                } else {
                    sheetPath
                }
                parseSheet(zip, normalizedPath, name, sharedStrings)
            }

            return DocumentContent.SpreadsheetContent(
                sheets = sheets.ifEmpty {
                    // Fallback: try sheet1.xml directly
                    listOf(parseSheet(zip, "xl/worksheets/sheet1.xml", "Sheet1", sharedStrings))
                },
                styles = styles
            )
        }
    }

    fun parseXls(path: String): DocumentContent.SpreadsheetContent {
        return try {
            parseXlsInternal(path)
        } catch (e: Exception) {
            try {
                parseXlsxInternal(path)
            } catch (fallbackEx: Exception) {
                throw e
            }
        }
    }

    private fun parseXlsInternal(path: String): DocumentContent.SpreadsheetContent {
        FileInputStream(path).use { fis ->
            val workbook = HSSFWorkbook(fis)
            val sheets = mutableListOf<SpreadsheetSheet>()

            val numStyles = workbook.numCellStyles
            val styles = (0 until numStyles).map { idx ->
                val poiStyle = workbook.getCellStyleAt(idx)
                val fontIndex = poiStyle.fontIndexAsInt
                val poiFont = workbook.getFontAt(fontIndex)

                val bold = poiFont.bold
                val italic = poiFont.italic
                val fontSize = poiFont.fontHeightInPoints.toFloat()

                val palette = workbook.customPalette
                val fontColorHex = fontColorToHex(poiFont, palette)
                val bgColorHex = fillPatternToHex(poiStyle, palette)

                val alignment = when (poiStyle.alignment) {
                    HorizontalAlignment.CENTER -> SpreadsheetAlignment.CENTER
                    HorizontalAlignment.RIGHT -> SpreadsheetAlignment.RIGHT
                    HorizontalAlignment.LEFT -> SpreadsheetAlignment.LEFT
                    else -> SpreadsheetAlignment.GENERAL
                }

                val borders = CellBorders(
                    top = poiStyle.borderTop != BorderStyle.NONE,
                    bottom = poiStyle.borderBottom != BorderStyle.NONE,
                    left = poiStyle.borderLeft != BorderStyle.NONE,
                    right = poiStyle.borderRight != BorderStyle.NONE
                )

                SpreadsheetStyle(
                    fontBold = bold,
                    fontItalic = italic,
                    fontSize = fontSize,
                    fontColor = fontColorHex,
                    backgroundColor = bgColorHex,
                    numberFormat = poiStyle.dataFormatString,
                    horizontalAlignment = alignment,
                    borders = borders,
                    fontFamily = poiFont.fontName
                )
            }

            for (sheetIdx in 0 until workbook.numberOfSheets) {
                val poiSheet = workbook.getSheetAt(sheetIdx)
                val sheetName = workbook.getSheetName(sheetIdx)

                val columnWidths = mutableMapOf<Int, Float>()
                var maxCol = 0
                val rowCount = poiSheet.lastRowNum + 1

                val rows = mutableListOf<SpreadsheetRow>()
                for (rowIdx in 0..poiSheet.lastRowNum) {
                    val poiRow = poiSheet.getRow(rowIdx) ?: continue
                    val cells = mutableMapOf<Int, SpreadsheetCell>()
                    for (colIdx in 0 until poiRow.lastCellNum) {
                        val poiCell = poiRow.getCell(colIdx) ?: continue
                        if (colIdx + 1 > maxCol) {
                            maxCol = colIdx + 1
                        }

                        val widthInChars = poiSheet.getColumnWidth(colIdx) / 256f
                        val widthDp = widthInChars * 8f + 18f
                        columnWidths[colIdx] = widthDp

                        val styleIndex = poiCell.cellStyle.index.toInt()

                        var displayValue = ""
                        var rawValue = ""
                        var cellType = CellType.BLANK

                        when (poiCell.cellType) {
                            org.apache.poi.ss.usermodel.CellType.STRING -> {
                                displayValue = poiCell.stringCellValue ?: ""
                                rawValue = displayValue
                                cellType = CellType.STRING
                            }
                            org.apache.poi.ss.usermodel.CellType.NUMERIC -> {
                                if (DateUtil.isCellDateFormatted(poiCell)) {
                                    val dateVal = poiCell.dateCellValue
                                    displayValue = dateVal?.toString() ?: ""
                                    rawValue = displayValue
                                    cellType = CellType.DATE
                                } else {
                                    val numVal = poiCell.numericCellValue
                                    displayValue = formatNumericValue(numVal.toString())
                                    rawValue = numVal.toString()
                                    cellType = CellType.NUMBER
                                }
                            }
                            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> {
                                displayValue = if (poiCell.booleanCellValue) "TRUE" else "FALSE"
                                rawValue = if (poiCell.booleanCellValue) "1" else "0"
                                cellType = CellType.BOOLEAN
                            }
                            org.apache.poi.ss.usermodel.CellType.FORMULA -> {
                                cellType = CellType.FORMULA
                                runCatching {
                                    rawValue = "=" + poiCell.cellFormula
                                    val evaluator = workbook.creationHelper.createFormulaEvaluator()
                                    val evalValue = evaluator.evaluate(poiCell)
                                    displayValue = when (evalValue.cellType) {
                                        org.apache.poi.ss.usermodel.CellType.NUMERIC -> formatNumericValue(evalValue.numberValue.toString())
                                        org.apache.poi.ss.usermodel.CellType.STRING -> evalValue.stringValue ?: ""
                                        org.apache.poi.ss.usermodel.CellType.BOOLEAN -> if (evalValue.booleanValue) "TRUE" else "FALSE"
                                        else -> ""
                                    }
                                }.onFailure {
                                    rawValue = "=" + (runCatching { poiCell.cellFormula }.getOrNull() ?: "")
                                    displayValue = runCatching { poiCell.richStringCellValue.string }.getOrNull() ?: ""
                                }
                            }
                            org.apache.poi.ss.usermodel.CellType.BLANK -> {
                                displayValue = ""
                                rawValue = ""
                                cellType = CellType.BLANK
                            }
                            else -> {
                                displayValue = ""
                                rawValue = ""
                                cellType = CellType.BLANK
                            }
                        }

                        cells[colIdx] = SpreadsheetCell(
                            value = displayValue,
                            rawValue = rawValue,
                            type = cellType,
                            styleIndex = styleIndex
                        )
                    }
                    rows.add(SpreadsheetRow(rowIndex = rowIdx, cells = cells))
                }

                val mergedRegions = mutableListOf<MergedRegion>()
                for (i in 0 until poiSheet.numMergedRegions) {
                    val range = poiSheet.getMergedRegion(i)
                    mergedRegions.add(
                        MergedRegion(
                            startRow = range.firstRow,
                            endRow = range.lastRow,
                            startCol = range.firstColumn,
                            endCol = range.lastColumn
                        )
                    )
                }

                val finalColCount = maxCol.coerceAtLeast(10)
                val finalRowCount = rowCount.coerceAtLeast(20)

                for (col in 0 until finalColCount) {
                    if (!columnWidths.containsKey(col)) {
                        columnWidths[col] = 100f
                    }
                }

                sheets.add(
                    SpreadsheetSheet(
                        name = sheetName,
                        rows = rows,
                        columnCount = finalColCount,
                        rowCount = finalRowCount,
                        columnWidths = columnWidths,
                        mergedRegions = mergedRegions
                    )
                )
            }

            return DocumentContent.SpreadsheetContent(
                sheets = sheets,
                styles = styles
            )
        }
    }

    private fun fontColorToHex(font: HSSFFont, palette: HSSFPalette?): String? {
        val colorIndex = font.color
        if (colorIndex == org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.AUTOMATIC.index) {
            return null
        }
        val color = palette?.getColor(colorIndex) ?: return null
        val triplets = color.triplet ?: return null
        if (triplets.size < 3) return null
        return String.format("#%02X%02X%02X", triplets[0].toInt() and 0xFF, triplets[1].toInt() and 0xFF, triplets[2].toInt() and 0xFF)
    }

    private fun fillPatternToHex(style: org.apache.poi.ss.usermodel.CellStyle, palette: HSSFPalette?): String? {
        if (style.fillPattern == FillPatternType.NO_FILL) return null
        val colorIndex = style.fillForegroundColor
        if (colorIndex == org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined.AUTOMATIC.index) {
            return null
        }
        val color = palette?.getColor(colorIndex) ?: return null
        val triplets = color.triplet ?: return null
        if (triplets.size < 3) return null
        return String.format("#%02X%02X%02X", triplets[0].toInt() and 0xFF, triplets[1].toInt() and 0xFF, triplets[2].toInt() and 0xFF)
    }

    /**
     * Convert a CSV file into [DocumentContent.SpreadsheetContent] so it can
     * be rendered in the spreadsheet viewer with the same UI.
     */
    fun csvToSpreadsheet(rows: List<List<String>>): DocumentContent.SpreadsheetContent {
        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        val spreadsheetRows = rows.mapIndexed { rowIdx, row ->
            val cells = row.mapIndexed { colIdx, value ->
                val type = if (value.toDoubleOrNull() != null) CellType.NUMBER
                else if (value.equals("true", true) || value.equals("false", true)) CellType.BOOLEAN
                else CellType.STRING
                colIdx to SpreadsheetCell(value = value, type = type)
            }.toMap()
            SpreadsheetRow(rowIndex = rowIdx, cells = cells)
        }
        val sheet = SpreadsheetSheet(
            name = "Sheet1",
            rows = spreadsheetRows,
            columnCount = maxCols,
            rowCount = rows.size
        )
        return DocumentContent.SpreadsheetContent(sheets = listOf(sheet))
    }

    // ─── Shared Strings ──────────────────────────────────────────────────────

    private fun parseSharedStrings(zip: ZipFile): List<String> {
        val xml = readZipXml(zip, "xl/sharedStrings.xml") ?: return emptyList()
        val strings = mutableListOf<String>()
        val siNodes = xml.getElementsByTagName("si")
        for (i in 0 until siNodes.length) {
            strings.add(extractAllText(siNodes.item(i)))
        }
        return strings
    }

    private fun extractAllText(node: Node): String {
        val sb = StringBuilder()
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            val localName = child.localName ?: child.nodeName.substringAfter(':')
            when (localName) {
                "t" -> sb.append(child.textContent.orEmpty())
                "r" -> {
                    // Rich text run: extract <t> from within
                    val runChildren = child.childNodes
                    for (j in 0 until runChildren.length) {
                        val rc = runChildren.item(j)
                        val rcName = rc.localName ?: rc.nodeName.substringAfter(':')
                        if (rcName == "t") {
                            sb.append(rc.textContent.orEmpty())
                        }
                    }
                }
            }
        }
        return sb.toString()
    }

    // ─── Styles ──────────────────────────────────────────────────────────────

    private fun parseStyles(zip: ZipFile): List<SpreadsheetStyle> {
        val xml = readZipXml(zip, "xl/styles.xml") ?: return emptyList()

        // Parse number formats
        val numFormats = mutableMapOf<Int, String>()
        val numFmtNodes = xml.getElementsByTagName("numFmt")
        for (i in 0 until numFmtNodes.length) {
            val node = numFmtNodes.item(i)
            val id = node.attributes?.getNamedItem("numFmtId")?.nodeValue?.toIntOrNull() ?: continue
            val code = node.attributes?.getNamedItem("formatCode")?.nodeValue ?: continue
            numFormats[id] = code
        }

        // Parse fonts
        data class FontInfo(
            val bold: Boolean = false,
            val italic: Boolean = false,
            val size: Float = 11f,
            val color: String? = null,
            val family: String? = null
        )

        val fonts = mutableListOf<FontInfo>()
        val fontNodes = xml.getElementsByTagName("fonts")
        if (fontNodes.length > 0) {
            val fontsRoot = fontNodes.item(0)
            val fontChildren = fontsRoot.childNodes
            for (i in 0 until fontChildren.length) {
                val fontNode = fontChildren.item(i)
                val localName = fontNode.localName ?: fontNode.nodeName.substringAfter(':')
                if (localName != "font") continue
                var bold = false
                var italic = false
                var size = 11f
                var color: String? = null
                var family: String? = null
                val fc = fontNode.childNodes
                for (j in 0 until fc.length) {
                    val child = fc.item(j)
                    val childName = child.localName ?: child.nodeName.substringAfter(':')
                    when (childName) {
                        "b" -> bold = true
                        "i" -> italic = true
                        "sz" -> size = child.attributes?.getNamedItem("val")?.nodeValue?.toFloatOrNull() ?: 11f
                        "color" -> {
                            color = child.attributes?.getNamedItem("rgb")?.nodeValue
                            if (color == null) {
                                color = child.attributes?.getNamedItem("theme")?.nodeValue?.let { "theme:$it" }
                            }
                        }
                        "name" -> family = child.attributes?.getNamedItem("val")?.nodeValue
                    }
                }
                fonts.add(FontInfo(bold, italic, size, color, family))
            }
        }

        // Parse fills
        val fillColors = mutableListOf<String?>()
        val fillNodes = xml.getElementsByTagName("fills")
        if (fillNodes.length > 0) {
            val fillsRoot = fillNodes.item(0)
            val fillChildren = fillsRoot.childNodes
            for (i in 0 until fillChildren.length) {
                val fillNode = fillChildren.item(i)
                val localName = fillNode.localName ?: fillNode.nodeName.substringAfter(':')
                if (localName != "fill") { continue }
                var bgColor: String? = null
                val pfNodes = fillNode.childNodes
                for (j in 0 until pfNodes.length) {
                    val pf = pfNodes.item(j)
                    val pfName = pf.localName ?: pf.nodeName.substringAfter(':')
                    if (pfName == "patternFill") {
                        val pfChildren = pf.childNodes
                        for (k in 0 until pfChildren.length) {
                            val pc = pfChildren.item(k)
                            val pcName = pc.localName ?: pc.nodeName.substringAfter(':')
                            if (pcName == "fgColor" || pcName == "bgColor") {
                                bgColor = pc.attributes?.getNamedItem("rgb")?.nodeValue
                                if (bgColor != null) break
                            }
                        }
                        if (bgColor == null) {
                            bgColor = pf.attributes?.getNamedItem("fgColor")?.nodeValue
                        }
                    }
                }
                fillColors.add(bgColor)
            }
        }

        // Parse borders
        data class BorderInfo(val top: Boolean, val bottom: Boolean, val left: Boolean, val right: Boolean)

        val borderList = mutableListOf<BorderInfo>()
        val bordersNodes = xml.getElementsByTagName("borders")
        if (bordersNodes.length > 0) {
            val bordersRoot = bordersNodes.item(0)
            val borderChildren = bordersRoot.childNodes
            for (i in 0 until borderChildren.length) {
                val bNode = borderChildren.item(i)
                val bName = bNode.localName ?: bNode.nodeName.substringAfter(':')
                if (bName != "border") continue
                var top = false; var bottom = false; var left = false; var right = false
                val bc = bNode.childNodes
                for (j in 0 until bc.length) {
                    val side = bc.item(j)
                    val sideName = side.localName ?: side.nodeName.substringAfter(':')
                    val hasStyle = side.attributes?.getNamedItem("style")?.nodeValue?.let { it != "none" && it.isNotEmpty() } ?: false
                    when (sideName) {
                        "top" -> top = hasStyle
                        "bottom" -> bottom = hasStyle
                        "left" -> left = hasStyle
                        "right" -> right = hasStyle
                    }
                }
                borderList.add(BorderInfo(top, bottom, left, right))
            }
        }

        // Parse cell XFs (the actual style combinations applied to cells)
        val styles = mutableListOf<SpreadsheetStyle>()
        val cellXfsNodes = xml.getElementsByTagName("cellXfs")
        if (cellXfsNodes.length > 0) {
            val xfsRoot = cellXfsNodes.item(0)
            val xfChildren = xfsRoot.childNodes
            for (i in 0 until xfChildren.length) {
                val xfNode = xfChildren.item(i)
                val xfName = xfNode.localName ?: xfNode.nodeName.substringAfter(':')
                if (xfName != "xf") continue

                val fontId = xfNode.attributes?.getNamedItem("fontId")?.nodeValue?.toIntOrNull() ?: 0
                val fillId = xfNode.attributes?.getNamedItem("fillId")?.nodeValue?.toIntOrNull() ?: 0
                val borderId = xfNode.attributes?.getNamedItem("borderId")?.nodeValue?.toIntOrNull() ?: 0
                val numFmtId = xfNode.attributes?.getNamedItem("numFmtId")?.nodeValue?.toIntOrNull() ?: 0

                val font = fonts.getOrNull(fontId) ?: FontInfo()
                val bgColor = fillColors.getOrNull(fillId)
                val border = borderList.getOrNull(borderId) ?: BorderInfo(false, false, false, false)

                // Parse alignment from within xf
                var alignment = SpreadsheetAlignment.GENERAL
                val xfChildren2 = xfNode.childNodes
                for (j in 0 until xfChildren2.length) {
                    val child = xfChildren2.item(j)
                    val childName = child.localName ?: child.nodeName.substringAfter(':')
                    if (childName == "alignment") {
                        alignment = when (child.attributes?.getNamedItem("horizontal")?.nodeValue) {
                            "left" -> SpreadsheetAlignment.LEFT
                            "center" -> SpreadsheetAlignment.CENTER
                            "right" -> SpreadsheetAlignment.RIGHT
                            else -> SpreadsheetAlignment.GENERAL
                        }
                    }
                }

                styles.add(
                    SpreadsheetStyle(
                        fontBold = font.bold,
                        fontItalic = font.italic,
                        fontSize = font.size,
                        fontColor = font.color,
                        backgroundColor = bgColor,
                        numberFormat = numFormats[numFmtId],
                        horizontalAlignment = alignment,
                        borders = CellBorders(border.top, border.bottom, border.left, border.right),
                        fontFamily = font.family
                    )
                )
            }
        }

        return styles
    }

    // ─── Workbook ────────────────────────────────────────────────────────────

    private data class SheetInfo(val name: String, val rId: String)

    private fun parseWorkbook(zip: ZipFile): List<SheetInfo> {
        val xml = readZipXml(zip, "xl/workbook.xml") ?: return emptyList()
        val sheets = mutableListOf<SheetInfo>()
        val sheetNodes = xml.getElementsByTagName("sheet")
        for (i in 0 until sheetNodes.length) {
            val node = sheetNodes.item(i)
            val name = node.attributes?.getNamedItem("name")?.nodeValue ?: "Sheet${i + 1}"
            val rId = node.attributes?.getNamedItemNS(
                "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"
            )?.nodeValue
                ?: node.attributes?.getNamedItem("r:id")?.nodeValue
                ?: "rId${i + 1}"
            sheets.add(SheetInfo(name, rId))
        }
        return sheets
    }

    private fun parseWorkbookRels(zip: ZipFile): Map<String, String> {
        val xml = readZipXml(zip, "xl/_rels/workbook.xml.rels") ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        val relNodes = xml.getElementsByTagName("Relationship")
        for (i in 0 until relNodes.length) {
            val node = relNodes.item(i)
            val id = node.attributes?.getNamedItem("Id")?.nodeValue ?: continue
            val target = node.attributes?.getNamedItem("Target")?.nodeValue ?: continue
            map[id] = target
        }
        return map
    }

    // ─── Sheet Parsing ───────────────────────────────────────────────────────

    private fun parseSheet(
        zip: ZipFile,
        sheetPath: String,
        sheetName: String,
        sharedStrings: List<String>
    ): SpreadsheetSheet {
        val xml = readZipXml(zip, sheetPath)
            ?: return SpreadsheetSheet(sheetName, emptyList(), 0, 0)

        // Parse freeze panes
        var frozenRows = 0
        var frozenCols = 0
        val paneNodes = xml.getElementsByTagName("pane")
        if (paneNodes.length > 0) {
            val pane = paneNodes.item(0)
            frozenRows = pane.attributes?.getNamedItem("ySplit")?.nodeValue?.toIntOrNull() ?: 0
            frozenCols = pane.attributes?.getNamedItem("xSplit")?.nodeValue?.toIntOrNull() ?: 0
        }

        // Parse column widths
        val columnWidths = mutableMapOf<Int, Float>()
        val colNodes = xml.getElementsByTagName("col")
        for (i in 0 until colNodes.length) {
            val colNode = colNodes.item(i)
            val min = colNode.attributes?.getNamedItem("min")?.nodeValue?.toIntOrNull() ?: continue
            val max = colNode.attributes?.getNamedItem("max")?.nodeValue?.toIntOrNull() ?: min
            val width = colNode.attributes?.getNamedItem("width")?.nodeValue?.toFloatOrNull() ?: continue
            for (c in min..max) {
                columnWidths[c - 1] = width  // 0-based
            }
        }

        // Parse merged cells
        val mergedRegions = mutableListOf<MergedRegion>()
        val mergeCellNodes = xml.getElementsByTagName("mergeCell")
        for (i in 0 until mergeCellNodes.length) {
            val ref = mergeCellNodes.item(i).attributes?.getNamedItem("ref")?.nodeValue ?: continue
            val parts = ref.split(":")
            if (parts.size == 2) {
                val (startCol, startRow) = parseCellRef(parts[0])
                val (endCol, endRow) = parseCellRef(parts[1])
                mergedRegions.add(MergedRegion(startRow, endRow, startCol, endCol))
            }
        }

        // Parse rows and cells
        val rows = mutableListOf<SpreadsheetRow>()
        var maxCol = 0
        var maxRow = 0

        val rowNodes = xml.getElementsByTagName("row")
        for (rowIndex in 0 until rowNodes.length) {
            val rowNode = rowNodes.item(rowIndex)
            val rowNum = rowNode.attributes?.getNamedItem("r")?.nodeValue?.toIntOrNull()
                ?: (rowIndex + 1)
            val zeroRow = rowNum - 1
            if (rowNum > maxRow) maxRow = rowNum

            val cells = mutableMapOf<Int, SpreadsheetCell>()
            val children = rowNode.childNodes
            for (childIndex in 0 until children.length) {
                val cellNode = children.item(childIndex)
                val cellName = cellNode.localName ?: cellNode.nodeName.substringAfter(':')
                if (cellName != "c") continue

                val ref = cellNode.attributes?.getNamedItem("r")?.nodeValue
                val colIdx = if (ref != null) parseCellRef(ref).first else cells.size
                if (colIdx + 1 > maxCol) maxCol = colIdx + 1

                val typeAttr = cellNode.attributes?.getNamedItem("t")?.nodeValue
                val styleAttr = cellNode.attributes?.getNamedItem("s")?.nodeValue?.toIntOrNull() ?: -1

                // Get value and formula
                var valueText = ""
                var formulaText: String? = null
                val cellChildren = cellNode.childNodes
                for (k in 0 until cellChildren.length) {
                    val cc = cellChildren.item(k)
                    val ccName = cc.localName ?: cc.nodeName.substringAfter(':')
                    when (ccName) {
                        "v" -> valueText = cc.textContent.orEmpty()
                        "f" -> formulaText = cc.textContent.orEmpty()
                        "is" -> {
                            // Inline string
                            valueText = extractAllText(cc)
                        }
                    }
                }

                val displayValue: String
                val rawValue: String
                val cellType: CellType

                when (typeAttr) {
                    "s" -> {
                        // Shared string
                        displayValue = sharedStrings.getOrNull(valueText.toIntOrNull() ?: -1).orEmpty()
                        rawValue = displayValue
                        cellType = CellType.STRING
                    }
                    "inlineStr" -> {
                        displayValue = valueText
                        rawValue = valueText
                        cellType = CellType.STRING
                    }
                    "b" -> {
                        displayValue = if (valueText == "1") "TRUE" else "FALSE"
                        rawValue = valueText
                        cellType = CellType.BOOLEAN
                    }
                    "e" -> {
                        displayValue = valueText  // error value like #REF!
                        rawValue = valueText
                        cellType = CellType.STRING
                    }
                    "str" -> {
                        // Formula result is a string
                        displayValue = valueText
                        rawValue = formulaText?.let { "=$it" } ?: valueText
                        cellType = if (formulaText != null) CellType.FORMULA else CellType.STRING
                    }
                    else -> {
                        if (formulaText != null) {
                            // Formula cell, value is cached result
                            displayValue = valueText
                            rawValue = "=$formulaText"
                            cellType = CellType.FORMULA
                        } else if (valueText.isNotEmpty()) {
                            displayValue = formatNumericValue(valueText)
                            rawValue = valueText
                            cellType = CellType.NUMBER
                        } else {
                            displayValue = ""
                            rawValue = ""
                            cellType = CellType.BLANK
                        }
                    }
                }

                cells[colIdx] = SpreadsheetCell(
                    value = displayValue,
                    rawValue = rawValue,
                    type = cellType,
                    styleIndex = styleAttr
                )
            }

            rows.add(SpreadsheetRow(rowIndex = zeroRow, cells = cells))
        }

        return SpreadsheetSheet(
            name = sheetName,
            rows = rows,
            columnCount = maxCol,
            rowCount = maxRow,
            frozenRows = frozenRows,
            frozenCols = frozenCols,
            columnWidths = columnWidths,
            mergedRegions = mergedRegions
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Parse cell reference like "B3" into (colIndex, rowIndex) — both 0-based.
     */
    fun parseCellRef(ref: String): Pair<Int, Int> {
        var col = 0
        var i = 0
        while (i < ref.length && ref[i].isLetter()) {
            col = col * 26 + (ref[i].uppercaseChar() - 'A' + 1)
            i++
        }
        val row = ref.substring(i).toIntOrNull() ?: 1
        return Pair(col - 1, row - 1)  // 0-based
    }

    /**
     * Convert 0-based column index to Excel column name (A, B, ..., Z, AA, AB, ...).
     */
    fun columnName(index: Int): String {
        var value = index
        val name = StringBuilder()
        do {
            name.insert(0, 'A' + (value % 26))
            value = value / 26 - 1
        } while (value >= 0)
        return name.toString()
    }

    /**
     * Format a numeric string nicely — remove unnecessary trailing zeros.
     */
    private fun formatNumericValue(value: String): String {
        val d = value.toDoubleOrNull() ?: return value
        if (d == d.toLong().toDouble()) {
            return d.toLong().toString()
        }
        return value
    }

    private fun readZipXml(zip: ZipFile, entryName: String): Document? {
        val entry = zip.getEntry(entryName) ?: return null
        val bytes = zip.getInputStream(entry).readBytes()
        val dbFactory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
        }
        return dbFactory.newDocumentBuilder()
            .parse(ByteArrayInputStream(bytes))
    }
}
