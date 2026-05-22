package com.ioristudios.anydoc.util

import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

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

            val toRemove = mutableListOf<Node>()
            val pChildren = pNode.childNodes
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
