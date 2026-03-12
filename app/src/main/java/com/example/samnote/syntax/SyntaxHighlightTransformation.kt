package com.example.samnote.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * BasicTextField용 VisualTransformation
 * 텍스트 내용은 변경하지 않고 시각적 스타일(색상, 굵기 등)만 적용
 */
class SyntaxHighlightTransformation(
    private val language: LanguageDefinition,
    private val theme: SyntaxTheme
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        if (rawText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(rawText)

        // 이미 스타일이 적용된 위치 추적 (중복 방지)
        val claimed = BooleanArray(rawText.length)

        // 규칙 순서대로 매칭 - 먼저 매칭된 규칙이 우선
        // (주석/문자열을 먼저 정의하여 키워드와 충돌 방지)
        for (rule in language.rules) {
            val style = theme.styles[rule.tokenType] ?: continue

            try {
                val matches = rule.pattern.findAll(rawText)
                for (match in matches) {
                    val start = match.range.first
                    val end = match.range.last + 1 // exclusive

                    // 이미 다른 규칙에서 매칭된 영역이면 건너뛰기
                    var overlap = false
                    for (i in start until end.coerceAtMost(rawText.length)) {
                        if (claimed[i]) {
                            overlap = true
                            break
                        }
                    }
                    if (overlap) continue

                    // 스타일 적용
                    builder.addStyle(style, start, end.coerceAtMost(rawText.length))

                    // 영역 표시
                    for (i in start until end.coerceAtMost(rawText.length)) {
                        claimed[i] = true
                    }
                }
            } catch (_: Exception) {
                // 정규식 매칭 에러 무시 (안전성)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
