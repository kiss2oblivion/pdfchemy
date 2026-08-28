package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

object PdfOcrEngine {

    /**
     * Performs 100% on-device OCR recognition on a PDF document (or scanned images)
     * and generates a searchable PDF where the original visual page is intact
     * with an invisible, selectable text layer underneath/over each recognized word.
     */
    suspend fun createSearchablePdf(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var outputDoc: PDDocument? = null

        try {
            pfd = if (sourceUri.scheme == "file") {
                val path = sourceUri.path
                if (path != null) ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY) else null
            } else {
                context.contentResolver.openFileDescriptor(sourceUri, "r")
            }

            if (pfd == null) return@withContext false

            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount
            if (pageCount <= 0) return@withContext false

            outputDoc = PDDocument()

            for (i in 0 until pageCount) {
                onProgress(i + 1, pageCount)
                val page = renderer.openPage(i)
                
                try {
                    val pageWidth = page.width.toFloat()
                    val pageHeight = page.height.toFloat()

                    // Render page bitmap at 2x resolution for optimal OCR fidelity
                    val renderScale = 2f
                    val bmpWidth = (pageWidth * renderScale).toInt().coerceAtLeast(1)
                    val bmpHeight = (pageHeight * renderScale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    // Run ML Kit Text Recognition on the rendered bitmap
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val visionText: Text = try {
                        recognizer.process(inputImage).await()
                    } catch (e: Exception) {
                        null
                    } ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(inputImage).await()

                    // Create PDF page with matching dimensions
                    val pdPage = PDPage(PDRectangle(pageWidth, pageHeight))
                    outputDoc.addPage(pdPage)

                    // 1. Draw original page image
                    val pdImage = JPEGFactory.createFromImage(outputDoc, bitmap, 0.85f)
                    val contentStream = PDPageContentStream(outputDoc, pdPage)
                    contentStream.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)

                    // 2. Inject transparent searchable text layer
                    if (visionText.textBlocks.isNotEmpty()) {
                        // Set transparent graphics state for invisible text selection layer
                        val extGState = PDExtendedGraphicsState().apply {
                            nonStrokingAlphaConstant = 0.0f
                        }
                        contentStream.setGraphicsStateParameters(extGState)

                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    val box = element.boundingBox ?: continue
                                    val text = element.text.trim()
                                    if (text.isEmpty()) continue

                                    // Scale coordinates from bitmap (2x) back to PDF points
                                    val scaleX = pageWidth / bmpWidth.toFloat()
                                    val scaleY = pageHeight / bmpHeight.toFloat()

                                    val x = box.left * scaleX
                                    val y = pageHeight - (box.bottom * scaleY)
                                    val elementWidth = box.width() * scaleX
                                    val elementHeight = box.height() * scaleY

                                    val fontSize = elementHeight.coerceIn(4f, 72f)
                                    val font = PDType1Font.HELVETICA

                                    try {
                                        contentStream.beginText()
                                        contentStream.setFont(font, fontSize)
                                        contentStream.newLineAtOffset(x, y)
                                        contentStream.showText(text)
                                        contentStream.endText()
                                    } catch (e: Exception) {
                                        // Ignore unsupported glyphs in standard font
                                    }
                                }
                            }
                        }
                    }

                    contentStream.close()
                    bitmap.recycle()
                } finally {
                    page.close()
                }
            }

            context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                outputDoc.save(outStream)
            }
            true
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to create searchable OCR PDF: ${e.message}", e)
            false
        } finally {
            outputDoc?.close()
            renderer?.close()
            pfd?.close()
            recognizer.close()
        }
    }
}
