package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStreamWriter
import java.util.regex.Pattern
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle

enum class RedactPattern(val label: String, val regex: String) {
    CREDIT_CARD("Credit Card Numbers", "\\b(?:\\d[ -]*?){13,16}\\b"),
    SSN_US("Social Security Number (US)", "\\b\\d{3}[- ]?\\d{2}[- ]?\\d{4}\\b"),
    EMAIL("Email Addresses", "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"),
    PHONE_NUMBERS("Phone Numbers", "\\b(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b"),
    IBAN("IBAN / Bank Accounts", "\\b[A-Z]{2}[0-9]{2}(?:[ ]?[0-9A-Z]{4}){3,7}\\b")
}

data class RedactionConfig(
    val isBlackout: Boolean = true, // true = Blackout box, false = Whiteout box
    val defaultOverlayText: String = "REDACTED",
    val searchKeyword: String = "",
    val isRegex: Boolean = false,
    val manualBoxes: List<RedactionBox> = emptyList(),
    val forensicSanitize: Boolean = true
)

object PdfRedactionEngine {

    /**
     * Automatically redacts predefined PII patterns across the entire document.
     */
    suspend fun smartRedact(
        context: Context,
        pdfUri: Uri,
        destUri: Uri,
        patterns: List<RedactPattern>,
        config: RedactionConfig = RedactionConfig(isBlackout = true, defaultOverlayText = "REDACTED", forensicSanitize = true)
    ): Result<Int> {
        val combinedRegex = patterns.joinToString(separator = "|") { it.regex }
        val searchResult = searchRedactionTargets(context, pdfUri, combinedRegex, isRegex = true)
        if (searchResult.isFailure) return Result.failure(searchResult.exceptionOrNull() ?: Exception("Unknown error"))
        
        val boxes = searchResult.getOrNull() ?: emptyList()
        if (boxes.isEmpty()) return Result.success(0)
        
        return applyRedactions(context, pdfUri, destUri, boxes, config)
    }

    /**
     * Searches the PDF for occurrences of a keyword or regex pattern and returns bounding boxes.
     */
    suspend fun searchRedactionTargets(
        context: Context,
        pdfUri: Uri,
        query: String,
        isRegex: Boolean = false
    ): Result<List<RedactionBox>> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages
            val foundBoxes = mutableListOf<RedactionBox>()

            val pattern = if (isRegex) {
                try { Pattern.compile(query, Pattern.CASE_INSENSITIVE) } catch (e: Exception) { null }
            } else {
                Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
            } ?: return@withContext Result.success(emptyList())

