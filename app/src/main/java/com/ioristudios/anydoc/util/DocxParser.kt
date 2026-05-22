package com.ioristudios.anydoc.util

import com.ioristudios.anydoc.model.*
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import org.apache.poi.hwpf.HWPFDocument

object DocxParser {

    fun parseDocx(path: String): DocumentContent.WordDocumentContent {
        val elements = mutableListOf<DocxElement>()
        runCatching {
            ZipFile(path).use { zip ->
                // 1. Read relationships to resolve images
                val relsMap = mutableMapOf<String, String>()
                runCatching {
                    val entry = zip.getEntry("word/_rels/document.xml.rels")
                        ?: zip.getEntry("word/document.xml.rels")
                    val relsXml = entry?.let { zip.getInputStream(it).readBytes().toString(Charsets.UTF_8) }
                    if (relsXml != null) {
                        val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        val doc = db.parse(ByteArrayInputStream(relsXml.toByteArray(Charsets.UTF_8)))
                        val rels = doc.getElementsByTagName("Relationship")
                        for (i in 0 until rels.length) {
                            val node = rels.item(i)
                            val id = node.attributes.getNamedItem("Id")?.nodeValue
                            val target = node.attributes.getNamedItem("Target")?.nodeValue
                            if (id != null && target != null) {
                                relsMap[id] = target
                            }
                        }
                    }
                }

                // 2. Parse main document content
                val docXmlEntry = zip.getEntry("word/document.xml") ?: error("word/document.xml not found")
                val docXml = zip.getInputStream(docXmlEntry).readBytes().toString(Charsets.UTF_8)
                val db = DocumentBuilderFactory.newInstance().apply {
                    isNamespaceAware = true
                }.newDocumentBuilder()
                val xmlDoc = db.parse(ByteArrayInputStream(docXml.toByteArray(Charsets.UTF_8)))
                val body = xmlDoc.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "body").item(0)
                    ?: xmlDoc.getElementsByTagName("w:body").item(0)
                    ?: error("Document body not found")

                val children = body.childNodes
                for (i in 0 until children.length) {
                    val child = children.item(i)
                    val nodeName = child.localName ?: child.nodeName.substringAfter(':')
                    when (nodeName) {
                        "p" -> parseParagraphNode(child, relsMap, elements)
                        "tbl" -> {
                            val tbl = parseTableNode(child, relsMap)
                            if (tbl != null) {
                                elements.add(DocxElement.Table(tbl))
                            }
                        }
                    }
                }
            }
        }

