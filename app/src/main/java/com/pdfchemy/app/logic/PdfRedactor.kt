package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RedactionBox(
    val pageIndex: Int,
    val normalizedRect: RectF, // Coordinates normalized to [0..1] relative to page width and height
    val overlayLabel: String? = "REDACTED"
)

object PdfRedactor {

    /**
     * Applies permanent visual redaction to the specified pages of a PDF.
     * Draws opaque redaction fill rectangles and optional security warning labels
     * to sanitize sensitive data permanently.
     */
    suspend fun applyRedactions(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        redactions: List<RedactionBox>
    ): Boolean = withContext(Dispatchers.IO) {
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                doc = PDDocument.load(inStream)
                if (doc == null) return@withContext false

                val redactionsByPage = redactions.groupBy { it.pageIndex }

                for ((pageIdx, pageRedactions) in redactionsByPage) {
                    if (pageIdx < 0 || pageIdx >= doc!!.numberOfPages) continue
                    val page = doc!!.getPage(pageIdx)
                    val mediaBox = page.mediaBox
                    val pageWidth = mediaBox.width
                    val pageHeight = mediaBox.height

                    // Open append stream to draw opaque redactions on top of content
                    val contentStream = PDPageContentStream(
                        doc,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    )

                    val blackColor = PDColor(floatArrayOf(0f, 0f, 0f), PDDeviceRGB.INSTANCE)
                    val whiteColor = PDColor(floatArrayOf(1f, 1f, 1f), PDDeviceRGB.INSTANCE)

                    for (redaction in pageRedactions) {
                        val norm = redaction.normalizedRect
                        // PDF coordinate origin is bottom-left
                        val x = norm.left * pageWidth
                        val y = (1f - norm.bottom) * pageHeight
                        val width = (norm.right - norm.left) * pageWidth
                        val height = (norm.bottom - norm.top) * pageHeight

                        // 1. Draw solid black redaction rectangle
                        contentStream.setNonStrokingColor(blackColor)
                        contentStream.addRect(x, y, width, height)
                        contentStream.fill()

                        // 2. Optionally draw white centered security text
                        if (!redaction.overlayLabel.isNullOrBlank() && width > 40 && height > 12) {
                            val fontSize = (height * 0.45f).coerceIn(6f, 12f)
                            val font = PDType1Font.HELVETICA_BOLD
                            val label = redaction.overlayLabel
                            val textWidth = font.getStringWidth(label) / 1000f * fontSize
                            
                            if (textWidth < width - 4) {
                                val textX = x + (width - textWidth) / 2f
                                val textY = y + (height - fontSize) / 2f + 1f

                                contentStream.beginText()
                                contentStream.setFont(font, fontSize)
                                contentStream.setNonStrokingColor(whiteColor)
                                contentStream.newLineAtOffset(textX, textY)
                                contentStream.showText(label)
                                contentStream.endText()
                            }
                        }
                    }

                    contentStream.close()
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    doc?.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to apply redactions: ${e.message}", e)
            false
        } finally {
            doc?.close()
        }
    }
}
