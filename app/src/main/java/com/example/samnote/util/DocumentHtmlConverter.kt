package com.example.samnote.util

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.zip.ZipInputStream

/**
 * 문서 파일을 HTML로 변환 (서식 유지)
 * - DOCX: bold, italic, underline, 글자 크기/색상, 정렬, 표, 제목
 * - PPTX: 슬라이드별 카드 형태, 텍스트 서식
 * - 레거시: 텍스트 추출 후 HTML 감싸기
 */
object DocumentHtmlConverter {

    // ── DOCX → HTML ─────────────────────────────────────

    fun convertDocx(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bodyHtml = StringBuilder()
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                            parseDocxToHtml(xml, bodyHtml)
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (bodyHtml.isEmpty()) null
                else buildHtml(bodyHtml.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseDocxToHtml(xml: String, html: StringBuilder) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inParagraph = false
            var inRun = false
            var inRunProps = false
            var inParaProps = false

            // Run 서식 상태
            var isBold = false
            var isItalic = false
            var isUnderline = false
            var runFontSize: Int? = null
            var runFontColor: String? = null

            // Paragraph 서식 상태
            var alignment: String? = null
            var headingLevel = 0

            val runHtml = StringBuilder()
            val paraRuns = StringBuilder()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        when {
                            // 표
                            name == "w:tbl" || name == "tbl" -> {
                                html.append("<table class=\"doc-table\">")
                            }
                            name == "w:tr" || name == "tr" -> html.append("<tr>")
                            name == "w:tc" || name == "tc" -> html.append("<td>")

                            // 단락
                            name == "w:p" || name == "p" -> {
                                inParagraph = true
                                paraRuns.clear()
                                alignment = null
                                headingLevel = 0
                            }

                            // 단락 속성
                            name == "w:pPr" || name == "pPr" -> inParaProps = true
                            (name == "w:jc" || name == "jc") && inParaProps -> {
                                alignment = getAttr(parser, "w:val")
                                    ?: getAttr(parser, "val")
                            }
                            (name == "w:pStyle" || name == "pStyle") && inParaProps -> {
                                val style = getAttr(parser, "w:val")
                                    ?: getAttr(parser, "val") ?: ""
                                val match = Regex("Heading(\\d+)", RegexOption.IGNORE_CASE).find(style)
                                if (match != null) {
                                    headingLevel = match.groupValues[1].toIntOrNull()?.coerceIn(1, 6) ?: 0
                                }
                            }

                            // 런
                            name == "w:r" || name == "r" -> {
                                inRun = true
                                isBold = false
                                isItalic = false
                                isUnderline = false
                                runFontSize = null
                                runFontColor = null
                                runHtml.clear()
                            }

                            // 런 속성
                            name == "w:rPr" || name == "rPr" -> inRunProps = true
                            (name == "w:b" || name == "b") && inRunProps -> isBold = true
                            (name == "w:i" || name == "i") && inRunProps -> isItalic = true
                            (name == "w:u" || name == "u") && inRunProps -> isUnderline = true
                            (name == "w:sz" || name == "sz") && inRunProps -> {
                                val sizeVal = getAttr(parser, "w:val")
                                    ?: getAttr(parser, "val")
                                runFontSize = sizeVal?.toIntOrNull()
                            }
                            (name == "w:color" || name == "color") && inRunProps -> {
                                runFontColor = getAttr(parser, "w:val")
                                    ?: getAttr(parser, "val")
                            }

                            // 텍스트
                            name == "w:t" || name == "t" -> {
                                if (inRun) {
                                    val text = parser.nextText()?.escapeHtml() ?: ""
                                    runHtml.append(text)
                                }
                            }
                            name == "w:tab" || name == "tab" -> runHtml.append("&emsp;")
                            name == "w:br" || name == "br" -> runHtml.append("<br>")
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        when {
                            name == "w:rPr" || name == "rPr" -> inRunProps = false
                            name == "w:pPr" || name == "pPr" -> inParaProps = false

                            // 런 닫기 → 스타일 적용
                            name == "w:r" || name == "r" -> {
                                if (inRun && runHtml.isNotEmpty()) {
                                    val styles = mutableListOf<String>()
                                    if (isBold) styles.add("font-weight:bold")
                                    if (isItalic) styles.add("font-style:italic")
                                    if (isUnderline) styles.add("text-decoration:underline")
                                    if (runFontSize != null) {
                                        styles.add("font-size:${runFontSize!! / 2}pt")
                                    }
                                    if (runFontColor != null && runFontColor != "auto") {
                                        styles.add("color:#$runFontColor")
                                    }

                                    if (styles.isNotEmpty()) {
                                        paraRuns.append("<span style=\"${styles.joinToString(";")}\">")
                                        paraRuns.append(runHtml)
                                        paraRuns.append("</span>")
                                    } else {
                                        paraRuns.append(runHtml)
                                    }
                                }
                                inRun = false
                            }

                            // 단락 닫기
                            name == "w:p" || name == "p" -> {
                                if (inParagraph) {
                                    val content = paraRuns.toString()
                                    val alignStyle = alignment?.let {
                                        " style=\"text-align:${mapAlignment(it)}\""
                                    } ?: ""

                                    if (headingLevel in 1..6) {
                                        html.append("<h$headingLevel$alignStyle>$content</h$headingLevel>\n")
                                    } else if (content.isNotEmpty()) {
                                        html.append("<p$alignStyle>$content</p>\n")
                                    } else {
                                        html.append("<p>&nbsp;</p>\n")
                                    }
                                    inParagraph = false
                                }
                            }

                            // 표 닫기
                            name == "w:tc" || name == "tc" -> html.append("</td>")
                            name == "w:tr" || name == "tr" -> html.append("</tr>\n")
                            name == "w:tbl" || name == "tbl" -> html.append("</table>\n")
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── PPTX → HTML ─────────────────────────────────────

    fun convertPptx(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val slides = mutableMapOf<Int, StringBuilder>()
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val match = Regex("ppt/slides/slide(\\d+)\\.xml").find(entry.name)
                        if (match != null) {
                            val slideNum = match.groupValues[1].toInt()
                            val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                            val sb = StringBuilder()
                            parseSlideToHtml(xml, sb, slideNum)
                            slides[slideNum] = sb
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (slides.isEmpty()) return@use null

                val bodyHtml = StringBuilder()
                slides.keys.sorted().forEach { slideNum ->
                    bodyHtml.append(slides[slideNum])
                }
                buildHtml(bodyHtml.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSlideToHtml(xml: String, html: StringBuilder, slideNum: Int) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            html.append("<div class=\"slide\">")
            html.append("<div class=\"slide-header\">슬라이드 $slideNum</div>")
            html.append("<div class=\"slide-body\">")

            var inRun = false
            var inRunProps = false
            var isBold = false
            var isItalic = false
            var runFontSize: Int? = null
            var runFontColor: String? = null
            val runHtml = StringBuilder()
            val paraRuns = StringBuilder()
            var inParagraph = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        when {
                            name == "a:p" || (name == "p" && !inParagraph) -> {
                                inParagraph = true
                                paraRuns.clear()
                            }
                            name == "a:r" || name == "r" -> {
                                inRun = true
                                isBold = false
                                isItalic = false
                                runFontSize = null
                                runFontColor = null
                                runHtml.clear()
                            }
                            name == "a:rPr" || name == "rPr" -> {
                                inRunProps = true
                                val bold = getAttr(parser, "b")
                                if (bold == "1" || bold == "true") isBold = true
                                val italic = getAttr(parser, "i")
                                if (italic == "1" || italic == "true") isItalic = true
                                val sz = getAttr(parser, "sz")
                                runFontSize = sz?.toIntOrNull()
                            }
                            (name == "a:solidFill" || name == "solidFill") && inRunProps -> {}
                            (name == "a:srgbClr" || name == "srgbClr") && inRunProps -> {
                                runFontColor = getAttr(parser, "val")
                            }
                            name == "a:t" || name == "t" -> {
                                if (inRun) {
                                    val text = parser.nextText()?.escapeHtml() ?: ""
                                    runHtml.append(text)
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        when {
                            name == "a:rPr" || name == "rPr" -> inRunProps = false
                            name == "a:r" || name == "r" -> {
                                if (inRun && runHtml.isNotEmpty()) {
                                    val styles = mutableListOf<String>()
                                    if (isBold) styles.add("font-weight:bold")
                                    if (isItalic) styles.add("font-style:italic")
                                    // PPTX sz는 100분의 1 pt
                                    if (runFontSize != null) {
                                        styles.add("font-size:${runFontSize!! / 100}pt")
                                    }
                                    if (runFontColor != null) {
                                        styles.add("color:#$runFontColor")
                                    }
                                    if (styles.isNotEmpty()) {
                                        paraRuns.append("<span style=\"${styles.joinToString(";")}\">")
                                        paraRuns.append(runHtml)
                                        paraRuns.append("</span>")
                                    } else {
                                        paraRuns.append(runHtml)
                                    }
                                }
                                inRun = false
                            }
                            name == "a:p" || name == "p" -> {
                                if (inParagraph) {
                                    val content = paraRuns.toString()
                                    if (content.isNotEmpty()) {
                                        html.append("<p>$content</p>\n")
                                    }
                                    inParagraph = false
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            html.append("</div></div>\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── XLS/XLSX → HTML ───────────────────────────────

    fun convertXls(context: Context, uri: Uri, extension: String): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                if (extension == "xlsx") {
                    val bodyHtml = StringBuilder()
                    val sheets = mutableMapOf<String, StringBuilder>()
                    var currentSheet = ""

                    ZipInputStream(inputStream).use { zip ->
                        // 먼저 공유 문자열 테이블 읽기
                        val sharedStrings = mutableListOf<String>()
                        val sheetXmls = mutableMapOf<String, String>()
                        val sheetNames = mutableListOf<String>()

                        var entry = zip.nextEntry
                        while (entry != null) {
                            when {
                                entry.name == "xl/sharedStrings.xml" -> {
                                    val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                                    parseSharedStrings(xml, sharedStrings)
                                }
                                entry.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) -> {
                                    val sheetNum = Regex("sheet(\\d+)")
                                        .find(entry.name)?.groupValues?.get(1) ?: "1"
                                    sheetXmls["Sheet$sheetNum"] =
                                        zip.bufferedReader(Charsets.UTF_8).readText()
                                    sheetNames.add("Sheet$sheetNum")
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }

                        // 시트를 HTML 테이블로 변환
                        sheetNames.sorted().forEach { name ->
                            val xml = sheetXmls[name] ?: return@forEach
                            val sheetHtml = StringBuilder()
                            parseXlsxSheet(xml, sharedStrings, sheetHtml)
                            if (sheetHtml.isNotEmpty()) {
                                bodyHtml.append("<h3 class=\"sheet-name\">$name</h3>\n")
                                bodyHtml.append(sheetHtml)
                            }
                        }
                    }

                    if (bodyHtml.isEmpty()) null
                    else buildHtml(bodyHtml.toString())
                } else {
                    // 레거시 XLS → 바이너리 텍스트 추출
                    val bytes = inputStream.readBytes()
                    val text = extractXlsText(bytes)
                    if (text.isNotEmpty()) {
                        val escapedText = text.escapeHtml().replace("\n", "<br>\n")
                        buildHtml("<div class=\"legacy\">$escapedText</div>")
                    } else null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSharedStrings(xml: String, strings: MutableList<String>) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var inSi = false
            val currentString = StringBuilder()
            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        if (name == "si") {
                            inSi = true
                            currentString.clear()
                        } else if (name == "t" && inSi) {
                            currentString.append(parser.nextText() ?: "")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if ((parser.name ?: "") == "si") {
                            strings.add(currentString.toString())
                            inSi = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseXlsxSheet(
        xml: String,
        sharedStrings: List<String>,
        html: StringBuilder
    ) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            html.append("<table class=\"doc-table\">\n")
            var inRow = false
            var inCell = false
            var cellType: String? = null
            var cellValue = StringBuilder()
            var hasData = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        when (name) {
                            "row" -> {
                                inRow = true
                                html.append("<tr>")
                            }
                            "c" -> {
                                inCell = true
                                cellType = getAttr(parser, "t")
                                cellValue.clear()
                            }
                            "v" -> {
                                if (inCell) {
                                    cellValue.append(parser.nextText() ?: "")
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        when (name) {
                            "c" -> {
                                if (inCell) {
                                    val value = cellValue.toString()
                                    val displayValue = if (cellType == "s") {
                                        // 공유 문자열 인덱스
                                        val idx = value.toIntOrNull()
                                        if (idx != null && idx < sharedStrings.size) {
                                            sharedStrings[idx]
                                        } else value
                                    } else {
                                        value
                                    }
                                    html.append("<td>${displayValue.escapeHtml()}</td>")
                                    if (displayValue.isNotEmpty()) hasData = true
                                    inCell = false
                                }
                            }
                            "row" -> {
                                if (inRow) {
                                    html.append("</tr>\n")
                                    inRow = false
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
            html.append("</table>\n")

            if (!hasData) {
                html.clear()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractXlsText(bytes: ByteArray): String {
        // XLS 바이너리에서 텍스트 추출 (UTF-16LE 기반)
        val sb = StringBuilder()
        val current = StringBuilder()
        var i = 0
        while (i + 1 < bytes.size) {
            val lo = bytes[i].toInt() and 0xFF
            val hi = bytes[i + 1].toInt() and 0xFF
            val char = (hi shl 8) or lo
            if (char in 0x20..0xD7FF || char == 0x0A || char == 0x0D || char == 0x09) {
                current.append(char.toChar())
            } else {
                if (current.length >= 3) {
                    sb.append(current)
                    sb.append("\n")
                }
                current.clear()
            }
            i += 2
        }
        if (current.length >= 3) {
            sb.append(current)
        }
        return sb.toString()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    // ── 레거시 (DOC/PPT/HWP) → HTML ────────────────────

    fun convertLegacyDocument(context: Context, uri: Uri, extension: String): String {
        val text = DocumentReader.readLegacyDocument(context, uri, extension)
        val escapedText = text.escapeHtml().replace("\n", "<br>\n")
        return buildHtml("<div class=\"legacy\">$escapedText</div>")
    }

    // ── HTML 템플릿 ─────────────────────────────────────

    private fun buildHtml(bodyContent: String): String {
        return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0">
<style>
  :root {
    --bg: #FFFFFF;
    --text: #1a1a1a;
    --border: #e0e0e0;
    --heading: #00695C;
    --slide-bg: #f8f9fa;
    --slide-header-bg: #00897B;
    --table-header: #e0f2f1;
    --table-border: #b2dfdb;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #1e1e1e;
      --text: #d4d4d4;
      --border: #404040;
      --heading: #4DB6AC;
      --slide-bg: #2d2d2d;
      --slide-header-bg: #00695C;
      --table-header: #1a3a36;
      --table-border: #2e5450;
    }
  }
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    font-family: -apple-system, 'Noto Sans KR', sans-serif;
    font-size: 14px;
    line-height: 1.7;
    color: var(--text);
    background: var(--bg);
    padding: 20px;
    word-wrap: break-word;
    overflow-wrap: break-word;
  }
  h1, h2, h3, h4, h5, h6 {
    color: var(--heading);
    margin: 1em 0 0.5em;
    line-height: 1.3;
  }
  h1 { font-size: 1.8em; border-bottom: 2px solid var(--border); padding-bottom: 0.3em; }
  h2 { font-size: 1.5em; }
  h3 { font-size: 1.25em; }
  p { margin: 0.4em 0; }
  .doc-table {
    width: 100%;
    border-collapse: collapse;
    margin: 1em 0;
  }
  .doc-table td {
    border: 1px solid var(--table-border);
    padding: 8px 12px;
    vertical-align: top;
  }
  .doc-table tr:first-child td {
    background: var(--table-header);
    font-weight: bold;
  }
  .slide {
    background: var(--slide-bg);
    border-radius: 12px;
    margin: 16px 0;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }
  .slide-header {
    background: var(--slide-header-bg);
    color: white;
    padding: 8px 16px;
    font-size: 0.85em;
    font-weight: 600;
  }
  .slide-body {
    padding: 20px;
  }
  .slide-body p {
    margin: 0.3em 0;
  }
  .legacy {
    white-space: pre-wrap;
    font-family: 'Noto Sans KR', sans-serif;
    line-height: 1.8;
  }
</style>
</head>
<body>
$bodyContent
</body>
</html>
""".trimIndent()
    }

    // ── 유틸리티 ─────────────────────────────────────────

    private fun getAttr(parser: XmlPullParser, name: String): String? {
        return parser.getAttributeValue(null, name)
    }

    private fun mapAlignment(align: String): String {
        return when (align.lowercase()) {
            "center" -> "center"
            "right", "end" -> "right"
            "both", "justify" -> "justify"
            else -> "left"
        }
    }

    private fun String.escapeHtml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}
