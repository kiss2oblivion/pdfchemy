package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.Writer

data class TextMatchOccurrence(
    val pageIndex: Int,
    val matchedText: String,
    val snippet: String,
    val bounds: RectF, // In PDF page coordinate space (origin at bottom-left)
    val fontSize: Float
)

data class FindReplaceSummary(
    val totalMatches: Int,
    val pagesAffected: Int,
    val occurrences: List<TextMatchOccurrence>
)

object PdfFindAndReplaceEngine {

    /**
     * Custom text stripper that captures character bounding boxes and locates query occurrences.
     */
    private class PositionalSearchStripper(
        private val query: String,
        private val matchCase: Boolean
    ) : PDFTextStripper() {

        val matches = mutableListOf<TextMatchOccurrence>()
        private var currentPage = 0
        private val currentLinePositions = mutableListOf<TextPosition>()
        private val currentLineText = StringBuilder()

        fun setTargetPage(page: Int) {
            currentPage = page
            startPage = page + 1
            endPage = page + 1
        }

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            val line = text
            val target = if (matchCase) query else query.lowercase()
            val source = if (matchCase) line else line.lowercase()

            var startIndex = 0
            while (startIndex < source.length) {
                val foundIndex = source.indexOf(target, startIndex)
                if (foundIndex == -1) break

                val endIndex = foundIndex + target.length
                if (endIndex <= textPositions.size) {
                    val matchPositions = textPositions.subList(foundIndex, endIndex)
                    if (matchPositions.isNotEmpty()) {
                        val firstPos = matchPositions.first()
                        val lastPos = matchPositions.last()

                        val minX = firstPos.xDirAdj
                        val maxX = lastPos.xDirAdj + lastPos.widthDirAdj
                        val width = (maxX - minX).coerceAtLeast(1f)
                        val avgHeight = matchPositions.map { it.heightDir }.average().toFloat().coerceAtLeast(8f)
                        val avgFontSize = matchPositions.map { it.fontSizeInPt }.average().toFloat().coerceAtLeast(10f)

                        // yDirAdj is measured from top of page. We will compute standard PDF coordinate (bottom-left) when applying.
                        val topY = firstPos.yDirAdj

                        val snippetStart = (foundIndex - 20).coerceAtLeast(0)
                        val snippetEnd = (endIndex + 20).coerceAtMost(line.length)
                        val snippet = line.substring(snippetStart, snippetEnd).trim()

                        matches.add(
                            TextMatchOccurrence(
                                pageIndex = currentPage,
                                matchedText = line.substring(foundIndex, endIndex),
                                snippet = "...$snippet...",
                                bounds = RectF(minX, topY, minX + width, topY + avgHeight),
                                fontSize = avgFontSize
                            )
                        )
                    }
                }
                startIndex = foundIndex + 1
            }
        }
    }

    /**
     * Finds all text occurrences across all pages of a PDF.
     */
    suspend fun findOccurrences(
        context: Context,
        pdfUri: Uri,
        query: String,
        matchCase: Boolean = false
    ): FindReplaceSummary = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext FindReplaceSummary(0, 0, emptyList())

        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null
        val allMatches = mutableListOf<TextMatchOccurrence>()

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext FindReplaceSummary(0, 0, emptyList())
            document = PDDocument.load(inputStream)

            val stripper = PositionalSearchStripper(query, matchCase)
            val nullWriter = OutputStreamWriter(ByteArrayOutputStream())

            for (pageIndex in 0 until document.numberOfPages) {
                stripper.setTargetPage(pageIndex)
                stripper.writeText(document, nullWriter)
            }
            allMatches.addAll(stripper.matches)
        } catch (e: Exception) {
            AppLogger.e("PdfFindAndReplaceEngine: Error finding text occurrences", e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }

        val pagesAffected = allMatches.map { it.pageIndex }.distinct().size
        return@withContext FindReplaceSummary(allMatches.size, pagesAffected, allMatches)
    }

    /**
     * Replaces occurrences of findText with replaceText across the PDF and writes output to destUri.
     */
    suspend fun replaceAll(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        findText: String,
        replaceText: String,
        matchCase: Boolean = false,
        maskColorRgb: Triple<Float, Float, Float> = Triple(1f, 1f, 1f), // Pure white mask by default
        textColorRgb: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)  // Black text by default
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (findText.isBlank()) return@withContext Result.failure(IllegalArgumentException("Search query cannot be blank"))

        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        return@withContext try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalArgumentException("Cannot open source PDF")
            document = PDDocument.load(inputStream)

            val stripper = PositionalSearchStripper(findText, matchCase)
            val nullWriter = OutputStreamWriter(ByteArrayOutputStream())

            for (pageIndex in 0 until document.numberOfPages) {
                stripper.setTargetPage(pageIndex)
                stripper.writeText(document, nullWriter)
            }

            val matchesByPage = stripper.matches.groupBy { it.pageIndex }
            var totalReplaced = 0

            for ((pageIndex, pageMatches) in matchesByPage) {
                if (pageIndex !in 0 until document.numberOfPages) continue
                val page = document.getPage(pageIndex)
                val cropBox = page.cropBox ?: page.mediaBox
                val pageHeight = cropBox.height

                PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { cs ->
                    for (match in pageMatches) {
                        // Invert topY to PDF bottom-left coordinate space
                        val pdfX = match.bounds.left
                        val pdfWidth = match.bounds.width()
                        val pdfHeight = match.bounds.height()
                        val pdfY = pageHeight - match.bounds.bottom

                        // 1. Draw opaque background rectangle to mask out old text
                        cs.setNonStrokingColor(maskColorRgb.first, maskColorRgb.second, maskColorRgb.third)
                        cs.addRect(pdfX - 1f, pdfY - 1f, pdfWidth + 2f, pdfHeight + 2f)
                        cs.fill()

                        // 2. Draw replacement text if not empty
                        if (replaceText.isNotEmpty()) {
                            cs.beginText()
                            cs.setNonStrokingColor(textColorRgb.first, textColorRgb.second, textColorRgb.third)
                            cs.setFont(PDType1Font.HELVETICA, match.fontSize)
                            cs.newLineAtOffset(pdfX, pdfY + 1f)
                            cs.showText(replaceText)
                            cs.endText()
                        }
                        totalReplaced++
                    }
                }
            }

            val tempFile = File(context.cacheDir, "find_replace_tmp_${System.currentTimeMillis()}.pdf")
            FileOutputStream(tempFile).use { out ->
                document.save(out)
            }

            context.contentResolver.openOutputStream(destPdfUri)?.use { destStream ->
                tempFile.inputStream().use { tempIn ->
                    tempIn.copyTo(destStream)
                }
            } ?: throw IllegalStateException("Cannot open destination output stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(destPdfUri, com.pdfchemy.app.utils.FileUtils.getFileName(context, destPdfUri) ?: "replaced.pdf", "Find & Replace PDF")

            Result.success(totalReplaced)
        } catch (e: Exception) {
            AppLogger.e("PdfFindAndReplaceEngine: Error replacing text", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
