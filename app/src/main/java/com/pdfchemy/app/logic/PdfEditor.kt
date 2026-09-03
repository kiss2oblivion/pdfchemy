package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

enum class EditorTool {
    VIEW,
    PEN,
    HIGHLIGHTER,
    TEXT,
    STAMP,
    REDACT
}

enum class StampType(val text: String, val colorHex: Long) {
    APPROVED("APPROVED", 0xFF2E7D32),
    CONFIDENTIAL("CONFIDENTIAL", 0xFFC62828),
    DRAFT("DRAFT", 0xFFEF6C00),
    PAID("PAID", 0xFF1565C0),
    REJECTED("REJECTED", 0xFFB71C1C),
    FINAL("FINAL", 0xFF4527A0),
    URGENT("URGENT", 0xFFD84315)
}

data class DrawingPoint(val x: Float, val y: Float)

data class DrawingPath(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DrawingPoint>,
    val color: Int,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

data class TextAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val fontSize: Float = 16f,
    val textColor: Int = android.graphics.Color.BLACK,
    val backgroundColor: Int = android.graphics.Color.TRANSPARENT
)

data class StampAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val type: StampType,
    val xRatio: Float,
    val yRatio: Float,
    val scale: Float = 1.0f,
    val rotation: Float = -15f
)

data class PageModification(
    val pageIndex: Int,
    val rotationDegrees: Int = 0,
    val isDeleted: Boolean = false,
    val drawings: List<DrawingPath> = emptyList(),
    val textAnnotations: List<TextAnnotation> = emptyList(),
    val stamps: List<StampAnnotation> = emptyList(),
    val redactions: List<RedactionBox> = emptyList()
) {
    val hasChanges: Boolean
        get() = rotationDegrees != 0 || isDeleted || drawings.isNotEmpty() || textAnnotations.isNotEmpty() || stamps.isNotEmpty() || redactions.isNotEmpty()
}

object PdfEditor {

