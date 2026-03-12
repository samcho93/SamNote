package com.example.samnote.util

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.util.zip.ZipInputStream

/**
 * 외부 라이브러리 없이 문서 파일에서 텍스트를 추출
 * - DOCX: ZIP → word/document.xml → <w:t> 태그 추출
 * - PPTX: ZIP → ppt/slide*.xml → <a:t> 태그 추출
 * - DOC/PPT/HWP: 바이너리에서 텍스트 추출 시도
 */
object DocumentReader {

    // ── DOCX ──────────────────────────────────────────

    fun readDocx(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val sb = StringBuilder()
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                            parseWordXml(xmlContent, sb)
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                val result = sb.toString().trimEnd()
                result.ifEmpty { null }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseWordXml(xml: String, sb: StringBuilder) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        // <w:t> 또는 <t> 태그에서 텍스트 추출
                        if (name == "w:t" || name == "t") {
                            val text = parser.nextText()
                            sb.append(text)
                        }
                        // <w:tab/> → 탭 문자
                        if (name == "w:tab" || name == "tab") {
                            sb.append("\t")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        // </w:p> → 줄바꿈
                        if (name == "w:p" || name == "p") {
                            sb.append("\n")
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── PPTX ──────────────────────────────────────────

    fun readPptx(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val slides = mutableMapOf<Int, StringBuilder>()
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        // ppt/slides/slide1.xml, slide2.xml, ...
                        val match = Regex("ppt/slides/slide(\\d+)\\.xml").find(entry.name)
                        if (match != null) {
                            val slideNum = match.groupValues[1].toInt()
                            val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                            val sb = StringBuilder()
                            parseSlideXml(xmlContent, sb)
                            slides[slideNum] = sb
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }

                if (slides.isEmpty()) return@use null

                val result = StringBuilder()
                slides.keys.sorted().forEach { slideNum ->
                    result.append("── 슬라이드 $slideNum ").append("─".repeat(30)).append("\n\n")
                    result.append(slides[slideNum].toString().trim())
                    result.append("\n\n")
                }
                result.toString().trimEnd()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseSlideXml(xml: String, sb: StringBuilder) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        // <a:t> 태그에서 텍스트 추출
                        if (name == "a:t" || name == "t") {
                            val text = parser.nextText()
                            sb.append(text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        // </a:p> → 줄바꿈
                        if (name == "a:p" || name == "p") {
                            sb.append("\n")
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── DOC / PPT / HWP (레거시 바이너리) ─────────────

    fun readLegacyDocument(context: Context, uri: Uri, extension: String): String {
        return try {
            // HWPX (ZIP 기반 한글 파일) 시도
            if (extension == "hwp") {
                val hwpxResult = tryReadHwpx(context, uri)
                if (hwpxResult != null) return hwpxResult
            }

            // 바이너리에서 텍스트 추출 시도
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                extractTextFromBinary(bytes)
            } ?: fallbackMessage(extension)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackMessage(extension)
        }
    }

    /**
     * HWPX 형식 (ZIP 기반 한글 파일) 시도
     * 새로운 한글 파일은 ZIP 내 Contents/section0.xml 등에 텍스트 포함
     */
    private fun tryReadHwpx(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val sb = StringBuilder()
                var foundContent = false
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.startsWith("Contents/section") &&
                            entry.name.endsWith(".xml")
                        ) {
                            foundContent = true
                            val xmlContent = zip.bufferedReader(Charsets.UTF_8).readText()
                            parseHwpxSection(xmlContent, sb)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (foundContent && sb.isNotEmpty()) sb.toString().trimEnd() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseHwpxSection(xml: String, sb: StringBuilder) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))

            var eventType = parser.eventType
            var inText = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        // hp:t 또는 t 태그
                        if (name.endsWith(":t") || name == "t") {
                            inText = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inText) {
                            sb.append(parser.text)
                            inText = false
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        if (name.endsWith(":t") || name == "t") {
                            inText = false
                        }
                        // 단락 끝
                        if (name.endsWith(":p") || name == "p") {
                            sb.append("\n")
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 바이너리 파일에서 텍스트 추출 (DOC/PPT/HWP 공통)
     * UTF-16LE (DOC에서 주로 사용) 및 ASCII 텍스트 탐색
     */
    private fun extractTextFromBinary(bytes: ByteArray): String {
        val sb = StringBuilder()

        // UTF-16LE 텍스트 추출 시도 (DOC 파일의 주요 텍스트 인코딩)
        val utf16Text = extractUtf16LeText(bytes)
        if (utf16Text.length > 50) {
            return utf16Text
        }

        // ASCII 텍스트 추출 시도
        val asciiText = extractAsciiText(bytes)
        if (asciiText.length > 50) {
            return asciiText
        }

        // 둘 다 부족하면 합치기
        if (utf16Text.isNotEmpty()) sb.append(utf16Text)
        if (asciiText.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(asciiText)
        }

        return sb.toString().ifEmpty {
            "[텍스트를 추출할 수 없습니다. 외부 앱에서 열어주세요.]"
        }
    }

    private fun extractUtf16LeText(bytes: ByteArray): String {
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
                if (current.length >= 4) {
                    sb.append(current)
                    sb.append("\n")
                }
                current.clear()
            }
            i += 2
        }
        if (current.length >= 4) {
            sb.append(current)
        }
        return sb.toString()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun extractAsciiText(bytes: ByteArray): String {
        val sb = StringBuilder()
        val current = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E || c == 0x0A || c == 0x0D || c == 0x09) {
                current.append(c.toChar())
            } else {
                if (current.length >= 6) {
                    sb.append(current)
                    sb.append("\n")
                }
                current.clear()
            }
        }
        if (current.length >= 6) {
            sb.append(current)
        }
        return sb.toString()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun fallbackMessage(extension: String): String {
        val typeName = when (extension) {
            "doc" -> "Word (DOC)"
            "ppt" -> "PowerPoint (PPT)"
            "hwp" -> "한글 (HWP)"
            else -> extension.uppercase()
        }
        return "[${typeName} 파일입니다. 텍스트를 추출할 수 없습니다.\n외부 앱에서 열어주세요.]"
    }
}
