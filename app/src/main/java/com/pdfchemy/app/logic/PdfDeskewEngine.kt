package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.util.Matrix as PdfMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 100% Offline & Local-First Auto-Deskew & Scanner Angle Straightener.
 * Detects scan tilt using projection profile variance analysis and corrects skew losslessly.
 */
object PdfDeskewEngine {

    /**
     * Estimates skew angle in degrees (negative = tilted counterclockwise, positive = tilted clockwise).
     */
    fun detectSkewAngle(bitmap: Bitmap): Float {
        // Downscale to fast analysis dimensions (~250px)
        val targetWidth = 250
        val scale = targetWidth.toFloat() / bitmap.width.toFloat()
        val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        val w = scaled.width
        val h = scaled.height

        // Convert to binary/luminance array
        val pixels = IntArray(w * h)
        scaled.getPixels(pixels, 0, w, 0, 0, w, h)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        val lum = FloatArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            lum[i] = if ((0.299f * r + 0.587f * g + 0.114f * b) < 180f) 1.0f else 0.0f
        }

        var maxVariance = -1.0
        var bestAngle = 0.0f

        // Search angles in range [-15.0, +15.0] in 0.5 degree steps
        var angle = -15.0f
        while (angle <= 15.0f) {
            val rad = Math.toRadians(angle.toDouble())
            val cosA = cos(rad)
            val sinA = sin(rad)

            val rowSums = DoubleArray(h)
            val centerX = w / 2.0
            val centerY = h / 2.0

            for (y in 0 until h) {
                val dy = y - centerY
                for (x in 0 until w step 2) {
                    val dx = x - centerX
                    val rotY = ((-dx * sinA + dy * cosA) + centerY).toInt()
                    if (rotY in 0 until h) {
                        rowSums[rotY] += lum[y * w + x].toDouble()
                    }
                }
            }

            // Calculate variance of projection profile
            var mean = 0.0
            for (s in rowSums) mean += s
            mean /= h.toDouble()

            var variance = 0.0
            for (s in rowSums) {
                val diff = s - mean
                variance += diff * diff
            }

            if (variance > maxVariance) {
                maxVariance = variance
                bestAngle = angle
            }

            angle += 0.5f
        }

        return bestAngle
    }

    suspend fun deskewDocument(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        targetPages: Set<Int>? = null
    ): Int = withContext(Dispatchers.IO) {
        var straightenedCount = 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var document: PDDocument? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            val docStream = context.contentResolver.openInputStream(sourceUri)
            if (pfd != null && docStream != null) {
                renderer = PdfRenderer(pfd)
                document = PDDocument.load(docStream)
                val totalPages = document.numberOfPages

                for (i in 0 until totalPages) {
                    if (targetPages != null && i !in targetPages) continue

                    var page: PdfRenderer.Page? = null
                    val angle = try {
                        page = renderer.openPage(i)
                        val bmp = Bitmap.createBitmap(250, (250f * page.height / page.width).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val detected = detectSkewAngle(bmp)
                        bmp.recycle()
                        detected
                    } catch (e: Exception) {
                        AppLogger.e("Deskew angle detection failed on page $i", e)
                        0.0f
                    } finally {
                        page?.close()
                    }

                    if (abs(angle) >= 0.5f) {
                        val pdPage = document.getPage(i)
                        applyDeskewTransform(document, pdPage, -angle)
                        straightenedCount++
                    }
                }

                context.contentResolver.openOutputStream(destUri)?.use { out ->
                    document.save(out)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error deskewing document", e)
        } finally {
            document?.close()
            renderer?.close()
            pfd?.close()
        }

        straightenedCount
    }

    private fun applyDeskewTransform(doc: PDDocument, page: PDPage, correctionAngleDegrees: Float) {
        val rad = Math.toRadians(correctionAngleDegrees.toDouble())
        val cos = cos(rad).toFloat()
        val sin = sin(rad).toFloat()

        val mb = page.mediaBox
        val cx = mb.width / 2f
        val cy = mb.height / 2f

        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.PREPEND, false, false).use { cs ->
            cs.saveGraphicsState()
            val translateToCenter = PdfMatrix.getTranslateInstance(cx, cy)
            val rotateMatrix = PdfMatrix(cos, sin, -sin, cos, 0f, 0f)
            val translateBack = PdfMatrix.getTranslateInstance(-cx, -cy)

            cs.transform(translateToCenter)
            cs.transform(rotateMatrix)
            cs.transform(translateBack)
        }

        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false, false).use { cs ->
            cs.restoreGraphicsState()
        }
    }
}
