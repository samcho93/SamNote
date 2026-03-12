package com.example.samnote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samnote.data.ThemeMode
import com.example.samnote.ui.theme.DividerDark
import com.example.samnote.ui.theme.DividerLight
import com.example.samnote.ui.theme.EditorBgDark
import com.example.samnote.ui.theme.EditorBgLight
import com.example.samnote.ui.theme.LineNumberBgDark
import com.example.samnote.ui.theme.LineNumberBgLight
import com.example.samnote.ui.theme.LineNumberTextDark
import com.example.samnote.ui.theme.LineNumberTextLight

@Composable
fun LineNumberEditor(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isEditable: Boolean,
    fontSize: TextUnit = 14.sp,
    textColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified,
    fontFamily: FontFamily = FontFamily.Monospace,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val lineNumberBg = if (isDark) LineNumberBgDark else LineNumberBgLight
    val lineNumberTextColor = if (isDark) LineNumberTextDark else LineNumberTextLight
    val dividerColor = if (isDark) DividerDark else DividerLight
    val editorBg = if (backgroundColor != Color.Unspecified) backgroundColor
        else if (isDark) EditorBgDark else EditorBgLight
    val resolvedTextColor = if (textColor != Color.Unspecified) textColor
        else MaterialTheme.colorScheme.onSurface

    val text = textFieldValue.text
    val lineCount = remember(text) {
        if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    }
    val lineNumberWidth = remember(lineCount) {
        lineCount.toString().length
    }
    val lineNumbersText = remember(lineCount, lineNumberWidth) {
        (1..lineCount).joinToString("\n") { it.toString().padStart(lineNumberWidth) }
    }

    val lineHeight = fontSize * 1.5f
    val textStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = resolvedTextColor
    )
    val lineNumberStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = lineNumberTextColor
    )

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Row(modifier = modifier.fillMaxSize()) {
        // Line numbers column
        Text(
            text = lineNumbersText,
            style = lineNumberStyle,
            modifier = Modifier
                .background(lineNumberBg)
                .verticalScroll(verticalScrollState)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(min = 32.dp)
        )

        // Divider
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = dividerColor
        )

        // Editor area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(editorBg)
        ) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = onValueChange,
                readOnly = !isEditable,
                textStyle = textStyle,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
