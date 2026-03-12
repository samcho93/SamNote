package com.example.samnote.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samnote.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    fontFamily: String,
    onFontFamilyChange: (String) -> Unit,
    textColor: Color,
    onTextColorChange: (Color) -> Unit,
    bgColor: Color,
    onBgColorChange: (Color) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "설정",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Theme section
            SettingSectionHeader(
                icon = Icons.Default.ColorLens,
                title = "테마"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    label = { Text("시스템") },
                    leadingIcon = {
                        Icon(Icons.Default.PhoneAndroid, null, Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    label = { Text("라이트") },
                    leadingIcon = {
                        Icon(Icons.Default.LightMode, null, Modifier.size(18.dp))
                    }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    label = { Text("다크") },
                    leadingIcon = {
                        Icon(Icons.Default.DarkMode, null, Modifier.size(18.dp))
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Font size section
            SettingSectionHeader(
                icon = Icons.Default.FormatSize,
                title = "글자 크기"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${fontSize.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(48.dp)
                )
                Slider(
                    value = fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 8f..36f,
                    steps = 27,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Text(
                text = "미리보기: 가나다라마바사 ABCDEF 0123456789",
                fontSize = fontSize.sp,
                fontFamily = when (fontFamily) {
                    "Monospace" -> FontFamily.Monospace
                    "Sans Serif" -> FontFamily.SansSerif
                    "Serif" -> FontFamily.Serif
                    else -> FontFamily.Default
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Font family section
            SettingSectionHeader(
                icon = Icons.Default.FontDownload,
                title = "글꼴"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Monospace", "Sans Serif", "Serif", "Default").forEach { family ->
                    FilterChip(
                        selected = fontFamily == family,
                        onClick = { onFontFamilyChange(family) },
                        label = { Text(family) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Text color section
            SettingSectionHeader(
                icon = Icons.Default.FormatColorText,
                title = "글자색"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTextColorPicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (textColor == Color.Unspecified) "기본값 (자동)" else "커스텀",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text("변경 >", color = MaterialTheme.colorScheme.primary)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Background color section
            SettingSectionHeader(
                icon = Icons.Default.FormatColorFill,
                title = "배경색"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBgColorPicker = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bgColor == Color.Unspecified) "기본값 (자동)" else "커스텀",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text("변경 >", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTextColorPicker) {
        ColorPickerDialog(
            title = "글자색 선택",
            currentColor = textColor,
            onColorSelected = onTextColorChange,
            onDismiss = { showTextColorPicker = false }
        )
    }

    if (showBgColorPicker) {
        ColorPickerDialog(
            title = "배경색 선택",
            currentColor = bgColor,
            onColorSelected = onBgColorChange,
            onDismiss = { showBgColorPicker = false }
        )
    }
}

@Composable
private fun SettingSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
