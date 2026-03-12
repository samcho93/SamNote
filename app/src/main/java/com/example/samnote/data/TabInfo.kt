package com.example.samnote.data

import android.net.Uri
import androidx.compose.ui.text.input.TextFieldValue
import java.io.File
import java.util.UUID

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class TabInfo(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "새 문서",
    val uri: Uri? = null,
    val content: TextFieldValue = TextFieldValue(""),
    val isModified: Boolean = false,
    val isEditable: Boolean = true,
    val htmlContent: String? = null,
    val pdfFile: File? = null,
    val isLoading: Boolean = false
) {
    val fileExtension: String
        get() = fileName.substringAfterLast('.', "").lowercase()

    val isMarkdown: Boolean
        get() = fileExtension in setOf("md", "markdown")

    val isPdf: Boolean
        get() = fileExtension == "pdf"

    val isDocumentFile: Boolean
        get() = fileExtension in setOf(
            "pdf", "doc", "docx", "ppt", "pptx", "hwp", "hwpx",
            "xls", "xlsx"
        )
}
