package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DocumentMetadataInfo(
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val creator: String = "",
    val producer: String = "",
    val creationDate: String = "",
    val modificationDate: String = "",
    val pageCount: Int = 0,
    val hasXmpMetadata: Boolean = false,
    val isEncrypted: Boolean = false
)

object PdfMetadataEngine {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Reads all standard and extended metadata from a PDF document.
     */
    suspend fun readMetadata(
        context: Context,
        pdfUri: Uri
    ): Result<DocumentMetadataInfo> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val info = document.documentInformation
            val catalog = document.documentCatalog

            val creationStr = try {
                info?.creationDate?.let { dateFormatter.format(it.time) } ?: ""
            } catch (_: Exception) { "" }

            val modStr = try {
                info?.modificationDate?.let { dateFormatter.format(it.time) } ?: ""
            } catch (_: Exception) { "" }

            val hasXmp = catalog?.metadata != null

            val result = DocumentMetadataInfo(
                title = info?.title ?: "",
                author = info?.author ?: "",
                subject = info?.subject ?: "",
                keywords = info?.keywords ?: "",
                creator = info?.creator ?: "",
                producer = info?.producer ?: "",
                creationDate = creationStr,
                modificationDate = modStr,
                pageCount = document.numberOfPages,
                hasXmpMetadata = hasXmp,
                isEncrypted = document.isEncrypted
            )

            Result.success(result)
        } catch (e: Exception) {
            AppLogger.e("PdfMetadataEngine: Error reading metadata", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Updates or completely sanitizes/wipes metadata from a PDF document.
     */
    suspend fun writeOrSanitizeMetadata(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        newMetadata: DocumentMetadataInfo?,
        wipeAllMetadata: Boolean = false
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)

            if (wipeAllMetadata) {
                // Wipe all document information fields
                val blankInfo = PDDocumentInformation()
                blankInfo.title = null
                blankInfo.author = null
                blankInfo.subject = null
                blankInfo.keywords = null
                blankInfo.creator = null
                blankInfo.producer = null
                blankInfo.creationDate = null
                blankInfo.modificationDate = null
                blankInfo.trapped = null
                document.documentInformation = blankInfo

                // Remove XMP XML metadata packet
                document.documentCatalog?.metadata = null
            } else if (newMetadata != null) {
                var info = document.documentInformation
                if (info == null) {
                    info = PDDocumentInformation()
                    document.documentInformation = info
                }

                info.title = newMetadata.title.ifBlank { null }
                info.author = newMetadata.author.ifBlank { null }
                info.subject = newMetadata.subject.ifBlank { null }
                info.keywords = newMetadata.keywords.ifBlank { null }
                info.creator = newMetadata.creator.ifBlank { null }
                info.producer = newMetadata.producer.ifBlank { null }
                info.modificationDate = Calendar.getInstance()
            }

            // Save to temp file first
            val tempFile = File(context.cacheDir, "metadata_temp_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { destStream ->
                tempFile.inputStream().use { tempIn ->
                    tempIn.copyTo(destStream)
                }
            } ?: throw IllegalStateException("Cannot open destination output stream")

            tempFile.delete()

            val actionName = if (wipeAllMetadata) "Sanitized PDF (No Metadata)" else "Updated Metadata PDF"
            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "document.pdf",
                actionName
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfMetadataEngine: Error writing metadata", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
