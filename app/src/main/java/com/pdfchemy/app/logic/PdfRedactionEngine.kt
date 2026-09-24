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
import kotlinx.coroutines.async
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.regex.Pattern
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import android.graphics.pdf.PdfRenderer
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.pdfchemy.app.sandbox.NativeRendererCoordinator

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
     * Searches the PDF for occurrences of a keyword or regex pattern and returns bounding boxes.
     */
    suspend fun searchRedactionTargets(
        context: Context,
        inputStream: InputStream,
        query: String,
        isRegex: Boolean = false
    ): Result<List<RedactionBox>> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            if (query.isBlank()) return@withContext Result.success(emptyList())

            if (query.isBlank()) return@withContext Result.success(emptyList())

            val memSettings = com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMixed(10 * 1024 * 1024, 250 * 1024 * 1024)
            document = PDDocument.load(inputStream, memSettings)
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
        }
    }

    /**
     * Applies permanent redaction blackout/whiteout boxes to the PDF.
     */
    suspend fun applyRedactions(
        context: Context,
        inputStream: InputStream,
        outputStream: OutputStream,
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

            val memSettings = com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMixed(10 * 1024 * 1024, 250 * 1024 * 1024)
            document = PDDocument.load(inputStream, memSettings)
            val totalPages = document.numberOfPages
            val font = PDType1Font.HELVETICA_BOLD

            val boxesByPage = boxes.groupBy { it.pageIndex }

            for ((pageIdx, pageBoxes) in boxesByPage) {
                if (pageIdx !in 0 until totalPages) continue
                val page = document.getPage(pageIdx)
                val cropBox = page.cropBox ?: page.mediaBox
                val rot = ((page.rotation % 360) + 360) % 360
                val visualW = if (rot == 90 || rot == 270) cropBox.height else cropBox.width
                val visualH = if (rot == 90 || rot == 270) cropBox.width else cropBox.height
                val cx = cropBox.lowerLeftX
                val cy = cropBox.lowerLeftY
                val w = cropBox.width
                val h = cropBox.height

                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    for (box in pageBoxes) {
                        val norm = box.normalizedRect
                        
                        // Map visual coordinates to unrotated PDF coordinates
                        val xv = norm.left * visualW
                        val yv = norm.top * visualH
                        val wv = (norm.right - norm.left) * visualW
                        val hv = (norm.bottom - norm.top) * visualH
                        
                        val pdfX1 = when (rot) {
                            90 -> cx + yv
                            180 -> cx + w - xv
                            270 -> cx + w - yv
                            else -> cx + xv
                        }
                        val pdfY1 = when (rot) {
                            90 -> cy + xv
                            180 -> cy + yv
                            270 -> cy + h - xv
                            else -> cy + h - yv
                        }
                        val pdfX2 = when (rot) {
                            90 -> cx + (yv + hv)
                            180 -> cx + w - (xv + wv)
                            270 -> cx + w - (yv + hv)
                            else -> cx + (xv + wv)
                        }
                        val pdfY2 = when (rot) {
                            90 -> cy + (xv + wv)
                            180 -> cy + (yv + hv)
                            270 -> cy + h - (xv + wv)
                            else -> cy + h - (yv + hv)
                        }
                        
                        val drawX = minOf(pdfX1, pdfX2)
                        val drawY = minOf(pdfY1, pdfY2)
                        val drawW = Math.abs(pdfX2 - pdfX1)
                        val drawH = Math.abs(pdfY2 - pdfY1)

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
                        if (overlay.isNotBlank() && wv > 30f && hv > 10f) {
                            val fontSize = (hv * 0.6f).coerceIn(6f, 10f)
                            val textW = (font.getStringWidth(overlay) / 1000f) * fontSize
                            if (textW < wv) {
                                cs.saveGraphicsState()
                                cs.beginText()
                                cs.setFont(font, fontSize)
                                if (config.isBlackout) {
                                    cs.setNonStrokingColor(255, 255, 255)
                                } else {
                                    cs.setNonStrokingColor(80, 80, 80)
                                }
                                
                                // Rotate text to match visual orientation
                                val rad = Math.toRadians((360 - rot).toDouble())
                                val tx = when (rot) {
                                    90 -> drawX + drawW - ((drawW - fontSize) / 2f)
                                    180 -> drawX + drawW - ((drawW - textW) / 2f)
                                    270 -> drawX + ((drawW - fontSize) / 2f)
                                    else -> drawX + ((drawW - textW) / 2f)
                                }
                                val ty = when (rot) {
                                    90 -> drawY + drawH - ((drawH - textW) / 2f)
                                    180 -> drawY + drawH - ((drawH - fontSize) / 2f)
                                    270 -> drawY + ((drawH - textW) / 2f)
                                    else -> drawY + ((drawH - fontSize) / 2f)
                                }
                                
                                val matrix = com.tom_roush.pdfbox.util.Matrix.getRotateInstance(rad, tx, ty)
                                cs.setTextMatrix(matrix)
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
                    val pageCount = if (pfd != null) NativeRendererCoordinator.getPageCount(context, pfd!!) else null
                    val memSettings = com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMixed(10 * 1024 * 1024, 250 * 1024 * 1024)
                    val baseDoc = PDDocument.load(tempFile, memSettings)
                    
                    if (pageCount != null) {
                        newDoc = PDDocument()
                        try {
                            for (i in 0 until pageCount) {
                                if (boxesByPage.containsKey(i)) {
                                    // Forensically flatten only pages with redactions
                                    val pipe = ParcelFileDescriptor.createPipe()
                                    val readFd = pipe[0]
                                    val writeFd = pipe[1]
                                    var pdImage: com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject? = null
                                    
                                    val renderJob = async(Dispatchers.IO) {
                                        NativeRendererCoordinator.renderPageToJpeg(context, pfd!!, i, writeFd)
                                    }
                                    
                                    try {
                                        ParcelFileDescriptor.AutoCloseInputStream(readFd).use { pipeIn ->
                                            pdImage = JPEGFactory.createFromStream(newDoc, pipeIn)
                                        }
                                    } finally {
                                        renderJob.await()
                                    }
                                    
                                    if (pdImage != null) {
                                        val pdfPage = PDPage(PDRectangle(pdImage!!.width.toFloat(), pdImage!!.height.toFloat()))
                                        newDoc.addPage(pdfPage)
                                        PDPageContentStream(newDoc, pdfPage).use { cs ->
                                            cs.drawImage(pdImage!!, 0f, 0f, pdImage!!.width.toFloat(), pdImage!!.height.toFloat())
                                        }
                                    } else {
                                        throw SecurityException("Native renderer failed to yield valid image data.")
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
                        throw SecurityException("Cannot forensically rasterize the PDF because NativeRendererCoordinator is unavailable. Aborting redaction to ensure maximum security without data loss.")
                    }
                } else {
                    tempFile
                }

                finalFile.inputStream().use { inp ->
                    inp.copyTo(outputStream)
                }

                Result.success(boxes.size)
            } finally {
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
        }
    }
}
