package com.example.samnote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val presetColors = listOf(
    Color.Unspecified,  // Default / auto
    Color(0xFF000000),  // Black
    Color(0xFFFFFFFF),  // White
    Color(0xFFE53935),  // Red
    Color(0xFFD81B60),  // Pink
    Color(0xFF8E24AA),  // Purple
    Color(0xFF5E35B1),  // Deep Purple
    Color(0xFF3949AB),  // Indigo
    Color(0xFF1E88E5),  // Blue
    Color(0xFF039BE5),  // Light Blue
    Color(0xFF00ACC1),  // Cyan
    Color(0xFF00897B),  // Teal
    Color(0xFF43A047),  // Green
    Color(0xFF7CB342),  // Light Green
    Color(0xFFC0CA33),  // Lime
    Color(0xFFFDD835),  // Yellow
    Color(0xFFFFB300),  // Amber
    Color(0xFFFB8C00),  // Orange
    Color(0xFFF4511E),  // Deep Orange
    Color(0xFF6D4C41),  // Brown
    Color(0xFF757575),  // Grey
    Color(0xFF546E7A),  // Blue Grey
    Color(0xFF263238),  // Dark
    Color(0xFFFAFAFA),  // Near White
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = if (selectedColor == Color.Unspecified) "기본값" else "커스텀",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onColorSelected(selectedColor)
                onDismiss()
            }) {
                Text("확인")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val displayColor = if (color == Color.Unspecified) Color(0xFFE0E0E0) else color
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(displayColor)
            .border(3.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (color == Color.Unspecified) {
            Text(
                text = "자동",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
        if (isSelected && color != Color.Unspecified) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (isLightColor(color)) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}
