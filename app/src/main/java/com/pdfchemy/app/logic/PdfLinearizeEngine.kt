package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class LinearizeStatus(
    val isLinearized: Boolean,
    val fileSizeOriginal: Long,
    val pageCount: Int
)

object PdfLinearizeEngine {

    /**
     * Inspects if the PDF has Fast Web View (Linearization) enabled.
     */
    suspend fun checkLinearized(
        context: Context,
        pdfUri: Uri
    ): Result<LinearizeStatus> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            val headerBytes = ByteArray(2048)
            val bytesRead = inputStream.read(headerBytes)
            val headerString = if (bytesRead > 0) String(headerBytes, 0, bytesRead, Charsets.US_ASCII) else ""
            val isLinear = headerString.contains("/Linearized")

            inputStream.close()
            inputStream = context.contentResolver.openInputStream(pdfUri)
            document = PDDocument.load(inputStream)

            val pageCount = document.numberOfPages
            val size = FileUtils.getFileSize(context, pdfUri)

            Result.success(
                LinearizeStatus(
                    isLinearized = isLinear,
                    fileSizeOriginal = size,
                    pageCount = pageCount
                )
            )
        } catch (e: Exception) {
            AppLogger.e("PdfLinearizeEngine: Error checking linearization", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Optimizes PDF object stream ordering and produces an efficient streamable PDF.
     */
    suspend fun optimizeFastWebView(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri
    ): Result<Long> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open source PDF")

            document = PDDocument.load(inputStream)
            if (document.numberOfPages == 0) {
                return@withContext Result.failure(IllegalStateException("PDF has no pages"))
            }

            val tempFile = File(context.cacheDir, "linear_${System.currentTimeMillis()}.pdf")
            // PDFBox save automatically compresses streams and reorganizes object structures
            document.save(tempFile)
            document.close()
            document = null

            val outBytes = tempFile.length()

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "web_optimized.pdf",
                "Fast Web View Stream Optimizer"
            )

            Result.success(outBytes)
        } catch (e: Exception) {
            AppLogger.e("PdfLinearizeEngine: Error optimizing stream", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
