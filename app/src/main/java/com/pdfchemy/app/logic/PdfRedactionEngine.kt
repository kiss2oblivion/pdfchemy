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

            document = PDDocument.load(inputStream, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
            val totalPages = document.numberOfPages
            val foundBoxes = mutableListOf<RedactionBox>()

            val pattern = if (isRegex) {
                try { Pattern.compile(query, Pattern.CASE_INSENSITIVE) } catch (e: Exception) { null }
            } else {
                Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
            } ?: return@withContext Result.success(emptyList())

            val stripper = object : PDFTextStripper() {
                override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                    val pageIdx = (currentPageNo - 1).coerceAtLeast(0)
                    if (pageIdx !in 0 until totalPages) return
                    val page = document.getPage(pageIdx)
                    val cropBox = page.cropBox ?: page.mediaBox
                    val rot = ((page.rotation % 360) + 360) % 360
                    val pageW = if (rot == 90 || rot == 270) cropBox.height else cropBox.width
                    val pageH = if (rot == 90 || rot == 270) cropBox.width else cropBox.height

                    val matcher = pattern.matcher(text)
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        if (start in textPositions.indices && end - 1 in textPositions.indices) {
                            val matchPositions = textPositions.subList(start, end)
                            if (matchPositions.isNotEmpty()) {
                                val firstPos = matchPositions.first()
                                val lastPos = matchPositions.last()

                                val minX = minOf(firstPos.xDirAdj, lastPos.xDirAdj)
                                val maxX = maxOf(firstPos.xDirAdj + firstPos.widthDirAdj, lastPos.xDirAdj + lastPos.widthDirAdj)
                                val topY = minOf(firstPos.yDirAdj, lastPos.yDirAdj)
                                val height = matchPositions.map { it.heightDir }.average().toFloat().coerceAtLeast(12f)
                                val bottomY = topY + height

                                val leftNorm = (minX / pageW).coerceIn(0f, 1f)
                                val topNorm = (topY / pageH).coerceIn(0f, 1f)
                                val rightNorm = (maxX / pageW).coerceIn(0f, 1f)
                                val bottomNorm = (bottomY / pageH).coerceIn(0f, 1f)

                                if (rightNorm > leftNorm && bottomNorm > topNorm) {
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
                    }
                    super.writeString(text, textPositions)
                }
            }
            stripper.startPage = 1
            stripper.endPage = totalPages
            val dummyWriter = OutputStreamWriter(ByteArrayOutputStream())
            stripper.writeText(document, dummyWriter)

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

            document = PDDocument.load(inputStream, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
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
            var rasterFile: File? = null
            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var newDoc: PDDocument? = null

            try {
                document.save(tempFile)
                document.close()
                document = null

                val finalFile = if (config.forensicSanitize) {
                    rasterFile = File(context.cacheDir, "rasterized_${System.currentTimeMillis()}.pdf")
                    pfd = try { ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY) } catch (_: Exception) { null }
                    renderer = if (pfd != null) try { PdfRenderer(pfd) } catch (_: Exception) { null } else null
                    val baseDoc = PDDocument.load(tempFile, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                    
                    if (renderer != null) {
                        newDoc = PDDocument()
                        try {
                            for (i in 0 until renderer.pageCount) {
                                if (boxesByPage.containsKey(i)) {
                                    // Forensically flatten only pages with redactions to permanently scrub underlying text
                                    var renderPage: PdfRenderer.Page? = null
                                    var bmp: Bitmap? = null
                                    try {
                                        renderPage = renderer.openPage(i)
                                        val maxDim = 2048
                                        val scale = minOf(2f, maxDim.toFloat() / maxOf(renderPage.width, renderPage.height).coerceAtLeast(1))
                                        val targetW = (renderPage.width * scale).toInt().coerceAtLeast(1)
                                        val targetH = (renderPage.height * scale).toInt().coerceAtLeast(1)
                                        bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                                        val canvas = android.graphics.Canvas(bmp)
                                        canvas.drawColor(android.graphics.Color.WHITE)
                                        
                                        renderPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                        
                                        val pdfPage = PDPage(PDRectangle(renderPage.width.toFloat(), renderPage.height.toFloat()))
                                        newDoc.addPage(pdfPage)
                                        
                                        val pdImage = JPEGFactory.createFromImage(newDoc, bmp, 0.90f)
                                        PDPageContentStream(newDoc, pdfPage).use { cs ->
                                            cs.drawImage(pdImage, 0f, 0f, renderPage.width.toFloat(), renderPage.height.toFloat())
                                        }
                                    } finally {
                                        bmp?.recycle()
                                        renderPage?.close()
                                    }
                                } else {
                                    // Preserve original crisp vector page, fonts, and searchability for unredacted pages
                                    if (i < baseDoc.numberOfPages) {
                                        newDoc.importPage(baseDoc.getPage(i))
                                    }
                                }
                            }
                        } finally {
                            try { baseDoc.close() } catch (_: Exception) {}
                        }
                        try { renderer.close() } catch (_: Exception) {}
                        renderer = null
                        try { pfd?.close() } catch (_: Exception) {}
                        pfd = null
                        
                        newDoc.save(rasterFile)
                        newDoc.close()
                        newDoc = null
                        tempFile.delete()
                        rasterFile
                    } else {
                        baseDoc.close()
                        try { pfd?.close() } catch (_: Exception) {}
                        tempFile.delete()
                        throw SecurityException("Cannot forensically rasterize the PDF because PdfRenderer is unavailable. Aborting redaction to ensure maximum security without data loss.")
                    }
                } else {
                    tempFile
                }

                context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                    finalFile.inputStream().use { inp ->
                        inp.copyTo(out)
                    }
                } ?: throw IllegalStateException("Cannot open destination PDF stream")

                val historyRepo = HistoryRepository(context)
                historyRepo.addHistoryItem(
                    destPdfUri,
                    FileUtils.getFileName(context, destPdfUri) ?: "redacted.pdf",
                    "Sanitized & Redacted PDF (${boxes.size} elements)"
                )

                Result.success(boxes.size)
            } finally {
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
                try { newDoc?.close() } catch (_: Exception) {}
                if (tempFile.exists()) tempFile.delete()
                if (rasterFile != null && rasterFile.exists()) rasterFile.delete()
            }
        } catch (e: Exception) {
            AppLogger.e("PdfRedactionEngine: Error applying redactions", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
