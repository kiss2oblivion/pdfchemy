package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Normalized crop rectangle where (0.0, 0.0) is top-left and (1.0, 1.0) is bottom-right.
 */
data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    fun isValid(): Boolean = left < right && top < bottom && left >= 0f && right <= 1f && top >= 0f && bottom <= 1f
}

object PdfCropEngine {

    /**
     * Detects non-white content bounding box in a page bitmap and returns normalized crop rectangle.
     */
    fun detectContentBounds(bitmap: Bitmap, toleranceThreshold: Int = 245): NormalizedCropRect {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return NormalizedCropRect()

        var minX = width
        var minY = height
        var maxX = 0
        var maxY = 0

        val stepX = max(1, width / 200)
        val stepY = max(1, height / 200)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val alpha = Color.alpha(pixel)

                // If not transparent and not near-white
                if (alpha > 50 && (r < toleranceThreshold || g < toleranceThreshold || b < toleranceThreshold)) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // If whole page appears empty or white, return default uncropped
        if (minX >= maxX || minY >= maxY) {
            return NormalizedCropRect(0.05f, 0.05f, 0.95f, 0.95f)
        }

        // Add 2% padding around content for aesthetics
        val padX = (width * 0.02f).toInt()
        val padY = (height * 0.02f).toInt()

        val cropLeft = max(0, minX - padX).toFloat() / width
        val cropTop = max(0, minY - padY).toFloat() / height
        val cropRight = min(width, maxX + padX).toFloat() / width
        val cropBottom = min(height, maxY + padY).toFloat() / height

        return NormalizedCropRect(
            left = cropLeft.coerceIn(0f, 0.45f),
            top = cropTop.coerceIn(0f, 0.45f),
            right = cropRight.coerceIn(0.55f, 1f),
            bottom = cropBottom.coerceIn(0.55f, 1f)
        )
    }

    /**
     * Crops pages of a PDF document by updating the PDPage cropBox.
     * Preserves vector graphics and text sharpness completely without rasterizing.
     */
    suspend fun cropPdf(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        cropRect: NormalizedCropRect,
        targetPageIndex: Int? = null // null means apply to all pages
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            if (pageCount == 0) {
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            val pagesToCrop = if (targetPageIndex != null) {
                listOf(targetPageIndex.coerceIn(0, pageCount - 1))
            } else {
                (0 until pageCount).toList()
            }

            for (idx in pagesToCrop) {
                val page = document.getPage(idx)
                val mediaBox = page.mediaBox ?: PDRectangle(PDRectangle.A4.width, PDRectangle.A4.height)

                val llx = mediaBox.lowerLeftX
                val lly = mediaBox.lowerLeftY
                val width = mediaBox.width
                val height = mediaBox.height

                // Transform top-left screen normalized coordinates to bottom-left PDF coordinates
                val cropLeft = llx + (cropRect.left * width)
                val cropRight = llx + (cropRect.right * width)
                val cropTopPdf = lly + ((1f - cropRect.top) * height)
                val cropBottomPdf = lly + ((1f - cropRect.bottom) * height)

                val newCropBox = PDRectangle(
                    cropLeft,
                    cropBottomPdf,
                    max(10f, cropRight - cropLeft),
                    max(10f, cropTopPdf - cropBottomPdf)
                )

                page.cropBox = newCropBox
            }

            // Save to temp file first
            val tempFile = File(context.cacheDir, "cropped_temp_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { destStream ->
                tempFile.inputStream().use { tempIn ->
                    tempIn.copyTo(destStream)
                }
            } ?: throw IllegalStateException("Cannot open destination stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "cropped.pdf",
                "Cropped PDF"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfCropEngine: Error cropping PDF", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
