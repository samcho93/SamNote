package com.example.samnote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samnote.data.ThemeMode
import com.example.samnote.ui.theme.BlockquoteBorderDark
import com.example.samnote.ui.theme.BlockquoteBorderLight
import com.example.samnote.ui.theme.CodeBlockBgDark
import com.example.samnote.ui.theme.CodeBlockBgLight
import com.example.samnote.ui.theme.EditorBgDark
import com.example.samnote.ui.theme.EditorBgLight

// ── 마크다운 요소 모델 ─────────────────────────────────

private sealed class MdElement {
    data class Heading(val level: Int, val text: String) : MdElement()
    data class Paragraph(val text: String) : MdElement()
    data class CodeBlock(val language: String, val code: String) : MdElement()
    data class ListItem(val marker: String, val text: String) : MdElement()
    data class Blockquote(val text: String) : MdElement()
    object HorizontalRule : MdElement()
    object BlankLine : MdElement()
}

// ── 마크다운 파서 ──────────────────────────────────────

private fun parseMarkdown(text: String): List<MdElement> {
    val elements = mutableListOf<MdElement>()
    val lines = text.split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        when {
            // 빈 줄
            trimmed.isEmpty() -> {
                elements.add(MdElement.BlankLine)
                i++
            }
            // 코드 블록 (```)
            trimmed.startsWith("```") -> {
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                elements.add(MdElement.CodeBlock(lang, codeLines.joinToString("\n")))
                i++ // skip closing ```
            }
            // 수평선
            trimmed.matches(Regex("^[-*_]{3,}$")) -> {
                elements.add(MdElement.HorizontalRule)
                i++
            }
            // 제목 (# ~ ######)
            trimmed.matches(Regex("^#{1,6}\\s+.*")) -> {
                val level = trimmed.takeWhile { it == '#' }.length
                val headingText = trimmed.drop(level).trim()
                elements.add(MdElement.Heading(level, headingText))
                i++
            }
            // 인용문 (>)
            trimmed.startsWith(">") -> {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trim())
                    i++
                }
                elements.add(MdElement.Blockquote(quoteLines.joinToString("\n")))
            }
            // 비순서 리스트 (-, *, +)
            trimmed.matches(Regex("^[-*+]\\s+.*")) -> {
                val marker = "•"
                val itemText = trimmed.substring(2).trim()
                elements.add(MdElement.ListItem(marker, itemText))
                i++
            }
            // 순서 리스트 (1., 2., ...)
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val marker = trimmed.takeWhile { it.isDigit() } + "."
                val itemText = trimmed.substringAfter(". ").trim()
                elements.add(MdElement.ListItem(marker, itemText))
                i++
            }
            // 일반 텍스트 (문단)
            else -> {
                val paraLines = mutableListOf<String>()
                while (i < lines.size) {
                    val l = lines[i].trim()
                    if (l.isEmpty() || l.startsWith("#") || l.startsWith("```")
                        || l.startsWith(">") || l.matches(Regex("^[-*+]\\s+.*"))
                        || l.matches(Regex("^\\d+\\.\\s+.*"))
                        || l.matches(Regex("^[-*_]{3,}$"))
                    ) break
                    paraLines.add(l)
                    i++
                }
                elements.add(MdElement.Paragraph(paraLines.joinToString(" ")))
            }
        }
    }
    return elements
}

// ── 인라인 마크다운 스타일 파싱 ────────────────────────

