package com.example.samnote.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import com.example.samnote.data.TabInfo
import com.example.samnote.data.ThemeMode
import com.example.samnote.util.DocumentHtmlConverter
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class EditorViewModel : ViewModel() {

    private val _tabs = mutableStateListOf(TabInfo())
    val tabs: List<TabInfo> = _tabs

    var activeTabIndex by mutableIntStateOf(0)
        private set

    private var _themeMode by mutableStateOf(ThemeMode.SYSTEM)
    val themeMode: ThemeMode get() = _themeMode

    private var _fontSize by mutableFloatStateOf(14f)
    val fontSize: Float get() = _fontSize

    private var _fontFamily by mutableStateOf("Monospace")
    val fontFamily: String get() = _fontFamily

    private var _textColor by mutableStateOf(Color.Unspecified)
    val textColor: Color get() = _textColor

    private var _editorBgColor by mutableStateOf(Color.Unspecified)
    val editorBgColor: Color get() = _editorBgColor

    private var _isMarkdownPreview by mutableStateOf(false)
    val isMarkdownPreview: Boolean get() = _isMarkdownPreview

    val activeTab: TabInfo?
        get() = _tabs.getOrNull(activeTabIndex)

    fun addTab(tab: TabInfo = TabInfo()) {
        _tabs.add(tab)
        activeTabIndex = _tabs.size - 1
    }

    fun closeTab(index: Int) {
        if (_tabs.size <= 1) return
        _tabs.removeAt(index)
        if (activeTabIndex >= _tabs.size) {
            activeTabIndex = _tabs.size - 1
        }
    }

    fun selectTab(index: Int) {
        if (index in _tabs.indices) {
            activeTabIndex = index
        }
    }

    fun updateContent(textFieldValue: TextFieldValue) {
        val tab = _tabs.getOrNull(activeTabIndex) ?: return
        _tabs[activeTabIndex] = tab.copy(
            content = textFieldValue,
            isModified = true
        )
    }

    fun openFile(context: Context, uri: Uri) {
        try {
            val fileName = getFileName(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val ext = fileName.substringAfterLast('.', "").lowercase()

            val existingIndex = _tabs.indexOfFirst { it.uri == uri }
            if (existingIndex >= 0) {
                activeTabIndex = existingIndex
                return
            }

            // PDF는 바로 PdfViewer로 표시
            if (ext == "pdf") {
                val tab = TabInfo(
                    fileName = fileName,
                    uri = uri,
                    content = TextFieldValue(""),
                    isEditable = false
                )
                _tabs.add(tab)
                activeTabIndex = _tabs.size - 1
                return
            }

            // 문서 파일 → HTML 변환 → WebView로 바로 표시
            val documentExtensions = setOf(
                "doc", "docx", "ppt", "pptx", "hwp", "hwpx",
                "xls", "xlsx"
            )
            if (ext in documentExtensions) {
                val html = when (ext) {
                    "docx" -> DocumentHtmlConverter.convertDocx(context, uri)
                    "pptx" -> DocumentHtmlConverter.convertPptx(context, uri)
                    "xls", "xlsx" -> DocumentHtmlConverter.convertXls(context, uri, ext)
                    else -> DocumentHtmlConverter.convertLegacyDocument(context, uri, ext)
                }
                val tab = TabInfo(
                    fileName = fileName,
                    uri = uri,
                    content = TextFieldValue(""),
                    isEditable = false,
                    htmlContent = html
                )
                _tabs.add(tab)
                activeTabIndex = _tabs.size - 1
                return
            }

            // 텍스트 파일
            val isEditable = isTextFile(fileName, mimeType)
            val content = if (isEditable || mimeType.startsWith("text/")) {
                readTextContent(context, uri)
            } else {
                "[이 파일 형식은 텍스트로 표시할 수 없습니다: $fileName]"
            }

            val tab = TabInfo(
                fileName = fileName,
                uri = uri,
                content = TextFieldValue(content),
                isEditable = isEditable
            )
            _tabs.add(tab)
            activeTabIndex = _tabs.size - 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveFile(context: Context, uri: Uri? = null) {
        val tab = _tabs.getOrNull(activeTabIndex) ?: return
        val targetUri = uri ?: tab.uri ?: return

        try {
            context.contentResolver.openOutputStream(targetUri, "wt")?.use { outputStream ->
                outputStream.write(tab.content.text.toByteArray(Charsets.UTF_8))
            }
            _tabs[activeTabIndex] = tab.copy(
                isModified = false,
                uri = targetUri,
                fileName = if (uri != null) getFileName(context, targetUri) else tab.fileName
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTheme(mode: ThemeMode) {
        _themeMode = mode
    }

    fun setFontSize(size: Float) {
        _fontSize = size.coerceIn(8f, 36f)
    }

    fun setFontFamily(family: String) {
        _fontFamily = family
    }

    fun setTextColor(color: Color) {
        _textColor = color
    }

    fun setEditorBgColor(color: Color) {
        _editorBgColor = color
    }

    fun toggleMarkdownPreview() {
        _isMarkdownPreview = !_isMarkdownPreview
    }

    private fun readTextContent(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
        } ?: ""
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun isTextFile(fileName: String, mimeType: String): Boolean {
        if (mimeType.startsWith("text/")) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val textExtensions = setOf(
            "txt", "md", "c", "cpp", "h", "hpp", "java", "kt", "kts",
            "py", "js", "ts", "jsx", "tsx", "html", "htm", "css", "scss",
            "xml", "json", "yaml", "yml", "toml", "ini", "cfg", "conf",
            "sh", "bash", "zsh", "bat", "cmd", "ps1",
            "sql", "rb", "rs", "go", "swift", "dart", "lua",
            "r", "m", "pl", "php", "csv", "tsv", "log",
            "gradle", "properties", "gitignore", "env"
        )
        return ext in textExtensions
    }

    // ── 상태 저장/복원 ──────────────────────────────────

    private companion object {
        const val PREFS_NAME = "samnote_state"
        const val KEY_TABS = "tabs"
        const val KEY_ACTIVE_INDEX = "active_index"
        const val KEY_THEME = "theme_mode"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_FONT_FAMILY = "font_family"
        const val KEY_TEXT_COLOR = "text_color"
        const val KEY_BG_COLOR = "bg_color"
    }

    fun saveState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            // 탭 저장
            val tabsJson = JSONArray()
            for (tab in _tabs) {
                val obj = JSONObject().apply {
                    put("fileName", tab.fileName)
                    put("uri", tab.uri?.toString() ?: "")
                    put("content", tab.content.text)
                    put("isEditable", tab.isEditable)
                    put("isModified", tab.isModified)
                }
                tabsJson.put(obj)
            }
            editor.putString(KEY_TABS, tabsJson.toString())
            editor.putInt(KEY_ACTIVE_INDEX, activeTabIndex)

            // 설정 저장
            editor.putString(KEY_THEME, _themeMode.name)
            editor.putFloat(KEY_FONT_SIZE, _fontSize)
            editor.putString(KEY_FONT_FAMILY, _fontFamily)
            if (_textColor != Color.Unspecified) {
                editor.putLong(KEY_TEXT_COLOR, _textColor.value.toLong())
            }
            if (_editorBgColor != Color.Unspecified) {
                editor.putLong(KEY_BG_COLOR, _editorBgColor.value.toLong())
            }

            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun restoreState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.contains(KEY_TABS)) return

            // 설정 복원
            prefs.getString(KEY_THEME, null)?.let {
                _themeMode = try { ThemeMode.valueOf(it) } catch (_: Exception) { ThemeMode.SYSTEM }
            }
            _fontSize = prefs.getFloat(KEY_FONT_SIZE, 14f)
            prefs.getString(KEY_FONT_FAMILY, null)?.let { _fontFamily = it }
            if (prefs.contains(KEY_TEXT_COLOR)) {
                _textColor = Color(prefs.getLong(KEY_TEXT_COLOR, 0).toULong())
            }
            if (prefs.contains(KEY_BG_COLOR)) {
                _editorBgColor = Color(prefs.getLong(KEY_BG_COLOR, 0).toULong())
            }

            // 탭 복원
            val tabsStr = prefs.getString(KEY_TABS, null) ?: return
            val tabsJson = JSONArray(tabsStr)
            if (tabsJson.length() == 0) return

            val restoredTabs = mutableListOf<TabInfo>()
            for (i in 0 until tabsJson.length()) {
                val obj = tabsJson.getJSONObject(i)
                val uriStr = obj.optString("uri", "")
                val uri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null
                val fileName = obj.getString("fileName")
                val ext = fileName.substringAfterLast('.', "").lowercase()

                // URI가 있는 파일은 다시 열기 (내용을 새로 읽음)
                if (uri != null) {
                    try {
                        if (ext == "pdf") {
                            restoredTabs.add(
                                TabInfo(
                                    fileName = fileName,
                                    uri = uri,
                                    content = TextFieldValue(""),
                                    isEditable = false
                                )
                            )
                        } else {
                            val docExts = setOf("doc", "docx", "ppt", "pptx", "hwp", "hwpx", "xls", "xlsx")
                            if (ext in docExts) {
                                val html = when (ext) {
                                    "docx" -> DocumentHtmlConverter.convertDocx(context, uri)
                                    "pptx" -> DocumentHtmlConverter.convertPptx(context, uri)
                                    "xls", "xlsx" -> DocumentHtmlConverter.convertXls(context, uri, ext)
                                    else -> DocumentHtmlConverter.convertLegacyDocument(context, uri, ext)
                                }
                                restoredTabs.add(
                                    TabInfo(
                                        fileName = fileName,
                                        uri = uri,
                                        content = TextFieldValue(""),
                                        isEditable = false,
                                        htmlContent = html
                                    )
                                )
                            } else {
                                val content = readTextContent(context, uri)
                                restoredTabs.add(
                                    TabInfo(
                                        fileName = fileName,
                                        uri = uri,
                                        content = TextFieldValue(content),
                                        isEditable = obj.optBoolean("isEditable", true),
                                        isModified = obj.optBoolean("isModified", false)
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // URI 접근 실패 시 건너뛰기
                        e.printStackTrace()
                    }
                } else {
                    // URI 없는 새 문서 → 저장된 내용 복원
                    val content = obj.optString("content", "")
                    restoredTabs.add(
                        TabInfo(
                            fileName = fileName,
                            content = TextFieldValue(content),
                            isEditable = obj.optBoolean("isEditable", true),
                            isModified = obj.optBoolean("isModified", false)
                        )
                    )
                }
            }

            if (restoredTabs.isNotEmpty()) {
                _tabs.clear()
                _tabs.addAll(restoredTabs)
                activeTabIndex = prefs.getInt(KEY_ACTIVE_INDEX, 0)
                    .coerceIn(0, _tabs.size - 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
