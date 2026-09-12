// =================================================================================================
// [FEATURE: Visual PDF Editor & Freehand Annotation Engine] (FEATURES_REGISTRY Android §4)
// Highlighting, freehand pen/pencil drawing, shapes, watermarking, and text annotations.
// =================================================================================================

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
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.InputStream
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
                try {
                    val origW = page.width.coerceAtLeast(1)
                    val origH = page.height.coerceAtLeast(1)
                    val scale = (targetWidth.toFloat() / origW).coerceIn(0.5f, 2.0f)
                    val rw = (origW * scale).toInt().coerceAtLeast(1)
                    val rh = (origH * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    leftBmp = bmp
                } finally {
                    page.close()
                }
            }

            if (rightIndex in 0 until total) {
                val page = renderer.openPage(rightIndex)
                try {
                    val origW = page.width.coerceAtLeast(1)
                    val origH = page.height.coerceAtLeast(1)
                    val scale = (targetWidth.toFloat() / origW).coerceIn(0.5f, 2.0f)
                    val rw = (origW * scale).toInt().coerceAtLeast(1)
                    val rh = (origH * scale).toInt().coerceAtLeast(1)
                    val bmp = Bitmap.createBitmap(rw, rh, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    rightBmp = bmp
                } finally {
                    page.close()
                }
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
        var inputStream: InputStream? = null
        var document: PDDocument? = null
        val hasAnyRedactions = modifications.values.any { it.redactions.isNotEmpty() }
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            if (hasAnyRedactions) {
                pfd = try { openParcelFileDescriptor(context, sourceUri) } catch (_: Exception) { null }
                if (pfd != null) {
                    renderer = try { PdfRenderer(pfd) } catch (_: Exception) { null }
                }
            }

            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(IllegalStateException("Cannot open source PDF"))

            document = PDDocument.load(inputStream, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
            val totalPages = document.numberOfPages

            // 1. Process annotations and rotations from last page to first to support safe deletions
            for (pageIdx in (totalPages - 1) downTo 0) {
                val mod = modifications[pageIdx] ?: continue

                if (mod.isDeleted) {
                    document.removePage(pageIdx)
                    continue
                }

                val page = document.getPage(pageIdx)

                // A. True Redaction: If page contains active redaction boxes, perform selective
                // single-page rasterization to permanently obliterate underlying text bytes from the PDF stream.
                if (mod.redactions.isNotEmpty()) {
                    var rasterized = false
                    if (renderer != null && pageIdx in 0 until renderer.pageCount) {
                        var renderPage: PdfRenderer.Page? = null
                        var baseBmp: Bitmap? = null
                        try {
                            renderPage = renderer.openPage(pageIdx)
                            val origW = renderPage.width.coerceAtLeast(1)
                            val origH = renderPage.height.coerceAtLeast(1)
                            val maxDim = 2400f
                            val scale = (maxDim / maxOf(origW, origH)).coerceIn(1.5f, 2.5f)
                            val targetW = (origW * scale).toInt().coerceAtLeast(1)
                            val targetH = (origH * scale).toInt().coerceAtLeast(1)

                            baseBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(baseBmp)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            renderPage.render(baseBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            // Render user drawings, text annotations, stamps, and redactions over base bitmap
                            val overlayBmp = renderAnnotationOverlayBitmap(mod, targetW, targetH)
                            if (overlayBmp != null) {
                                canvas.drawBitmap(overlayBmp, 0f, 0f, null)
                                overlayBmp.recycle()
                            }

                            // Apply optional user rotation on the flattened bitmap
                            var finalBmp = baseBmp
                            var finalW = origW
                            var finalH = origH
                            if (mod.rotationDegrees != 0) {
                                val matrix = android.graphics.Matrix()
                                matrix.postRotate(mod.rotationDegrees.toFloat())
                                val rotated = Bitmap.createBitmap(baseBmp, 0, 0, targetW, targetH, matrix, true)
                                if (rotated !== baseBmp) {
                                    baseBmp.recycle()
                                    finalBmp = rotated
                                }
                                if (mod.rotationDegrees == 90 || mod.rotationDegrees == 270) {
                                    finalW = origH
                                    finalH = origW
                                }
                            }

                            val pdImage = JPEGFactory.createFromImage(document, finalBmp, 0.92f)
                            if (finalBmp !== baseBmp) {
                                finalBmp.recycle()
                            } else {
                                baseBmp.recycle()
                            }

                            // Replace page content completely with the sanitized flattened image
                            page.rotation = 0
                            page.cropBox = null
                            page.mediaBox = PDRectangle(finalW.toFloat(), finalH.toFloat())
                            page.cosObject.removeItem(com.tom_roush.pdfbox.cos.COSName.ANNOTS)

                            PDPageContentStream(document, page, PDPageContentStream.AppendMode.OVERWRITE, false, false).use { cs ->
                                cs.drawImage(pdImage, 0f, 0f, finalW.toFloat(), finalH.toFloat())
                            }
                            rasterized = true
                        } catch (e: Exception) {
                            AppLogger.w("PdfEditor: renderer-based redaction flattening failed for page $pageIdx: ${e.message}")
                        } finally {
                            try { renderPage?.close() } catch (_: Exception) {}
                        }
                    }

                    if (!rasterized) {
                        throw SecurityException("Cannot forensically rasterize the PDF page because PdfRenderer is unavailable. Aborting redaction to ensure maximum security without data loss.")
                    }
                    continue
                }

                // B. Non-destructive modifications for pages WITHOUT redactions (preserves vector text)
                // Apply rotation
                if (mod.rotationDegrees != 0) {
                    val currentRotation = page.rotation
                    page.rotation = (currentRotation + mod.rotationDegrees) % 360
                }

                // Apply annotations overlay if any drawings, texts, or stamps exist
                if (mod.drawings.isNotEmpty() || mod.textAnnotations.isNotEmpty() || mod.stamps.isNotEmpty()) {
                    val cropBox = page.cropBox ?: page.mediaBox
                    val pageWidthPts = cropBox.width
                    val pageHeightPts = cropBox.height
                    val lowerLeftX = cropBox.lowerLeftX
                    val lowerLeftY = cropBox.lowerLeftY
                    val rot = page.rotation

                    val dispW = if (rot == 90 || rot == 270) pageHeightPts else pageWidthPts
                    val dispH = if (rot == 90 || rot == 270) pageWidthPts else pageHeightPts

                    // Render overlay to a crisp 2x resolution bitmap in display orientation
                    val overlayBmp = renderAnnotationOverlayBitmap(
                        mod = mod,
                        targetWidth = (dispW * 2).toInt(),
                        targetHeight = (dispH * 2).toInt()
                    )

                    if (overlayBmp != null) {
                        try {
                            val pdImage = LosslessFactory.createFromImage(document, overlayBmp)
                            val contentStream = PDPageContentStream(
                                document,
                                page,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                            )
                            contentStream.use { cs ->
                                cs.saveGraphicsState()
                                when (rot) {
                                    90 -> {
                                        cs.transform(Matrix(0f, 1f, -1f, 0f, lowerLeftX + pageWidthPts, lowerLeftY))
                                        cs.drawImage(pdImage, 0f, 0f, pageHeightPts, pageWidthPts)
                                    }
                                    180 -> {
                                        cs.transform(Matrix(-1f, 0f, 0f, -1f, lowerLeftX + pageWidthPts, lowerLeftY + pageHeightPts))
                                        cs.drawImage(pdImage, 0f, 0f, pageWidthPts, pageHeightPts)
                                    }
                                    270 -> {
                                        cs.transform(Matrix(0f, -1f, 1f, 0f, lowerLeftX, lowerLeftY + pageHeightPts))
                                        cs.drawImage(pdImage, 0f, 0f, pageHeightPts, pageWidthPts)
                                    }
                                    else -> {
                                        cs.drawImage(pdImage, lowerLeftX, lowerLeftY, pageWidthPts, pageHeightPts)
                                    }
                                }
                                cs.restoreGraphicsState()
                            }
                        } finally {
                            overlayBmp.recycle()
                        }
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
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            try { document?.close() } catch (_: Exception) {}
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
                // 1. Try direct descriptor from ContentResolver first
                try {
                    val directPfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (directPfd != null) {
                        try {
                            val testRenderer = PdfRenderer(directPfd)
                            testRenderer.close()
                            return context.contentResolver.openFileDescriptor(uri, "r")
                        } catch (_: Exception) {
                            try { directPfd.close() } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {}

                // 2. Fallback: Copy to a deterministic session cache file to avoid re-copying on every page render
                val hash = uri.toString().hashCode().toUInt().toString(16)
                val tempFile = File(context.cacheDir, "pdf_seekable_$hash.pdf")
                if (!tempFile.exists() || tempFile.length() == 0L || (System.currentTimeMillis() - tempFile.lastModified() > 10 * 60 * 1000L)) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
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