private fun buildInlineStyledText(
    text: String,
    baseColor: Color,
    linkColor: Color,
    codeColor: Color,
    codeBgColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            when {
                // 인라인 코드 `text`
                remaining.startsWith("`") -> {
                    val end = remaining.indexOf('`', 1)
                    if (end > 0) {
                        withStyle(SpanStyle(
                            color = codeColor,
                            background = codeBgColor,
                            fontFamily = FontFamily.Monospace
                        )) {
                            append(remaining.substring(1, end))
                        }
                        remaining = remaining.substring(end + 1)
                    } else {
                        append("`")
                        remaining = remaining.substring(1)
                    }
                }
                // 볼드+이탈릭 ***text***
                remaining.startsWith("***") -> {
                    val end = remaining.indexOf("***", 3)
                    if (end > 0) {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )) {
                            append(remaining.substring(3, end))
                        }
                        remaining = remaining.substring(end + 3)
                    } else {
                        append("***")
                        remaining = remaining.substring(3)
                    }
                }
                // 볼드 **text**
                remaining.startsWith("**") -> {
                    val end = remaining.indexOf("**", 2)
                    if (end > 0) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(remaining.substring(2, end))
                        }
                        remaining = remaining.substring(end + 2)
                    } else {
                        append("**")
                        remaining = remaining.substring(2)
                    }
                }
                // 이탈릭 *text*
                remaining.startsWith("*") -> {
                    val end = remaining.indexOf('*', 1)
                    if (end > 0) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(remaining.substring(1, end))
                        }
                        remaining = remaining.substring(end + 1)
                    } else {
                        append("*")
                        remaining = remaining.substring(1)
                    }
                }
                // 링크 [text](url)
                remaining.startsWith("[") -> {
                    val closeBracket = remaining.indexOf(']')
                    if (closeBracket > 0 && closeBracket + 1 < remaining.length
                        && remaining[closeBracket + 1] == '('
                    ) {
                        val closeParen = remaining.indexOf(')', closeBracket + 2)
                        if (closeParen > 0) {
                            val linkText = remaining.substring(1, closeBracket)
                            withStyle(SpanStyle(color = linkColor)) {
                                append(linkText)
                            }
                            remaining = remaining.substring(closeParen + 1)
                        } else {
                            append("[")
                            remaining = remaining.substring(1)
                        }
                    } else {
                        append("[")
                        remaining = remaining.substring(1)
                    }
                }
                // 일반 텍스트
                else -> {
                    val nextSpecial = findNextSpecialChar(remaining)
                    if (nextSpecial > 0) {
                        append(remaining.substring(0, nextSpecial))
                        remaining = remaining.substring(nextSpecial)
                    } else {
                        append(remaining)
                        remaining = ""
                    }
                }
            }
        }
    }
}

private fun findNextSpecialChar(text: String): Int {
    val specials = charArrayOf('`', '*', '[')
    for (i in text.indices) {
        if (text[i] in specials) return i
    }
    return -1
}

// ── 마크다운 미리보기 Composable ──────────────────────

@Composable
fun MarkdownPreview(
    text: String,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val bgColor = if (isDark) EditorBgDark else EditorBgLight
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = if (isDark) Color(0xFF4EC9B0) else Color(0xFF0366D6)
    val codeColor = if (isDark) Color(0xFFCE9178) else Color(0xFFE53935)
    val codeBgColor = if (isDark) CodeBlockBgDark else CodeBlockBgLight
    val blockquoteBorder = if (isDark) BlockquoteBorderDark else BlockquoteBorderLight

    val elements = remember(text) { parseMarkdown(text) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        for (element in elements) {
            when (element) {
                is MdElement.Heading -> {
                    val headingSize = when (element.level) {
                        1 -> fontSize * 2.0f
                        2 -> fontSize * 1.6f
                        3 -> fontSize * 1.35f
                        4 -> fontSize * 1.15f
                        else -> fontSize * 1.05f
                    }
                    Text(
                        text = buildInlineStyledText(element.text, textColor, linkColor, codeColor, codeBgColor),
                        fontSize = headingSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamily,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    if (element.level <= 2) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                is MdElement.Paragraph -> {
                    Text(
                        text = buildInlineStyledText(element.text, textColor, linkColor, codeColor, codeBgColor),
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        color = textColor,
                        lineHeight = fontSize * 1.6f,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                is MdElement.CodeBlock -> {
                    Surface(
                        color = codeBgColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = element.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize * 0.9f,
                            color = textColor,
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                }

                is MdElement.ListItem -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)) {
                        Text(
                            text = element.marker,
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = buildInlineStyledText(element.text, textColor, linkColor, codeColor, codeBgColor),
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                            color = textColor,
                            lineHeight = fontSize * 1.5f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MdElement.Blockquote -> {
                    val borderColor = blockquoteBorder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .drawBehind {
                                drawLine(
                                    color = borderColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 4.dp.toPx()
                                )
                            }
                            .padding(start = 16.dp)
                    ) {
                        Text(
                            text = buildInlineStyledText(element.text, textColor, linkColor, codeColor, codeBgColor),
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = fontSize * 1.5f
                        )
                    }
                }

                MdElement.HorizontalRule -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }

                MdElement.BlankLine -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}