            for (pageIdx in 0 until totalPages) {
                val page = document.getPage(pageIdx)
                val mediaBox = page.mediaBox
                val pageW = mediaBox.width
                val pageH = mediaBox.height

                val stripper = object : PDFTextStripper() {
                    val textPositions = mutableListOf<TextPosition>()

                    override fun processTextPosition(text: TextPosition) {
                        textPositions.add(text)
                        super.processTextPosition(text)
                    }
                }

                stripper.startPage = pageIdx + 1
                stripper.endPage = pageIdx + 1

                val dummyWriter = OutputStreamWriter(ByteArrayOutputStream())
                stripper.writeText(document, dummyWriter)

                val fullText = stripper.textPositions.joinToString("") { it.unicode ?: "" }
                val matcher = pattern.matcher(fullText)

                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    if (start in stripper.textPositions.indices && end - 1 in stripper.textPositions.indices) {
                        val firstPos = stripper.textPositions[start]
                        val lastPos = stripper.textPositions[end - 1]

                        val minX = firstPos.xDirAdj
                        val maxX = lastPos.xDirAdj + lastPos.widthDirAdj
                        val topY = firstPos.yDirAdj
                        val height = firstPos.heightDir.coerceAtLeast(12f)
                        val bottomY = topY + height

                        val leftNorm = (minX / pageW).coerceIn(0f, 1f)
                        val topNorm = (topY / pageH).coerceIn(0f, 1f)
                        val rightNorm = (maxX / pageW).coerceIn(0f, 1f)
                        val bottomNorm = (bottomY / pageH).coerceIn(0f, 1f)

                        foundBoxes.add(
                            RedactionBox(
                                pageIndex = pageIdx,
                                normalizedRect = RectF(leftNorm, topNorm, rightNorm, bottomNorm),
                                overlayLabel = matcher.group()
                            )
                        )
                    }
                }
            }

            Result.success(foundBoxes)
        } catch (e: Exception) {
            AppLogger.e("PdfRedactionEngine: Error searching targets", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Applies permanent redaction blackout/whiteout boxes to the PDF.
     */
    suspend fun applyRedactions(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        boxes: List<RedactionBox>,
        config: RedactionConfig
    ): Result<Int> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            if (boxes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("No redaction targets specified"))
            }

            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages
            val font = PDType1Font.HELVETICA_BOLD

            val boxesByPage = boxes.groupBy { it.pageIndex }

            for ((pageIdx, pageBoxes) in boxesByPage) {
                if (pageIdx !in 0 until totalPages) continue
                val page = document.getPage(pageIdx)
                val mediaBox = page.mediaBox
                val pageW = mediaBox.width
                val pageH = mediaBox.height

                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    for (box in pageBoxes) {
                        val norm = box.normalizedRect
                        val drawX = norm.left * pageW
                        val drawY = (1f - norm.bottom) * pageH
                        val drawW = (norm.right - norm.left) * pageW
                        val drawH = (norm.bottom - norm.top) * pageH

                        // 1. Draw solid opaque redaction rectangle
                        cs.saveGraphicsState()
                        if (config.isBlackout) {
                            cs.setNonStrokingColor(0, 0, 0)
                        } else {
                            cs.setNonStrokingColor(255, 255, 255)
                        }
                        cs.addRect(drawX, drawY, drawW, drawH)
                        cs.fill()
                        cs.restoreGraphicsState()

                        // 2. Optional text label (e.g. "REDACTED")
                        val overlay = config.defaultOverlayText
                        if (overlay.isNotBlank() && drawW > 30f && drawH > 10f) {
                            val fontSize = (drawH * 0.6f).coerceIn(6f, 10f)
                            val textW = (font.getStringWidth(overlay) / 1000f) * fontSize
                            if (textW < drawW) {
                                cs.saveGraphicsState()
                                cs.beginText()
                                cs.setFont(font, fontSize)
                                if (config.isBlackout) {
                                    cs.setNonStrokingColor(255, 255, 255)
                                } else {
                                    cs.setNonStrokingColor(80, 80, 80)
                                }
                                val textX = drawX + ((drawW - textW) / 2f)
                                val textY = drawY + ((drawH - fontSize) / 2f)
                                cs.newLineAtOffset(textX, textY)
                                cs.showText(overlay)
                                cs.endText()
                                cs.restoreGraphicsState()
                            }
                        }
                    }
                }
            }

            val tempFile = File(context.cacheDir, "redacted_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            val finalFile = if (config.forensicSanitize) {
                val rasterFile = File(context.cacheDir, "rasterized_${System.currentTimeMillis()}.pdf")
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                
                val newDoc = PDDocument()
                for (i in 0 until renderer.pageCount) {
                    val renderPage = renderer.openPage(i)
                    // 2x resolution (144 dpi) for good legibility while killing vectors
                    val bmp = Bitmap.createBitmap(
                        (renderPage.width * 2f).toInt(),
                        (renderPage.height * 2f).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    renderPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    renderPage.close()
                    
                    val pdfPage = PDPage(PDRectangle(renderPage.width.toFloat(), renderPage.height.toFloat()))
                    newDoc.addPage(pdfPage)
                    
                    val outStream = java.io.ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, outStream)
                    val imageBytes = outStream.toByteArray()
                    
                    val pdImage = JPEGFactory.createFromByteArray(newDoc, imageBytes)
                    val cs = PDPageContentStream(newDoc, pdfPage)
                    cs.drawImage(pdImage, 0f, 0f, renderPage.width.toFloat(), renderPage.height.toFloat())
                    cs.close()
                    
                    bmp.recycle()
                }
                renderer.close()
                pfd.close()
                
                newDoc.save(rasterFile)
                newDoc.close()
                tempFile.delete()
                rasterFile
            } else {
                tempFile
            }

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                finalFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF stream")

            finalFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "redacted.pdf",
                "Sanitized & Redacted PDF (${boxes.size} elements)"
            )

            Result.success(boxes.size)
        } catch (e: Exception) {
            AppLogger.e("PdfRedactionEngine: Error applying redactions", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
