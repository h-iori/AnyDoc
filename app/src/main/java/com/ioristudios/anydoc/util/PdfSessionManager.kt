package com.ioristudios.anydoc.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

data class PdfPageFrame(
    val bitmap: Bitmap,
    val widthPts: Float,
    val heightPts: Float
)

/**
 * Manages a persistent PdfRenderer session for the currently viewed document.
 * Thread-safe: serializes all native calls to avoid concurrency issues with Android's PdfRenderer.
 */
class PdfSessionManager private constructor() {
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private var currentPath: String? = null

    private val renderDispatcher: CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "PdfRenderWorker").apply { priority = Thread.NORM_PRIORITY + 1 }
        }.asCoroutineDispatcher()

    var pageCount: Int = 0
        private set
    var defaultAspectRatio: Float = 1.414f
        private set

    companion object {
        val instance = PdfSessionManager()
    }

    /**
     * Opens the PDF file descriptor and reads metadata in O(1) time (< 15ms).
     */
    suspend fun open(filePath: String): Int = withContext(renderDispatcher) {
        if (currentPath == filePath && renderer != null) {
            return@withContext pageCount
        }
        closeInternal()
        val file = File(filePath)
        if (!file.exists()) {
            return@withContext 0
        }
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(descriptor)

        pfd = descriptor
        renderer = pdfRenderer
        currentPath = filePath
        pageCount = pdfRenderer.pageCount

        if (pageCount > 0) {
            runCatching {
                pdfRenderer.openPage(0).use { firstPage ->
                    if (firstPage.width > 0) {
                        defaultAspectRatio = firstPage.height.toFloat() / firstPage.width.toFloat()
                    }
                }
            }
        }
        pageCount
    }

    /**
     * Renders a specific page into a pooled or newly created Bitmap.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int): PdfPageFrame? = withContext(renderDispatcher) {
        val pdfRenderer = renderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

        runCatching {
            pdfRenderer.openPage(pageIndex).use { page ->
                val scale = targetWidth.toFloat() / page.width.toFloat()
                val targetHeight = (page.height * scale).toInt()

                val bitmap = BitmapPool.acquire(targetWidth, targetHeight)
                    ?: Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)

                Canvas(bitmap).drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                PdfPageFrame(bitmap, page.width.toFloat(), page.height.toFloat())
            }
        }.getOrNull()
    }

    suspend fun close() = withContext(renderDispatcher) {
        closeInternal()
    }

    private fun closeInternal() {
        runCatching { renderer?.close() }
        runCatching { pfd?.close() }
        renderer = null
        pfd = null
        currentPath = null
        pageCount = 0
    }
}
