package com.example.samnote.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * 토큰 종류 - 구문 강조에서 각 텍스트 조각의 역할
 */
enum class TokenType {
    KEYWORD,
    STRING,
    COMMENT,
    NUMBER,
    ANNOTATION,
    TYPE,
    // Markdown 전용
    MD_HEADING,
    MD_BOLD,
    MD_ITALIC,
    MD_CODE,
    MD_LINK,
    MD_LIST_MARKER,
    MD_BLOCKQUOTE,
}

/**
 * 구문 규칙 - 정규식 패턴과 토큰 종류를 연결
 */
data class SyntaxRule(
    val pattern: Regex,
    val tokenType: TokenType
)

/**
 * 구문 강조 테마 - 토큰 종류별 스타일 매핑
 */
data class SyntaxTheme(
    val styles: Map<TokenType, SpanStyle>
)

// ── VS Code 스타일 라이트 테마 ─────────────────────────
val LightSyntaxTheme = SyntaxTheme(
    styles = mapOf(
        TokenType.KEYWORD to SpanStyle(color = Color(0xFF0000FF)),
        TokenType.STRING to SpanStyle(color = Color(0xFFA31515)),
        TokenType.COMMENT to SpanStyle(color = Color(0xFF008000)),
        TokenType.NUMBER to SpanStyle(color = Color(0xFF098658)),
        TokenType.ANNOTATION to SpanStyle(color = Color(0xFF808000)),
        TokenType.TYPE to SpanStyle(color = Color(0xFF267F99)),
        TokenType.MD_HEADING to SpanStyle(
            color = Color(0xFF0000FF),
            fontWeight = FontWeight.Bold
        ),
        TokenType.MD_BOLD to SpanStyle(fontWeight = FontWeight.Bold),
        TokenType.MD_ITALIC to SpanStyle(fontStyle = FontStyle.Italic),
        TokenType.MD_CODE to SpanStyle(
            color = Color(0xFFE53935),
            background = Color(0x1AE53935)
        ),
        TokenType.MD_LINK to SpanStyle(color = Color(0xFF0366D6)),
        TokenType.MD_LIST_MARKER to SpanStyle(color = Color(0xFF0000FF)),
        TokenType.MD_BLOCKQUOTE to SpanStyle(
            color = Color(0xFF6A737D),
            fontStyle = FontStyle.Italic
        ),
    )
)

// ── VS Code 스타일 다크 테마 ──────────────────────────
val DarkSyntaxTheme = SyntaxTheme(
    styles = mapOf(
        TokenType.KEYWORD to SpanStyle(color = Color(0xFF569CD6)),
        TokenType.STRING to SpanStyle(color = Color(0xFFCE9178)),
        TokenType.COMMENT to SpanStyle(color = Color(0xFF6A9955)),
        TokenType.NUMBER to SpanStyle(color = Color(0xFFB5CEA8)),
        TokenType.ANNOTATION to SpanStyle(color = Color(0xFFDCDCAA)),
        TokenType.TYPE to SpanStyle(color = Color(0xFF4EC9B0)),
        TokenType.MD_HEADING to SpanStyle(
            color = Color(0xFF569CD6),
            fontWeight = FontWeight.Bold
        ),
        TokenType.MD_BOLD to SpanStyle(fontWeight = FontWeight.Bold),
        TokenType.MD_ITALIC to SpanStyle(fontStyle = FontStyle.Italic),
        TokenType.MD_CODE to SpanStyle(
            color = Color(0xFFCE9178),
            background = Color(0x33CE9178)
        ),
        TokenType.MD_LINK to SpanStyle(color = Color(0xFF4EC9B0)),
        TokenType.MD_LIST_MARKER to SpanStyle(color = Color(0xFF569CD6)),
        TokenType.MD_BLOCKQUOTE to SpanStyle(
            color = Color(0xFF6A9955),
            fontStyle = FontStyle.Italic
        ),
    )
)