        val plainText = buildPlainText(elements)
        return DocumentContent.WordDocumentContent(elements, plainText)
    }

    fun parseDoc(path: String): DocumentContent.WordDocumentContent {
        val elements = mutableListOf<DocxElement>()
        runCatching {
            File(path).inputStream().use { fis ->
                HWPFDocument(fis).use { doc ->
                    val range = doc.range
                    for (i in 0 until range.numParagraphs()) {
                        val p = range.getParagraph(i)
                        val spans = mutableListOf<DocxSpan>()
                        for (j in 0 until p.numCharacterRuns()) {
                            val run = p.getCharacterRun(j)
                            val text = run.text()
                            if (!text.isNullOrEmpty()) {
                                // Extract styles
                                val bold = run.isBold
                                val italic = run.isItalic
                                val underline = run.underlineCode.toInt() != 0
                                val fontSize = run.fontSize / 2f // POI stores in half-points
                                spans.add(DocxSpan(text, bold, italic, underline, fontSize))
                            }
                        }
                        if (spans.isNotEmpty()) {
                            elements.add(DocxElement.Paragraph(DocxParagraph(spans, DocxParagraphStyle.Body)))
                        }
                    }
                }
            }
        }
        val plainText = buildPlainText(elements)
        return DocumentContent.WordDocumentContent(elements, plainText)
    }

    fun parseRtf(path: String): DocumentContent.WordDocumentContent {
        val elements = mutableListOf<DocxElement>()
        runCatching {
            val file = File(path)
            val bytes = file.readBytes()
            val text = bytes.toString(Charsets.US_ASCII)

            val currentSpans = mutableListOf<DocxSpan>()
            val currentText = StringBuilder()

            var bold = false
            var italic = false
            var underline = false

            var idx = 0
            val n = text.length
            while (idx < n) {
                val c = text[idx]
                if (c == '\\') {
                    idx++
                    if (idx >= n) break
                    val nextC = text[idx]
                    if (nextC == '\\' || nextC == '{' || nextC == '}') {
                        currentText.append(nextC)
                        idx++
                    } else {
                        val wordStart = idx
                        while (idx < n && text[idx].isLetter()) {
                            idx++
                        }
                        val controlWord = text.substring(wordStart, idx)
                        var param = ""
                        val paramStart = idx
                        if (idx < n && (text[idx] == '-' || text[idx].isDigit())) {
                            if (text[idx] == '-') idx++
                            while (idx < n && text[idx].isDigit()) {
                                idx++
                            }
                            param = text.substring(paramStart, idx)
                        }
                        if (idx < n && text[idx] == ' ') {
                            idx++
                        }

                        when (controlWord) {
                            "par", "sect" -> {
                                if (currentText.isNotEmpty() || currentSpans.isNotEmpty()) {
                                    if (currentText.isNotEmpty()) {
                                        currentSpans.add(DocxSpan(currentText.toString(), bold, italic, underline))
                                        currentText.clear()
                                    }
                                    elements.add(DocxElement.Paragraph(DocxParagraph(currentSpans.toList())))
                                    currentSpans.clear()
                                }
                            }
                            "b" -> {
                                if (currentText.isNotEmpty()) {
                                    currentSpans.add(DocxSpan(currentText.toString(), bold, italic, underline))
                                    currentText.clear()
                                }
                                bold = (param != "0")
                            }
                            "i" -> {
                                if (currentText.isNotEmpty()) {
                                    currentSpans.add(DocxSpan(currentText.toString(), bold, italic, underline))
                                    currentText.clear()
                                }
                                italic = (param != "0")
                            }
                            "ul" -> {
                                if (currentText.isNotEmpty()) {
                                    currentSpans.add(DocxSpan(currentText.toString(), bold, italic, underline))
                                    currentText.clear()
                                }
                                underline = true
                            }
                            "ulnone" -> {
                                if (currentText.isNotEmpty()) {
                                    currentSpans.add(DocxSpan(currentText.toString(), bold, italic, underline))
                                    currentText.clear()
                                }
                                underline = false
                            }
                            "u" -> {
                                val unicodeVal = param.toIntOrNull()
                                if (unicodeVal != null) {
                                    currentText.append(unicodeVal.toChar())
                                }
                                if (idx < n) {
                                    val nextChar = text[idx]
                                    if (nextChar != '\\' && nextChar != '{' && nextChar != '}') {
                                        idx++
                                    }
                                }
                            }
                        }
                    }
                } else if (c == '{' || c == '}') {
                    idx++
                } else {
                    currentText.append(c)
                    idx++
                }
            }

            if (currentText.isNotEmpty() || currentSpans.isNotEmpty()) {
                if (currentText.isNotEmpty()) {
                    currentSpans.add(DocxSpan(currentText.toString(), bold, italic, underline))
                }
                elements.add(DocxElement.Paragraph(DocxParagraph(currentSpans.toList())))
            }
        }

        val plainText = buildPlainText(elements)
        return DocumentContent.WordDocumentContent(elements, plainText)
    }

    private fun parseParagraphNode(pNode: Node, relsMap: Map<String, String>, outList: MutableList<DocxElement>) {
        val spans = mutableListOf<DocxSpan>()
        var style = DocxParagraphStyle.Body
        var isPageBreak = false
        var alignment = DocxTextAlignment.Start
        var indentStartTwips = 0
        var hangingTwips = 0
        var spacingBeforeTwips = 0
        var spacingAfterTwips = 120
        var lineSpacingTwips: Int? = null
        var listLevel = 0
        var isNumbered = false

        val children = pNode.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            val childName = child.localName ?: child.nodeName.substringAfter(':')
            when (childName) {
                "pPr" -> {
                    val pPrChildren = child.childNodes
                    for (j in 0 until pPrChildren.length) {
                        val pPrChild = pPrChildren.item(j)
                        val pPrChildName = pPrChild.localName ?: pPrChild.nodeName.substringAfter(':')
                        if (pPrChildName == "pStyle") {
                            val styleVal = pPrChild.attributes.getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")?.nodeValue
                                ?: pPrChild.attributes.getNamedItem("w:val")?.nodeValue
                            if (styleVal != null) {
                                style = when {
                                    styleVal.contains("Heading1", ignoreCase = true) -> DocxParagraphStyle.Heading1
                                    styleVal.contains("Heading2", ignoreCase = true) -> DocxParagraphStyle.Heading2
                                    styleVal.contains("Heading3", ignoreCase = true) -> DocxParagraphStyle.Heading3
                                    styleVal.contains("Heading4", ignoreCase = true) -> DocxParagraphStyle.Heading4
                                    styleVal.contains("List", ignoreCase = true) -> DocxParagraphStyle.ListItem
                                    else -> DocxParagraphStyle.Body
                                }
                            }
                        } else if (pPrChildName == "jc") {
                            alignment = when (pPrChild.wordVal()) {
                                "center" -> DocxTextAlignment.Center
                                "right", "end" -> DocxTextAlignment.End
                                "both", "distribute" -> DocxTextAlignment.Justify
                                else -> DocxTextAlignment.Start
                            }
                        } else if (pPrChildName == "ind") {
                            indentStartTwips = pPrChild.wordAttr("left")?.toIntOrNull()
                                ?: pPrChild.wordAttr("start")?.toIntOrNull()
                                ?: 0
                            hangingTwips = pPrChild.wordAttr("hanging")?.toIntOrNull() ?: 0
                        } else if (pPrChildName == "spacing") {
                            spacingBeforeTwips = pPrChild.wordAttr("before")?.toIntOrNull() ?: 0
                            spacingAfterTwips = pPrChild.wordAttr("after")?.toIntOrNull() ?: spacingAfterTwips
                            lineSpacingTwips = pPrChild.wordAttr("line")?.toIntOrNull()
                        } else if (pPrChildName == "numPr") {
                            style = DocxParagraphStyle.ListItem
                            isNumbered = true
                            val numChildren = pPrChild.childNodes
                            for (k in 0 until numChildren.length) {
                                val numChild = numChildren.item(k)
                                val numChildName = numChild.localName ?: numChild.nodeName.substringAfter(':')
                                if (numChildName == "ilvl") {
                                    listLevel = numChild.wordVal()?.toIntOrNull() ?: 0
                                }
                            }
                        }
                    }
                }
                "r" -> {
                    var bold = false
                    var italic = false
                    var underline = false
                    var fontSize: Float? = null
                    var color: String? = null
                    var fontFamily: String? = null

                    val runChildren = child.childNodes
                    for (j in 0 until runChildren.length) {
                        val runChild = runChildren.item(j)
                        val runChildName = runChild.localName ?: runChild.nodeName.substringAfter(':')
                        when (runChildName) {
                            "rPr" -> {
                                val rPrChildren = runChild.childNodes
                                for (k in 0 until rPrChildren.length) {
                                    val rPrChild = rPrChildren.item(k)
                                    val rPrChildName = rPrChild.localName ?: rPrChild.nodeName.substringAfter(':')
                                    when (rPrChildName) {
                                        "b" -> bold = true
                                        "i" -> italic = true
                                        "u" -> underline = true
                                        "sz" -> {
                                            val szVal = rPrChild.attributes.getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")?.nodeValue
                                                ?: rPrChild.attributes.getNamedItem("w:val")?.nodeValue
                                            val halfPt = szVal?.toFloatOrNull()
                                            if (halfPt != null) {
                                                fontSize = halfPt / 2f
                                            }
                                        }
                                        "color" -> {
                                            val colVal = rPrChild.attributes.getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")?.nodeValue
                                                ?: rPrChild.attributes.getNamedItem("w:val")?.nodeValue
                                            if (colVal != null && colVal != "auto") {
                                                color = colVal
                                            }
                                        }
                                        "rFonts" -> {
                                            fontFamily = rPrChild.wordAttr("ascii")
                                                ?: rPrChild.wordAttr("hAnsi")
                                                ?: rPrChild.wordAttr("cs")
                                        }
                                    }
                                }
                            }
                            "t" -> {
                                val textVal = runChild.textContent
                                if (!textVal.isNullOrEmpty()) {
                                    spans.add(DocxSpan(textVal, bold, italic, underline, fontSize, color, fontFamily))
                                }
                            }
                            "br" -> {
                                val brType = runChild.attributes.getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "type")?.nodeValue
                                    ?: runChild.attributes.getNamedItem("w:type")?.nodeValue
                                if (brType == "page") {
                                    isPageBreak = true
                                } else {
                                    spans.add(DocxSpan("\n", bold, italic, underline, fontSize, color, fontFamily))
                                }
                            }
                            "drawing" -> {
                                val blipNode = findBlipNode(runChild)
                                if (blipNode != null) {
                                    val embedId = blipNode.attributes.getNamedItemNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")?.nodeValue
                                        ?: blipNode.attributes.getNamedItem("r:embed")?.nodeValue
                                    if (embedId != null) {
                                        val relTarget = relsMap[embedId]
                                        if (relTarget != null) {
                                            val targetEntry = if (relTarget.startsWith("media/")) "word/$relTarget" else "word/media/${relTarget.substringAfterLast("/")}"
                                            if (spans.isNotEmpty()) {
                                                outList.add(DocxElement.Paragraph(DocxParagraph(
                                                    spans = spans.toList(),
                                                    style = style,
                                                    isPageBreak = isPageBreak,
                                                    alignment = alignment,
                                                    indentStartTwips = indentStartTwips,
                                                    hangingTwips = hangingTwips,
                                                    spacingBeforeTwips = spacingBeforeTwips,
                                                    spacingAfterTwips = spacingAfterTwips,
                                                    lineSpacingTwips = lineSpacingTwips,
                                                    listLevel = listLevel,
                                                    isNumbered = isNumbered
                                                )))
                                                spans.clear()
                                                isPageBreak = false
                                            }
                                            val ext = findExtentNode(runChild)
                                            outList.add(DocxElement.Image(DocxImage(
                                                entryName = targetEntry,
                                                widthEmu = ext?.wordAttr("cx")?.toLongOrNull(),
                                                heightEmu = ext?.wordAttr("cy")?.toLongOrNull()
                                            )))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (spans.isNotEmpty()) {
            outList.add(DocxElement.Paragraph(DocxParagraph(
                spans = spans.toList(),
                style = style,
                isPageBreak = isPageBreak,
                alignment = alignment,
                indentStartTwips = indentStartTwips,
                hangingTwips = hangingTwips,
                spacingBeforeTwips = spacingBeforeTwips,
                spacingAfterTwips = spacingAfterTwips,
                lineSpacingTwips = lineSpacingTwips,
                listLevel = listLevel,
                isNumbered = isNumbered
            )))
        } else if (isPageBreak) {
            outList.add(DocxElement.Paragraph(DocxParagraph(emptyList(), style, isPageBreak = true)))
        }
    }

    private fun parseTableNode(tblNode: Node, relsMap: Map<String, String>): DocxTable? {
        val rows = mutableListOf<DocxTableRow>()
        var tableWidthTwips: Int? = null
        val children = tblNode.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            val childName = child.localName ?: child.nodeName.substringAfter(':')
            if (childName == "tblPr") {
                val tblPrChildren = child.childNodes
                for (j in 0 until tblPrChildren.length) {
                    val tblPrChild = tblPrChildren.item(j)
                    val tblPrChildName = tblPrChild.localName ?: tblPrChild.nodeName.substringAfter(':')
                    if (tblPrChildName == "tblW") {
                        tableWidthTwips = tblPrChild.wordAttr("w")?.toIntOrNull()
                    }
                }
            } else if (childName == "tr") {
                val cells = mutableListOf<DocxTableCell>()
                val trChildren = child.childNodes
                for (j in 0 until trChildren.length) {
                    val trChild = trChildren.item(j)
                    val trChildName = trChild.localName ?: trChild.nodeName.substringAfter(':')
                    if (trChildName == "tc") {
                        val cellElements = mutableListOf<DocxElement>()
                        var cellWidthTwips: Int? = null
                        val tcChildren = trChild.childNodes
                        for (k in 0 until tcChildren.length) {
                            val tcChild = tcChildren.item(k)
                            val tcChildName = tcChild.localName ?: tcChild.nodeName.substringAfter(':')
                            when (tcChildName) {
                                "tcPr" -> {
                                    val tcPrChildren = tcChild.childNodes
                                    for (m in 0 until tcPrChildren.length) {
                                        val tcPrChild = tcPrChildren.item(m)
                                        val tcPrChildName = tcPrChild.localName ?: tcPrChild.nodeName.substringAfter(':')
                                        if (tcPrChildName == "tcW") {
                                            cellWidthTwips = tcPrChild.wordAttr("w")?.toIntOrNull()
                                        }
                                    }
                                }
                                "p" -> parseParagraphNode(tcChild, relsMap, cellElements)
                                "tbl" -> {
                                    val innerTable = parseTableNode(tcChild, relsMap)
                                    if (innerTable != null) {
                                        cellElements.add(DocxElement.Table(innerTable))
                                    }
                                }
                            }
                        }
                        cells.add(DocxTableCell(cellElements, cellWidthTwips))
                    }
                }
                rows.add(DocxTableRow(cells))
            }
        }
        if (rows.isEmpty()) return null
        return DocxTable(rows, tableWidthTwips)
    }

    private fun findBlipNode(node: Node): Node? {
        val localName = node.localName ?: node.nodeName.substringAfter(':')
        if (localName == "blip") return node
        val children = node.childNodes
        for (i in 0 until children.length) {
            val found = findBlipNode(children.item(i))
            if (found != null) return found
        }
        return null
    }

    private fun findExtentNode(node: Node): Node? {
        val localName = node.localName ?: node.nodeName.substringAfter(':')
        if (localName == "ext") return node
        val children = node.childNodes
        for (i in 0 until children.length) {
            val found = findExtentNode(children.item(i))
            if (found != null) return found
        }
        return null
    }

    fun paginateElements(elements: List<DocxElement>): List<List<DocxElement>> {
        val pages = mutableListOf<List<DocxElement>>()
        var currentPage = mutableListOf<DocxElement>()
        var currentWeight = 0
        val pageWeightLimit = 3600
        for (element in elements) {
            val weight = estimatedLayoutWeight(element)
            if (currentPage.isNotEmpty() && currentWeight + weight > pageWeightLimit) {
                pages.add(currentPage.toList())
                currentPage = mutableListOf()
                currentWeight = 0
            }
            currentPage.add(element)
            currentWeight += weight
            var shouldBreak = currentWeight >= pageWeightLimit
            if (element is DocxElement.Paragraph) {
                if (element.para.isPageBreak) {
                    shouldBreak = true
                }
            }
            if (shouldBreak) {
                pages.add(currentPage.toList())
                currentPage = mutableListOf()
                currentWeight = 0
            }
        }
        if (currentPage.isNotEmpty()) {
            pages.add(currentPage.toList())
        }
        return pages.ifEmpty { listOf(emptyList()) }
    }

    private fun estimatedLayoutWeight(element: DocxElement): Int {
        return when (element) {
            is DocxElement.Paragraph -> {
                val textLength = element.para.spans.sumOf { it.text.length }.coerceAtLeast(1)
                val styleBoost = when (element.para.style) {
                    DocxParagraphStyle.Heading1 -> 520
                    DocxParagraphStyle.Heading2 -> 420
                    DocxParagraphStyle.Heading3, DocxParagraphStyle.Heading4 -> 340
                    DocxParagraphStyle.ListItem -> 220
                    DocxParagraphStyle.Body -> 190
                }
                styleBoost + (textLength * 5) + element.para.spacingBeforeTwips / 8 + element.para.spacingAfterTwips / 8
            }
            is DocxElement.Table -> {
                380 + element.table.rows.sumOf { row ->
                    180 + (row.cells.maxOfOrNull { cell -> cell.elements.sumOf(::estimatedLayoutWeight) } ?: 120)
                }
            }
            is DocxElement.Image -> 1200
        }
    }

    fun buildPlainText(elements: List<DocxElement>): String {
        return elements.joinToString("\n") { el ->
            when (el) {
                is DocxElement.Paragraph -> el.para.spans.joinToString("") { it.text }
                is DocxElement.Table -> el.table.rows.joinToString("\n") { r ->
                    r.cells.joinToString("\t") { c ->
                        c.elements.joinToString(" ") { e ->
                            if (e is DocxElement.Paragraph) e.para.spans.joinToString("") { it.text } else ""
                        }
                    }
                }
                else -> ""
            }
        }
    }

    private fun Node.wordVal(): String? = wordAttr("val")

    private fun Node.wordAttr(localName: String): String? =
        attributes?.getNamedItemNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", localName)?.nodeValue
            ?: attributes?.getNamedItemNS("http://schemas.openxmlformats.org/drawingml/2006/main", localName)?.nodeValue
            ?: attributes?.getNamedItem(localName)?.nodeValue
            ?: attributes?.getNamedItem("w:$localName")?.nodeValue
            ?: attributes?.getNamedItem("a:$localName")?.nodeValue

}
