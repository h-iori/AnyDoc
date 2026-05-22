package com.ioristudios.anydoc.util

import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.poi.hssf.usermodel.HSSFWorkbook

object DocumentFileIo {
    private val utf8: Charset = Charsets.UTF_8

    fun readText(path: String): String = File(path).readText(utf8)

    fun writeText(path: String, text: String) {
        File(path).writeText(text, utf8)
    }

    fun readCsv(path: String): List<List<String>> = parseCsv(readText(path))

    fun writeCsv(path: String, rows: List<List<String>>) {
        writeText(path, rows.joinToString("\n") { row ->
            row.joinToString(",") { cell -> encodeCsvCell(cell) }
        })
    }

    fun readDocxText(path: String): List<String> {
        val xml = readZipEntry(path, "word/document.xml") ?: return emptyList()
        return extractXmlText(xml)
            .joinToString(" ")
            .split(Regex("\\s{2,}|\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(extractXmlText(xml).joinToString(" ").trim()) }
    }

    fun writeDocxInPlace(path: String, editedParagraphs: Map<Int, String>) {
        val originalXml = readZipEntry(path, "word/document.xml")
            ?: error("word/document.xml not found")
        val db = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder()
        val doc = db.parse(ByteArrayInputStream(originalXml.toByteArray(utf8)))
        val body = doc.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "body").item(0)
            ?: doc.getElementsByTagName("w:body").item(0)
            ?: error("Document body not found")

        val pNodes = mutableListOf<Node>()
        val children = body.childNodes
        for (i in 0 until children.length) {
            collectParagraphNodes(children.item(i), pNodes)
        }

        for (index in pNodes.indices) {
            val pNode = pNodes[index]
            val newText = editedParagraphs[index] ?: continue

            var originalRPr: Node? = null
            val pChildren = pNode.childNodes
            for (i in 0 until pChildren.length) {
                val child = pChildren.item(i)
                val childName = child.localName ?: child.nodeName.substringAfter(':')
                if (childName == "r") {
                    val runChildren = child.childNodes
                    for (j in 0 until runChildren.length) {
                        val rChild = runChildren.item(j)
                        val rChildName = rChild.localName ?: rChild.nodeName.substringAfter(':')
                        if (rChildName == "rPr") {
                            originalRPr = rChild
                            break
                        }
                    }
                    if (originalRPr != null) break
                }
            }

            val toRemove = mutableListOf<Node>()
            for (i in 0 until pChildren.length) {
                val child = pChildren.item(i)
                val childName = child.localName ?: child.nodeName.substringAfter(':')
                if (childName != "pPr" && childName != "pStyle") {
                    if (childName == "r") {
                        if (!hasPreservedElements(child)) {
                            toRemove.add(child)
                        }
                    } else {
                        toRemove.add(child)
                    }
                }
            }

            for (node in toRemove) {
                pNode.removeChild(node)
            }

            val nsUri = pNode.namespaceURI ?: "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            val prefix = pNode.prefix ?: "w"
            val rTag = if (prefix.isNotEmpty()) "$prefix:r" else "r"
            val tTag = if (prefix.isNotEmpty()) "$prefix:t" else "t"

            val rNode = doc.createElementNS(nsUri, rTag)
            if (originalRPr != null) {
                rNode.appendChild(doc.importNode(originalRPr, true))
            }
            val tNode = doc.createElementNS(nsUri, tTag)
            tNode.setAttribute("xml:space", "preserve")
            tNode.textContent = newText

            rNode.appendChild(tNode)
            pNode.appendChild(rNode)
        }

        val transformerFactory = javax.xml.transform.TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no")
        val writer = java.io.StringWriter()
        transformer.transform(
            javax.xml.transform.dom.DOMSource(doc),
            javax.xml.transform.stream.StreamResult(writer)
        )
        val updatedXml = writer.toString()
        replaceZipEntry(path, "word/document.xml", updatedXml.toByteArray(utf8))
    }

    private fun collectParagraphNodes(node: Node, outList: MutableList<Node>) {
        val name = node.localName ?: node.nodeName.substringAfter(':')
        when (name) {
            "p" -> {
                outList.add(node)
            }
            "tbl" -> {
                val children = node.childNodes
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    val childName = child.localName ?: child.nodeName.substringAfter(':')
                    if (childName == "tr") {
                        val trChildren = child.childNodes
                        for (j in 0 until trChildren.length) {
                            val trChild = trChildren.item(j)
                            val trChildName = trChild.localName ?: trChild.nodeName.substringAfter(':')
                            if (trChildName == "tc") {
                                val tcChildren = trChild.childNodes
                                for (k in 0 until tcChildren.length) {
                                    collectParagraphNodes(tcChildren.item(k), outList)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasPreservedElements(node: Node): Boolean {
        val name = node.localName ?: node.nodeName.substringAfter(':')
        if (name == "drawing" || name == "object") return true
        if (name == "br") {
            val typeAttr = node.attributes?.getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "type")?.nodeValue
                ?: node.attributes?.getNamedItem("w:type")?.nodeValue
            if (typeAttr == "page") return true
        }
        val children = node.childNodes
        for (i in 0 until children.length) {
            if (hasPreservedElements(children.item(i))) return true
        }
        return false
    }

    fun readXlsxRows(path: String): List<List<String>> {
        val sharedStrings = readZipEntry(path, "xl/sharedStrings.xml")
            ?.let(::extractXmlText)
            ?: emptyList()
        val sheetXml = readZipEntry(path, "xl/worksheets/sheet1.xml") ?: return emptyList()
        val document = parseXml(sheetXml)
        val rows = document.getElementsByTagName("row")
        val result = mutableListOf<List<String>>()
        for (rowIndex in 0 until rows.length) {
            val rowNode = rows.item(rowIndex)
            val cells = mutableListOf<String>()
            val children = rowNode.childNodes
            for (childIndex in 0 until children.length) {
                val cell = children.item(childIndex)
                if (cell.nodeName.endsWith(":c") || cell.nodeName == "c") {
                    val type = cell.attributes?.getNamedItem("t")?.nodeValue
                    val value = firstChildText(cell, setOf("v", "t"))
                    cells += if (type == "s") {
                        sharedStrings.getOrNull(value.toIntOrNull() ?: -1).orEmpty()
                    } else {
                        value
                    }
                }
            }
            result += cells
        }
        return result
    }

    fun writeXlsxRows(path: String, rows: List<List<String>>) {
        val sheet = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("<sheetData>")
            rows.forEachIndexed { rowIndex, row ->
                val excelRow = rowIndex + 1
                append("""<row r="$excelRow">""")
                row.forEachIndexed { colIndex, value ->
                    val cellRef = "${columnName(colIndex)}$excelRow"
                    append("""<c r="$cellRef" t="inlineStr"><is><t xml:space="preserve">""")
                    append(escapeXml(value))
                    append("</t></is></c>")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        replaceZipEntry(path, "xl/worksheets/sheet1.xml", sheet.toByteArray(utf8))
    }

    /**
     * Write edited cells to a specific sheet in the XLSX file.
     *
     * This rebuilds the sheet XML using inline strings for edited cells
     * and preserves other data. The [sheetIndex] is 0-based.
     * [editedCells] maps "row:col" → new value.
     * [originalContent] provides the full parsed sheet for preserving unedited cells.
     */
    fun writeXlsxSheet(
        path: String,
        sheetIndex: Int,
        editedCells: Map<String, String>,
        originalContent: com.ioristudios.anydoc.model.DocumentContent.SpreadsheetContent
    ) {
        val sheet = originalContent.sheets.getOrNull(sheetIndex) ?: return

        // Discover sheet path from workbook rels
        val sheetPath = discoverSheetPath(path, sheetIndex)

        // Build cell map: start from original, apply edits
        val cellMap = mutableMapOf<Int, MutableMap<Int, String>>() // row -> (col -> value)
        for (row in sheet.rows) {
            val rowMap = cellMap.getOrPut(row.rowIndex) { mutableMapOf() }
            for ((colIdx, cell) in row.cells) {
                rowMap[colIdx] = cell.value
            }
        }

        // Apply edits
        for ((key, value) in editedCells) {
            val parts = key.split(":")
            if (parts.size == 2) {
                val row = parts[0].toIntOrNull() ?: continue
                val col = parts[1].toIntOrNull() ?: continue
                val rowMap = cellMap.getOrPut(row) { mutableMapOf() }
                rowMap[col] = value
            }
        }

        // Build XML
        val xml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

            // Preserve column definitions
            if (sheet.columnWidths.isNotEmpty()) {
                append("<cols>")
                sheet.columnWidths.entries.sortedBy { it.key }.forEach { (colIdx, width) ->
                    val colNum = colIdx + 1
                    append("""<col min="$colNum" max="$colNum" width="$width" customWidth="1"/>""")
                }
                append("</cols>")
            }

            append("<sheetData>")
            val sortedRows = cellMap.keys.sorted()
            for (rowIdx in sortedRows) {
                val excelRow = rowIdx + 1
                val rowCells = cellMap[rowIdx] ?: continue
                append("""<row r="$excelRow">""")
                for (colIdx in rowCells.keys.sorted()) {
                    val value = rowCells[colIdx] ?: ""
                    val cellRef = "${columnName(colIdx)}$excelRow"
                    append("""<c r="$cellRef" t="inlineStr"><is><t xml:space="preserve">""")
                    append(escapeXml(value))
                    append("</t></is></c>")
                }
                append("</row>")
            }
            append("</sheetData>")

            // Preserve merge cells
            if (sheet.mergedRegions.isNotEmpty()) {
                append("""<mergeCells count="${sheet.mergedRegions.size}">""")
                for (region in sheet.mergedRegions) {
                    val startRef = "${columnName(region.startCol)}${region.startRow + 1}"
                    val endRef = "${columnName(region.endCol)}${region.endRow + 1}"
                    append("""<mergeCell ref="$startRef:$endRef"/>""")
                }
                append("</mergeCells>")
            }

            append("</worksheet>")
        }

        replaceZipEntry(path, sheetPath, xml.toByteArray(utf8))
    }

    fun writeXlsSheet(
        path: String,
        sheetIndex: Int,
        editedCells: Map<String, String>
    ) {
        val file = File(path)
        val workbook = if (file.exists() && file.length() > 0) {
            FileInputStream(file).use { fis ->
                HSSFWorkbook(fis)
            }
        } else {
            HSSFWorkbook()
        }

        val sheet = if (sheetIndex < workbook.numberOfSheets) {
            workbook.getSheetAt(sheetIndex)
        } else {
            while (workbook.numberOfSheets <= sheetIndex) {
                workbook.createSheet("Sheet${workbook.numberOfSheets + 1}")
            }
            workbook.getSheetAt(sheetIndex)
        }

        for ((cellKey, value) in editedCells) {
            val parts = cellKey.split(":")
            if (parts.size == 2) {
                val rowIdx = parts[0].toIntOrNull() ?: continue
                val colIdx = parts[1].toIntOrNull() ?: continue

                val row = sheet.getRow(rowIdx) ?: sheet.createRow(rowIdx)
                val cell = row.getCell(colIdx) ?: row.createCell(colIdx)

                val doubleVal = value.toDoubleOrNull()
                if (doubleVal != null) {
                    cell.setCellValue(doubleVal)
                } else if (value.startsWith("=")) {
                    runCatching {
                        cell.cellFormula = value.substring(1)
                    }.onFailure {
                        cell.setCellValue(value)
                    }
                } else {
                    cell.setCellValue(value)
                }
            }
        }

        FileOutputStream(file).use { fos ->
            workbook.write(fos)
        }
    }

    private fun discoverSheetPath(path: String, sheetIndex: Int): String {
        val defaultPath = "xl/worksheets/sheet${sheetIndex + 1}.xml"
        return runCatching {
            val relsXml = readZipEntry(path, "xl/_rels/workbook.xml.rels") ?: return defaultPath
            val wbXml = readZipEntry(path, "xl/workbook.xml") ?: return defaultPath

            val db = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
            }.newDocumentBuilder()

            // Get rId for the sheet
            val wbDoc = db.parse(java.io.ByteArrayInputStream(wbXml.toByteArray(utf8)))
            val sheetNodes = wbDoc.getElementsByTagName("sheet")
            val rId = if (sheetIndex < sheetNodes.length) {
                val node = sheetNodes.item(sheetIndex)
                node.attributes?.getNamedItemNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"
                )?.nodeValue
                    ?: node.attributes?.getNamedItem("r:id")?.nodeValue
                    ?: return defaultPath
            } else {
                return defaultPath
            }

            // Resolve rId to file path
            val relsDoc = db.parse(java.io.ByteArrayInputStream(relsXml.toByteArray(utf8)))
            val relNodes = relsDoc.getElementsByTagName("Relationship")
            for (i in 0 until relNodes.length) {
                val relNode = relNodes.item(i)
                val id = relNode.attributes?.getNamedItem("Id")?.nodeValue
                if (id == rId) {
                    val target = relNode.attributes?.getNamedItem("Target")?.nodeValue ?: return defaultPath
                    return if (target.startsWith("/")) {
                        target.substring(1)
                    } else if (!target.startsWith("xl/")) {
                        "xl/$target"
                    } else {
                        target
                    }
                }
            }
            defaultPath
        }.getOrDefault(defaultPath)
    }

    fun readPptxText(path: String): List<String> {
        ZipFile(path).use { zip ->
            return zip.entries().asSequence()
                .filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
                .sortedBy { it.name.filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE }
                .map { entry -> extractXmlText(zip.getInputStream(entry).readBytes().toString(utf8)).joinToString(" ") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toList()
        }
    }

    fun flattenRows(rows: List<List<String>>): String =
        rows.joinToString("\n") { it.joinToString("\t") }

    fun parseEditedRows(text: String): List<List<String>> =
        text.lineSequence().map { line -> line.split('\t') }.toList()

    private fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < text.length) {
            val ch = text[index]
            when {
                ch == '"' && inQuotes && index + 1 < text.length && text[index + 1] == '"' -> {
                    cell.append('"')
                    index++
                }
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    row += cell.toString()
                    cell.clear()
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && index + 1 < text.length && text[index + 1] == '\n') index++
                    row += cell.toString()
                    rows += row.toList()
                    row.clear()
                    cell.clear()
                }
                else -> cell.append(ch)
            }
            index++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            rows += row.toList()
        }
        return rows
    }

    private fun encodeCsvCell(value: String): String {
        val requiresQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (requiresQuotes) "\"$escaped\"" else escaped
    }

    private fun readZipEntry(path: String, entryName: String): String? =
        ZipFile(path).use { zip ->
            zip.getEntry(entryName)?.let { zip.getInputStream(it).readBytes().toString(utf8) }
        }

    private fun replaceZipEntry(path: String, targetEntry: String, bytes: ByteArray) {
        val source = File(path)
        val temp = File(source.parentFile, "${source.name}.tmp")
        ZipFile(source).use { zip ->
            ZipOutputStream(temp.outputStream().buffered()).use { output ->
                zip.entries().asSequence().forEach { entry ->
                    if (entry.name != targetEntry) {
                        output.putNextEntry(ZipEntry(entry.name))
                        zip.getInputStream(entry).copyTo(output)
                        output.closeEntry()
                    }
                }
                output.putNextEntry(ZipEntry(targetEntry))
                output.write(bytes)
                output.closeEntry()
            }
        }
        if (!temp.renameTo(source)) {
            temp.copyTo(source, overwrite = true)
            temp.delete()
        }
    }

    private fun extractXmlText(xml: String): List<String> {
        val document = parseXml(xml)
        val values = mutableListOf<String>()
        collectText(document.documentElement, values)
        return values
    }

    private fun collectText(node: Node, values: MutableList<String>) {
        if (node.nodeType == Node.TEXT_NODE) {
            val text = node.nodeValue?.trim().orEmpty()
            if (text.isNotEmpty()) values += text
        }
        val children = node.childNodes
        for (index in 0 until children.length) {
            collectText(children.item(index), values)
        }
    }

    private fun parseXml(xml: String) =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        }.newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(utf8)))

    private fun firstChildText(node: Node, localNames: Set<String>): String {
        val children = node.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            val local = child.localName ?: child.nodeName.substringAfter(':')
            if (local in localNames) return child.textContent.orEmpty()
            firstChildText(child, localNames).takeIf { it.isNotEmpty() }?.let { return it }
        }
        return ""
    }

    private fun columnName(index: Int): String {
        var value = index
        val name = StringBuilder()
        do {
            name.insert(0, 'A' + (value % 26))
            value = value / 26 - 1
        } while (value >= 0)
        return name.toString()
    }

    private fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
}
