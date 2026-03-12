package com.example.samnote.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

/**
 * HTML을 오프스크린 WebView로 렌더링한 뒤 PDF 파일로 변환
 */
object HtmlToPdfConverter {

    suspend fun convert(context: Context, html: String): File? =
        suspendCancellableCoroutine { cont ->
            Handler(Looper.getMainLooper()).post {
                try {
                    val appContext = context.applicationContext
                    val webView = WebView(appContext)
                    webView.settings.apply {
                        javaScriptEnabled = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        defaultTextEncodingName = "UTF-8"
                    }

                    // 렌더링 너비 (A4 비율 기준)
                    val renderWidth = 1080

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String?) {
                            // WebView 렌더링 완료 후 약간의 딜레이
                            view.postDelayed({
                                try {
                                    val contentHeight =
                                        (view.contentHeight * view.scale).toInt().coerceAtLeast(1)

                                    // 레이아웃 측정
                                    view.measure(
                                        View.MeasureSpec.makeMeasureSpec(
                                            renderWidth,
                                            View.MeasureSpec.EXACTLY
                                        ),
                                        View.MeasureSpec.makeMeasureSpec(
                                            contentHeight,
                                            View.MeasureSpec.EXACTLY
                                        )
                                    )
                                    view.layout(0, 0, renderWidth, contentHeight)

                                    // PDF 크기 (A4 @ 72dpi → 포인트)
                                    val pdfWidth = 595
                                    val pdfHeight = 842
                                    val margin = 36
                                    val usableWidth = pdfWidth - 2 * margin
                                    val usableHeight = pdfHeight - 2 * margin

                                    val scaleFactor = usableWidth.toFloat() / renderWidth
                                    val scaledContentHeight =
                                        (contentHeight * scaleFactor).toInt()
                                    val numPages = ((scaledContentHeight + usableHeight - 1)
                                            / usableHeight).coerceAtLeast(1)

                                    val pdfDocument = PdfDocument()

                                    for (i in 0 until numPages) {
                                        val pageInfo = PdfDocument.PageInfo.Builder(
                                            pdfWidth, pdfHeight, i
                                        ).create()
                                        val page = pdfDocument.startPage(pageInfo)
                                        val canvas = page.canvas

                                        canvas.save()
                                        canvas.translate(margin.toFloat(), margin.toFloat())
                                        canvas.clipRect(0, 0, usableWidth, usableHeight)
                                        canvas.scale(scaleFactor, scaleFactor)
                                        canvas.translate(
                                            0f,
                                            -(i * usableHeight / scaleFactor)
                                        )
                                        view.draw(canvas)
                                        canvas.restore()

                                        pdfDocument.finishPage(page)
                                    }

                                    // 캐시 디렉터리에 저장
                                    val file = File(
                                        appContext.cacheDir,
                                        "doc_${System.currentTimeMillis()}.pdf"
                                    )
                                    FileOutputStream(file).use { pdfDocument.writeTo(it) }
                                    pdfDocument.close()

                                    cont.resume(file)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    cont.resume(null)
                                } finally {
                                    view.destroy()
                                }
                            }, 500)
                        }
                    }

                    cont.invokeOnCancellation {
                        webView.destroy()
                    }

                    webView.loadDataWithBaseURL(
                        null, html, "text/html", "UTF-8", null
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    cont.resume(null)
                }
            }
        }
}
