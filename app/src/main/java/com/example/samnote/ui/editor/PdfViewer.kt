package com.example.samnote.ui.editor

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.samnote.data.ThemeMode
import com.example.samnote.ui.theme.EditorBgDark
import com.example.samnote.ui.theme.EditorBgLight

/**
 * PdfRenderer를 사용하는 상태 홀더
 */
private class PdfState(
    val renderer: PdfRenderer,
    val fileDescriptor: android.os.ParcelFileDescriptor
) {
    val pageCount: Int get() = renderer.pageCount

    fun renderPage(pageIndex: Int, width: Int): Bitmap? {
        if (pageIndex < 0 || pageIndex >= pageCount || width <= 0) return null
        return try {
            val page = renderer.openPage(pageIndex)
            val scale = width.toFloat() / page.width.coerceAtLeast(1)
            val scaledHeight = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, scaledHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun close() {
        try {
            renderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun PdfViewer(
    uri: Uri? = null,
    pdfFile: File? = null,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val bgColor = if (isDark) EditorBgDark else EditorBgLight

    // PdfRenderer 열기 (File 또는 Uri)
    val pdfState = remember(uri, pdfFile) {
        try {
            val pfd = when {
                pdfFile != null -> ParcelFileDescriptor.open(
                    pdfFile, ParcelFileDescriptor.MODE_READ_ONLY
                )
                uri != null -> context.contentResolver.openFileDescriptor(uri, "r")
                else -> null
            }
            if (pfd != null) {
                PdfState(PdfRenderer(pfd), pfd)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 리소스 해제
    DisposableEffect(pdfState) {
        onDispose { pdfState?.close() }
    }

    // 에러 상태
    if (pdfState == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PDF 파일을 열 수 없습니다.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // 너비 측정
    var containerWidth by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { containerWidth = it.width },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(pdfState.pageCount) { pageIndex ->
            PdfPageItem(
                pdfState = pdfState,
                pageIndex = pageIndex,
                renderWidth = containerWidth,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun PdfPageItem(
    pdfState: PdfState,
    pageIndex: Int,
    renderWidth: Int,
    isDark: Boolean
) {
    val bitmap = remember(pageIndex, renderWidth) {
        if (renderWidth > 0) {
            pdfState.renderPage(pageIndex, renderWidth)
        } else null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // 페이지 이미지
        Surface(
            shape = RoundedCornerShape(4.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "페이지 ${pageIndex + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 페이지 번호
        Text(
            text = "${pageIndex + 1} / ${pdfState.pageCount}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }
}
