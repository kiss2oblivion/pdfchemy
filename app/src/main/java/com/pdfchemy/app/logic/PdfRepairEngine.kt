package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

data class RepairDiagnostic(
    val hasValidHeader: Boolean = false,
    val hasValidEof: Boolean = false,
    val recoveredPages: Int = 0,
    val isEncrypted: Boolean = false,
    val issueSummary: String = ""
)

object PdfRepairEngine {

    /**
     * Inspects a potentially damaged PDF file to detect corruption and syntax issues.
     */
    suspend fun diagnosePdf(
        context: Context,
        pdfUri: Uri
    ): Result<RepairDiagnostic> = withContext(Dispatchers.IO) {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            val buffer = ByteArrayOutputStream()
            inputStream.copyTo(buffer)
            val bytes = buffer.toByteArray()

            if (bytes.size < 10) {
                return@withContext Result.success(
                    RepairDiagnostic(
                        hasValidHeader = false,
                        hasValidEof = false,
                        recoveredPages = 0,
                        issueSummary = "File is empty or too small"
                    )
                )
            }

            val headerStr = String(bytes.take(20).toByteArray(), Charsets.US_ASCII)
            val hasValidHeader = headerStr.contains("%PDF-")

            val tailStr = String(bytes.takeLast(100).toByteArray(), Charsets.US_ASCII)
            val hasValidEof = tailStr.contains("%%EOF")

            var pageCount = 0
            var isEncrypted = false
            var issueSummary = mutableListOf<String>()

            if (!hasValidHeader) issueSummary.add("Corrupted or missing %PDF header")
            if (!hasValidEof) issueSummary.add("Missing or truncated %%EOF marker")

            try {
                PDFBoxResourceLoader.init(context)
                val doc = PDDocument.load(bytes)
                pageCount = doc.numberOfPages
                isEncrypted = doc.isEncrypted
                doc.close()
            } catch (e: Exception) {
                issueSummary.add("Syntax or XRef table corruption: ${e.message?.take(60)}")
            }

            val summaryText = if (issueSummary.isEmpty()) "Standard PDF structure" else issueSummary.joinToString("; ")
            Result.success(
                RepairDiagnostic(
                    hasValidHeader = hasValidHeader,
                    hasValidEof = hasValidEof,
                    recoveredPages = pageCount,
                    isEncrypted = isEncrypted,
                    issueSummary = summaryText
                )
            )
        } catch (e: Exception) {
            AppLogger.e("PdfRepairEngine: Error diagnosing PDF", e)
            Result.failure(e)
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Repairs and reconstructs damaged PDF files by repairing headers, trailers, and building a clean XRef table.
     */
    suspend fun repairPdf(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            val buffer = ByteArrayOutputStream()
            inputStream.copyTo(buffer)
            var bytes = buffer.toByteArray()

            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Source file is empty"))
            }

            // 1. Repair header if missing or offset
            val headerIndex = indexOfBytes(bytes, "%PDF-".toByteArray(Charsets.US_ASCII))
            if (headerIndex > 0) {
                // Strip leading junk bytes
                bytes = bytes.copyOfRange(headerIndex, bytes.size)
            } else if (headerIndex < 0) {
                // Prepend standard PDF-1.7 header
                val header = "%PDF-1.7\n".toByteArray(Charsets.US_ASCII)
                bytes = header + bytes
            }

            // 2. Append %%EOF if truncated
            val tailStr = String(bytes.takeLast(60).toByteArray(), Charsets.US_ASCII)
            if (!tailStr.contains("%%EOF")) {
                val eofBytes = "\n%%EOF\n".toByteArray(Charsets.US_ASCII)
                bytes = bytes + eofBytes
            }

            // 3. Load with PDFBox parser and resave to generate fresh, valid XRef table
            document = PDDocument.load(bytes)
            val recoveredPageCount = document.numberOfPages

            val tempFile = File(context.cacheDir, "repaired_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination output stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "repaired.pdf",
                "Repaired Corrupted PDF"
            )

            Result.success(recoveredPageCount)
        } catch (e: Exception) {
            AppLogger.e("PdfRepairEngine: Error repairing PDF", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun indexOfBytes(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        for (i in 0..source.size - target.size) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