    /**
     * Gets the total number of pages in the PDF document.
     */
    fun getPageCount(context: Context, uri: Uri): Int {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = openParcelFileDescriptor(context, uri) ?: return 0
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            AppLogger.e("PdfEditor: failed to get page count", e)
            0
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Renders a specific PDF page to an Android Bitmap for high-resolution interactive viewing.
     */
    suspend fun renderPageBitmap(
        context: Context,
        uri: Uri,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        try {
            pfd = openParcelFileDescriptor(context, uri) ?: return@withContext null
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

            page = renderer.openPage(pageIndex)
            val originalWidth = page.width
            val originalHeight = page.height

            val scale = (targetWidth.toFloat() / originalWidth.toFloat()).coerceAtLeast(1.0f)
            val renderWidth = (originalWidth * scale).toInt()
            val renderHeight = (originalHeight * scale).toInt()

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            // Fill with pure white background for transparent PDF pages
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            AppLogger.e("PdfEditor: failed to render page $pageIndex", e)
            null
        } finally {
            try {
                page?.close()
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    data class DualPageBitmaps(
        val leftPage: Bitmap?,
        val rightPage: Bitmap?
    )

    /**
     * Renders two consecutive pages side-by-side for Foldable dual-screen and tablet book reading.
     */
    suspend fun renderDualPageBitmaps(
        context: Context,
        uri: Uri,
        leftIndex: Int,
        rightIndex: Int,
        targetWidth: Int = 720
    ): DualPageBitmaps = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var leftBmp: Bitmap? = null
        var rightBmp: Bitmap? = null
        try {
            pfd = openParcelFileDescriptor(context, uri) ?: return@withContext DualPageBitmaps(null, null)
            renderer = PdfRenderer(pfd)
            val total = renderer.pageCount

            if (leftIndex in 0 until total) {
                val page = renderer.openPage(leftIndex)
                val origW = page.width.coerceAtLeast(1)
                val origH = page.height.coerceAtLeast(1)
                val scale = (targetWidth.toFloat() / origW).coerceIn(0.5f, 2.0f)
                val rw = (origW * scale).toInt().coerceAtLeast(1)
                val rh = (origH * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                leftBmp = bmp
            }

            if (rightIndex in 0 until total) {
                val page = renderer.openPage(rightIndex)
                val origW = page.width.coerceAtLeast(1)
                val origH = page.height.coerceAtLeast(1)
                val scale = (targetWidth.toFloat() / origW).coerceIn(0.5f, 2.0f)
                val rw = (origW * scale).toInt().coerceAtLeast(1)
                val rh = (origH * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                rightBmp = bmp
            }
            DualPageBitmaps(leftBmp, rightBmp)
        } catch (e: Exception) {
            AppLogger.e("PdfEditor: failed to render dual pages ($leftIndex, $rightIndex)", e)
            DualPageBitmaps(leftBmp, rightBmp)
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Applies all user modifications (drawings, text boxes, stamps, page rotations, and deletions)
     * and saves the output to the destination URI without corrupting original vector content.
     */
    suspend fun exportModifiedPdf(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        modifications: Map<Int, PageModification>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open source PDF"))

            document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages

            // 1. Process annotations and rotations from last page to first to support safe deletions
            for (pageIdx in (totalPages - 1) downTo 0) {
                val mod = modifications[pageIdx] ?: continue

                if (mod.isDeleted) {
                    document.removePage(pageIdx)
                    continue
                }

                val page = document.getPage(pageIdx)

                // Apply rotation
                if (mod.rotationDegrees != 0) {
                    val currentRotation = page.rotation
                    page.rotation = (currentRotation + mod.rotationDegrees) % 360
                }

                // Apply annotations overlay if any drawings, texts, stamps, or redactions exist
                if (mod.drawings.isNotEmpty() || mod.textAnnotations.isNotEmpty() || mod.stamps.isNotEmpty() || mod.redactions.isNotEmpty()) {
                    val cropBox = page.cropBox ?: page.mediaBox
                    val pageWidthPts = cropBox.width
                    val pageHeightPts = cropBox.height

                    // Render overlay to a crisp 2x resolution bitmap
                    val overlayBmp = renderAnnotationOverlayBitmap(
                        mod = mod,
                        targetWidth = (pageWidthPts * 2).toInt(),
                        targetHeight = (pageHeightPts * 2).toInt()
                    )

                    if (overlayBmp != null) {
                        val pdImage = LosslessFactory.createFromImage(document, overlayBmp)
                        val contentStream = PDPageContentStream(
                            document,
                            page,
                            PDPageContentStream.AppendMode.APPEND,
                            true,
                            true
                        )
                        contentStream.use { cs ->
                            cs.drawImage(pdImage, 0f, 0f, pageWidthPts, pageHeightPts)
                        }
                        overlayBmp.recycle()
                    }
                }
            }

            val outputStream: OutputStream = context.contentResolver.openOutputStream(destUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open output stream"))

            outputStream.use { out ->
                document.save(out)
                out.flush()
            }

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfEditor: failed to export modified PDF", e)
            Result.failure(e)
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Renders user drawings, text boxes, and stamps onto a transparent overlay bitmap.
     */
    fun renderAnnotationOverlayBitmap(
        mod: PageModification,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null
        val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Render Freehand Drawings & Highlighters
        val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        for (drawing in mod.drawings) {
            if (drawing.points.size < 2) continue
            pathPaint.color = drawing.color
            pathPaint.strokeWidth = drawing.strokeWidth * (targetWidth / 1000f).coerceAtLeast(1f)
            if (drawing.isHighlighter) {
                pathPaint.alpha = 110 // Translucent highlighter
            }

            val path = Path()
            val p0 = drawing.points[0]
            path.moveTo(p0.x * targetWidth, p0.y * targetHeight)
            for (i in 1 until drawing.points.size) {
                val p = drawing.points[i]
                path.lineTo(p.x * targetWidth, p.y * targetHeight)
            }
            canvas.drawPath(path, pathPaint)
        }

        // 2. Render Text Box Annotations
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFakeBoldText = true
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (textAnn in mod.textAnnotations) {
            if (textAnn.text.isBlank()) continue
            val posX = textAnn.xRatio * targetWidth
            val posY = textAnn.yRatio * targetHeight
            val scaleFactor = (targetWidth / 1000f).coerceAtLeast(1f)
            val textSize = textAnn.fontSize * scaleFactor * 2.2f
            textPaint.textSize = textSize
            textPaint.color = textAnn.textColor

            val textWidth = textPaint.measureText(textAnn.text)
            val textHeight = textSize

            if (textAnn.backgroundColor != android.graphics.Color.TRANSPARENT) {
                bgPaint.color = textAnn.backgroundColor
                val padding = 8f * scaleFactor
                val rect = RectF(
                    posX - padding,
                    posY - textHeight - padding,
                    posX + textWidth + padding,
                    posY + padding
                )
                canvas.drawRoundRect(rect, 8f, 8f, bgPaint)
            }

            canvas.drawText(textAnn.text, posX, posY, textPaint)
        }

        // 3. Render Stamps
        val stampBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f * (targetWidth / 1000f).coerceAtLeast(1f)
        }
        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        for (stamp in mod.stamps) {
            val posX = stamp.xRatio * targetWidth
            val posY = stamp.yRatio * targetHeight
            val scaleFactor = (targetWidth / 1000f).coerceAtLeast(1f) * stamp.scale

            val color = stamp.type.colorHex.toInt()
            stampBorderPaint.color = color
            stampTextPaint.color = color
            stampTextPaint.textSize = 28f * scaleFactor

            val text = stamp.type.text
            val textWidth = stampTextPaint.measureText(text)
            val boxWidth = textWidth + (32f * scaleFactor)
            val boxHeight = 54f * scaleFactor

            canvas.save()
            canvas.translate(posX, posY)
            canvas.rotate(stamp.rotation)

            val stampRect = RectF(-boxWidth / 2f, -boxHeight / 2f, boxWidth / 2f, boxHeight / 2f)
            canvas.drawRoundRect(stampRect, 10f * scaleFactor, 10f * scaleFactor, stampBorderPaint)
            canvas.drawText(text, 0f, (boxHeight / 4f), stampTextPaint)

            canvas.restore()
        }

        // 4. Render Redactions (Solid Black Opaque Blocks)
        val redactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        val redactTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        for (redaction in mod.redactions) {
            val norm = redaction.normalizedRect
            val left = norm.left * targetWidth
            val top = norm.top * targetHeight
            val right = norm.right * targetWidth
            val bottom = norm.bottom * targetHeight
            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, redactPaint)

            if (!redaction.overlayLabel.isNullOrBlank() && rect.width() > 50 && rect.height() > 14) {
                val fontSize = (rect.height() * 0.45f).coerceIn(8f, 20f)
                redactTextPaint.textSize = fontSize
                canvas.drawText(redaction.overlayLabel, rect.centerX(), rect.centerY() + (fontSize / 3f), redactTextPaint)
            }
        }

        return bitmap
    }

    private fun openParcelFileDescriptor(context: Context, uri: Uri): ParcelFileDescriptor? {
        return try {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: return null)
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                // PdfRenderer strictly requires a seekable ParcelFileDescriptor.
                // Content providers frequently provide pipe-based or non-seekable descriptors.
                // Copying the stream to a temporary cache file guarantees full seekability and reliable rendering.
                val tempFile = File(context.cacheDir, "pdf_seekable_${System.currentTimeMillis()}.pdf")
                val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (copied != null && copied > 0 && tempFile.exists()) {
                    ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")
                }
            }
        } catch (e: Exception) {
            AppLogger.e("PdfEditor: failed to open parcel file descriptor", e)
            null
        }
    }
}
