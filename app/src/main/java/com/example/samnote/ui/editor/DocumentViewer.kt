package com.example.samnote.ui.editor

import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.samnote.data.ThemeMode

/**
 * WebView 기반 문서 뷰어
 * HTML로 변환된 문서를 렌더링
 */
@Composable
fun DocumentViewer(
    htmlContent: String,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // 다크 모드일 때 color-scheme 메타 태그 주입
    val finalHtml = remember(htmlContent, isDark) {
        if (isDark) {
            htmlContent.replace(
                "<head>",
                "<head><meta name=\"color-scheme\" content=\"dark\">"
            )
        } else {
            htmlContent
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    textZoom = 100
                    defaultTextEncodingName = "UTF-8"
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    settings.isAlgorithmicDarkeningAllowed = isDark
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    settings.forceDark = if (isDark)
                        WebSettings.FORCE_DARK_ON
                    else
                        WebSettings.FORCE_DARK_OFF
                }
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                null,
                finalHtml,
                "text/html",
                "UTF-8",
                null
            )
        },
        modifier = modifier.fillMaxSize()
    )
}
