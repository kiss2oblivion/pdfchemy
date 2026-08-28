package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WatermarkOptions(
    val text: String? = null,
    val imageBitmap: Bitmap? = null,
    val rotationDegrees: Float = 45f,
    val opacity: Float = 0.3f,
    val fontSize: Float = 40f,
    val isTiled: Boolean = false
)

enum class NumberPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

enum class NumberFormat {
    SIMPLE,        // "1"
    PAGE_X_OF_Y,   // "Page 1 of 5"
    SLASH          // "1 / 5"
}

data class PageNumberOptions(
    val position: NumberPosition = NumberPosition.BOTTOM_CENTER,
    val format: NumberFormat = NumberFormat.PAGE_X_OF_Y,
    val skipFirstPage: Boolean = false,
    val fontSize: Float = 10f,
    val marginPts: Float = 36f
)

object PdfStampAndNumberEngine {

    suspend fun addWatermark(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        options: WatermarkOptions
    ): Boolean = withContext(Dispatchers.IO) {
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                doc = PDDocument.load(inStream)
                if (doc == null) return@withContext false

                val totalPages = doc!!.numberOfPages
                val font = PDType1Font.HELVETICA_BOLD

                for (i in 0 until totalPages) {
                    val page = doc!!.getPage(i)
                    val mediaBox = page.cropBox ?: page.mediaBox
                    val pageWidth = mediaBox.width
                    val pageHeight = mediaBox.height

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        val gs = PDExtendedGraphicsState().apply {
                            nonStrokingAlphaConstant = options.opacity.coerceIn(0.05f, 1f)
                        }
                        cs.setGraphicsStateParameters(gs)

                        if (!options.text.isNullOrBlank()) {
                            val text = options.text
                            val textWidth = (font.getStringWidth(text) / 1000f) * options.fontSize
                            val textHeight = options.fontSize

                            if (options.isTiled) {
                                // Render 3x3 tiled watermark
                                for (row in 1..3) {
                                    for (col in 1..3) {
                                        val tileX = (pageWidth / 4f) * col
                                        val tileY = (pageHeight / 4f) * row

                                        cs.saveGraphicsState()
                                        val rad = Math.toRadians(options.rotationDegrees.toDouble())
                                        val matrix = Matrix.getRotateInstance(rad, tileX, tileY)
                                        cs.transform(matrix)

                                        cs.beginText()
                                        cs.setFont(font, options.fontSize * 0.7f)
                                        cs.setNonStrokingColor(128, 128, 128)
                                        cs.newLineAtOffset(-textWidth * 0.35f, -textHeight * 0.35f)
                                        cs.showText(text)
                                        cs.endText()

                                        cs.restoreGraphicsState()
                                    }
                                }
                            } else {
                                // Single Center Watermark
                                cs.saveGraphicsState()
                                val centerX = pageWidth / 2f
                                val centerY = pageHeight / 2f
                                val rad = Math.toRadians(options.rotationDegrees.toDouble())
                                val matrix = Matrix.getRotateInstance(rad, centerX, centerY)
                                cs.transform(matrix)

                                cs.beginText()
                                cs.setFont(font, options.fontSize)
                                cs.setNonStrokingColor(128, 128, 128)
                                cs.newLineAtOffset(-textWidth / 2f, -textHeight / 2f)
                                cs.showText(text)
                                cs.endText()

                                cs.restoreGraphicsState()
                            }
                        } else if (options.imageBitmap != null) {
                            val pdImage = LosslessFactory.createFromImage(doc, options.imageBitmap)
                            val imgW = (pageWidth * 0.5f).coerceAtLeast(100f)
                            val imgH = (imgW * (options.imageBitmap.height.toFloat() / options.imageBitmap.width.toFloat()))
                            val x = (pageWidth - imgW) / 2f
                            val y = (pageHeight - imgH) / 2f
                            cs.drawImage(pdImage, x, y, imgW, imgH)
                        }
                    }
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    doc!!.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e("Failed to add watermark to PDF: ${e.message}", e)
            false
        } finally {
            doc?.close()
        }
    }

    suspend fun addPageNumbers(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        options: PageNumberOptions
    ): Boolean = withContext(Dispatchers.IO) {
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                doc = PDDocument.load(inStream)
                if (doc == null) return@withContext false

                val totalPages = doc!!.numberOfPages
                val font = PDType1Font.HELVETICA

                for (i in 0 until totalPages) {
                    if (i == 0 && options.skipFirstPage) {
                        continue
                    }

                    val page = doc!!.getPage(i)
                    val mediaBox = page.cropBox ?: page.mediaBox
                    val pageWidth = mediaBox.width
                    val pageHeight = mediaBox.height

                    val pageNumberText = when (options.format) {
                        NumberFormat.SIMPLE -> "${i + 1}"
                        NumberFormat.PAGE_X_OF_Y -> "Page ${i + 1} of $totalPages"
                        NumberFormat.SLASH -> "${i + 1} / $totalPages"
                    }

                    val textWidth = (font.getStringWidth(pageNumberText) / 1000f) * options.fontSize
                    val margin = options.marginPts

                    val (x, y) = when (options.position) {
                        NumberPosition.TOP_LEFT -> margin to (pageHeight - margin)
                        NumberPosition.TOP_CENTER -> ((pageWidth - textWidth) / 2f) to (pageHeight - margin)
                        NumberPosition.TOP_RIGHT -> (pageWidth - margin - textWidth) to (pageHeight - margin)
                        NumberPosition.BOTTOM_LEFT -> margin to margin
                        NumberPosition.BOTTOM_CENTER -> ((pageWidth - textWidth) / 2f) to margin
                        NumberPosition.BOTTOM_RIGHT -> (pageWidth - margin - textWidth) to margin
                    }

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.beginText()
                        cs.setFont(font, options.fontSize)
                        cs.setNonStrokingColor(80, 80, 80)
                        cs.newLineAtOffset(x, y)
                        cs.showText(pageNumberText)
                        cs.endText()
                    }
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    doc!!.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e("Failed to add page numbers to PDF: ${e.message}", e)
            false
        } finally {
            doc?.close()
        }
    }
}
