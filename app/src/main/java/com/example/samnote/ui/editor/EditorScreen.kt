package com.example.samnote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.samnote.data.ThemeMode
import com.example.samnote.syntax.DarkSyntaxTheme
import com.example.samnote.syntax.LanguageRegistry
import com.example.samnote.syntax.LightSyntaxTheme
import com.example.samnote.syntax.SyntaxHighlightTransformation
import com.example.samnote.ui.theme.ModifiedDot
import com.example.samnote.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onSaveAsFile: () -> Unit,
    onShowSettings: () -> Unit
) {
    val activeTab = viewModel.activeTab

    // 다크 모드 판별
    val isDark = when (viewModel.themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // 구문 강조 VisualTransformation 생성
    val syntaxTransformation = remember(
        activeTab?.content?.text,
        activeTab?.fileExtension,
        isDark
    ) {
        val ext = activeTab?.fileExtension ?: ""
        val lang = LanguageRegistry.forExtension(ext)
        if (lang != null) {
            val theme = if (isDark) DarkSyntaxTheme else LightSyntaxTheme
            SyntaxHighlightTransformation(lang, theme)
        } else {
            VisualTransformation.None
        }
    }

    // 폰트 패밀리 결정
    val resolvedFontFamily = when (viewModel.fontFamily) {
        "Monospace" -> FontFamily.Monospace
        "Sans Serif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        else -> FontFamily.Default
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "SamNote",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            actions = {
                // 마크다운 미리보기 토글 버튼
                if (activeTab?.isMarkdown == true) {
                    IconButton(onClick = { viewModel.toggleMarkdownPreview() }) {
                        Icon(
                            imageVector = if (viewModel.isMarkdownPreview)
                                Icons.Default.Edit
                            else
                                Icons.Default.Visibility,
                            contentDescription = if (viewModel.isMarkdownPreview)
                                "편집 모드"
                            else
                                "미리보기"
                        )
                    }
                }
                IconButton(onClick = { viewModel.addTab() }) {
                    Icon(Icons.Default.Add, contentDescription = "새 탭")
                }
                IconButton(onClick = onOpenFile) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "파일 열기")
                }
                if (activeTab?.isEditable == true && !viewModel.isMarkdownPreview) {
                    IconButton(onClick = onSaveFile) {
                        Icon(Icons.Default.Save, contentDescription = "저장")
                    }
                    IconButton(onClick = onSaveAsFile) {
                        Icon(Icons.Default.SaveAs, contentDescription = "다른 이름으로 저장")
                    }
                }
                IconButton(onClick = onShowSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "설정")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.primary
            )
        )

        // Tab Row
        if (viewModel.tabs.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = viewModel.activeTabIndex,
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    if (viewModel.activeTabIndex < tabPositions.size) {
                        SecondaryIndicator(
                            modifier = Modifier.customTabIndicatorOffset(
                                tabPositions[viewModel.activeTabIndex]
                            ),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                viewModel.tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = index == viewModel.activeTabIndex,
                        onClick = { viewModel.selectTab(index) },
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            if (tab.isModified) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            ModifiedDot,
                                            shape = MaterialTheme.shapes.extraSmall
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = tab.fileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index == viewModel.activeTabIndex)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (viewModel.tabs.size > 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.closeTab(index) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "탭 닫기",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        // Editor / PDF Viewer / Document Viewer / Markdown Preview Area
        if (activeTab != null) {
            if (activeTab.isPdf && activeTab.uri != null) {
                // PDF 뷰어
                PdfViewer(
                    uri = activeTab.uri!!,
                    themeMode = viewModel.themeMode,
                    modifier = Modifier.weight(1f)
                )
            } else if (activeTab.htmlContent != null) {
                // 문서 뷰어 (DOCX, PPTX, XLS 등 → HTML → WebView)
                DocumentViewer(
                    htmlContent = activeTab.htmlContent!!,
                    themeMode = viewModel.themeMode,
                    modifier = Modifier.weight(1f)
                )
            } else if (activeTab.isMarkdown && viewModel.isMarkdownPreview) {
                // 마크다운 미리보기 모드
                MarkdownPreview(
                    text = activeTab.content.text,
                    fontSize = viewModel.fontSize.sp,
                    fontFamily = resolvedFontFamily,
                    themeMode = viewModel.themeMode,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 코드 에디터 (구문 강조 적용)
                LineNumberEditor(
                    textFieldValue = activeTab.content,
                    onValueChange = { viewModel.updateContent(it) },
                    isEditable = activeTab.isEditable,
                    fontSize = viewModel.fontSize.sp,
                    textColor = viewModel.textColor,
                    backgroundColor = viewModel.editorBgColor,
                    fontFamily = resolvedFontFamily,
                    themeMode = viewModel.themeMode,
                    visualTransformation = syntaxTransformation,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Status Bar
        if (activeTab != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            StatusBar(
                lineCount = remember(activeTab.content.text) {
                    if (activeTab.content.text.isEmpty()) 1
                    else activeTab.content.text.count { it == '\n' } + 1
                },
                charCount = activeTab.content.text.length,
                isEditable = activeTab.isEditable,
                isModified = activeTab.isModified,
                languageName = remember(activeTab.fileExtension) {
                    val ext = activeTab.fileExtension
                    when (ext) {
                        "pdf" -> "PDF"
                        "doc", "docx" -> "Word"
                        "ppt", "pptx" -> "PowerPoint"
                        "hwp", "hwpx" -> "HWP"
                        "xls", "xlsx" -> "Excel"
                        else -> if (ext.isNotEmpty())
                            LanguageRegistry.languageName(ext)
                        else ""
                    }
                },
                isPreviewMode = activeTab.isMarkdown && viewModel.isMarkdownPreview
            )
        }
    }
}

@Composable
private fun StatusBar(
    lineCount: Int,
    charCount: Int,
    isEditable: Boolean,
    isModified: Boolean,
    languageName: String,
    isPreviewMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "$lineCount 줄",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$charCount 글자",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isPreviewMode) {
                Text(
                    text = "미리보기",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!isEditable && !isPreviewMode) {
                Text(
                    text = "읽기 전용",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (isModified) {
                Text(
                    text = "수정됨",
                    style = MaterialTheme.typography.labelSmall,
                    color = ModifiedDot
                )
            }
            if (languageName.isNotEmpty()) {
                Text(
                    text = languageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "UTF-8",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "customTabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth = currentTabPosition.width
    val indicatorOffset = currentTabPosition.left
    fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset)
        .width(currentTabWidth)
}
