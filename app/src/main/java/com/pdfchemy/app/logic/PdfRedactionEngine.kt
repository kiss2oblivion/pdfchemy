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

data class RedactionConfig(
    val isBlackout: Boolean = true, // true = Blackout box, false = Whiteout box
    val defaultOverlayText: String = "REDACTED",
    val searchKeyword: String = "",
    val isRegex: Boolean = false,
    val manualBoxes: List<RedactionBox> = emptyList()
)

object PdfRedactionEngine {

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

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF stream")

            tempFile.delete()

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
