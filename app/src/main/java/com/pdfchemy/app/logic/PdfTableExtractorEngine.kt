package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

/**
 * 100% Offline & Local-First PDF Table & Data Extractor.
 * Extracts tabular data, forms, and spreadsheets from PDF documents directly to RFC 4180 CSV
 * using spatial 2D text clustering without lossy cloud dependencies.
 */
object PdfTableExtractorEngine {

    private data class SpatialWord(
        val text: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    suspend fun extractTablesToCsv(context: Context, sourceUri: Uri, pageIndex: Int? = null): String = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                document = PDDocument.load(inputStream)
                val doc = document ?: return@withContext ""
                extractFromDocument(doc, pageIndex)
            } ?: ""
        } catch (e: Exception) {
            AppLogger.e("Failed to extract tables to CSV: ${e.message}", e)
            ""
        } finally {
            document?.close()
        }
    }

    suspend fun extractTablesToCsvFile(context: Context, sourceUri: Uri, destUri: Uri, pageIndex: Int? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val csv = extractTablesToCsv(context, sourceUri, pageIndex)
            if (csv.isNotBlank()) {
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                        writer.write(csv)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to save extracted CSV to file: ${e.message}", e)
            false
        }
    }

    fun extractFromDocument(doc: PDDocument, pageIndex: Int? = null): String {
        val totalPages = doc.numberOfPages
        if (totalPages == 0) return ""

        val startPage = if (pageIndex != null) (pageIndex + 1).coerceIn(1, totalPages) else 1
        val endPage = if (pageIndex != null) (pageIndex + 1).coerceIn(1, totalPages) else totalPages

        val allWords = mutableListOf<SpatialWord>()
        val currentWordChars = StringBuilder()
        var wordStartX = 0f
        var wordStartY = 0f
        var wordEndX = 0f
        var wordMaxHeight = 0f
        var inWord = false

        fun flushWord() {
            if (inWord && currentWordChars.isNotEmpty()) {
                val str = currentWordChars.toString().trim()
                if (str.isNotEmpty()) {
                    allWords.add(
                        SpatialWord(
                            text = str,
                            x = wordStartX,
                            y = wordStartY,
                            width = (wordEndX - wordStartX).coerceAtLeast(1f),
                            height = wordMaxHeight
                        )
                    )
                }
                currentWordChars.setLength(0)
                inWord = false
            }
        }

        val stripper = object : PDFTextStripper() {
            override fun processTextPosition(text: TextPosition) {
                val str = text.unicode
                val isSpace = str.isBlank()

                if (isSpace) {
                    flushWord()
                } else {
                    val x = text.xDirAdj
                    val y = text.yDirAdj
                    val w = text.widthDirAdj
                    val h = text.heightDir

                    if (inWord) {
                        val gap = x - wordEndX
                        if (Math.abs(y - wordStartY) < 3.0f && gap >= -1.0f && gap <= (text.widthOfSpace * 1.5f).coerceAtLeast(3f)) {
                            currentWordChars.append(str)
                            wordEndX = x + w
                            wordMaxHeight = maxOf(wordMaxHeight, h)
                        } else {
                            flushWord()
                            currentWordChars.append(str)
                            wordStartX = x
                            wordStartY = y
                            wordEndX = x + w
                            wordMaxHeight = h
                            inWord = true
                        }
                    } else {
                        currentWordChars.append(str)
                        wordStartX = x
                        wordStartY = y
                        wordEndX = x + w
                        wordMaxHeight = h
                        inWord = true
                    }
                }
                super.processTextPosition(text)
            }
        }
        stripper.startPage = startPage
        stripper.endPage = endPage
        stripper.writeText(doc, java.io.StringWriter())
        flushWord()

        if (allWords.isEmpty()) return ""

        // Group into words based on horizontal proximity on the same horizontal line
        val lineThreshold = 3.5f
        val sortedByY = allWords.sortedWith(compareBy({ it.y }, { it.x }))

        val lines = mutableListOf<MutableList<SpatialWord>>()
        var currentLine = mutableListOf<SpatialWord>()
        var currentY = -1f

        for (item in sortedByY) {
            if (currentY < 0f || Math.abs(item.y - currentY) <= lineThreshold) {
                currentLine.add(item)
                if (currentY < 0f) currentY = item.y
            } else {
                if (currentLine.isNotEmpty()) {
                    currentLine.sortBy { it.x }
                    lines.add(currentLine)
                }
                currentLine = mutableListOf(item)
                currentY = item.y
            }
        }
        if (currentLine.isNotEmpty()) {
            currentLine.sortBy { it.x }
            lines.add(currentLine)
        }

        // Within each line, assemble words and split into cells when horizontal gap > columnGapThreshold
        val columnGapThreshold = 14.0f
        val csvRows = mutableListOf<List<String>>()

        for (line in lines) {
            val cells = mutableListOf<String>()
            val currentCellText = StringBuilder()
            var lastRight = -1f

            for (w in line) {
                if (lastRight >= 0f && (w.x - lastRight) > columnGapThreshold) {
                    cells.add(currentCellText.toString().trim())
                    currentCellText.clear()
                } else if (lastRight >= 0f && (w.x - lastRight) > 2.0f) {
                    currentCellText.append(" ")
                }
                currentCellText.append(w.text)
                lastRight = w.x + w.width
            }
            if (currentCellText.isNotEmpty()) {
                cells.add(currentCellText.toString().trim())
            }

            if (cells.isNotEmpty() && cells.any { it.isNotBlank() }) {
                csvRows.add(cells)
            }
        }

        // Format into RFC 4180 CSV
        val sb = StringBuilder()
        for (row in csvRows) {
            val rowStr = row.joinToString(",") { cell ->
                val escaped = cell.replace("\"", "\"\"")
                if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
                    "\"$escaped\""
                } else {
                    escaped
                }
            }
            sb.append(rowStr).append("\r\n")
        }

        return sb.toString()
    }
}
