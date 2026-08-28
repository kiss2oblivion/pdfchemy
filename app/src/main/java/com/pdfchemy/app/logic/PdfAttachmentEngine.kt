package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentNameDictionary
import com.tom_roush.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode
import com.tom_roush.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification
import com.tom_roush.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.Calendar

data class PdfAttachment(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val description: String? = null
)

object PdfAttachmentEngine {

    /**
     * Lists all embedded file attachments in a PDF.
     */
    suspend fun listAttachments(
        context: Context,
        pdfUri: Uri
    ): Result<List<PdfAttachment>> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val names = document.documentCatalog.names
            val embeddedFiles = names?.embeddedFiles
            val result = mutableListOf<PdfAttachment>()

            if (embeddedFiles != null) {
                val map = embeddedFiles.names ?: emptyMap()
                for ((key, fileSpec) in map) {
                    if (fileSpec is PDComplexFileSpecification) {
                        val ef = fileSpec.embeddedFile
                        val size = ef?.size?.toLong() ?: 0L
                        val mime = ef?.subtype ?: "application/octet-stream"
                        result.add(
                            PdfAttachment(
                                name = fileSpec.filename ?: key,
                                sizeBytes = size,
                                mimeType = mime,
                                description = fileSpec.fileDescription
                            )
                        )
                    }
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            AppLogger.e("PdfAttachmentEngine: Error listing attachments", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Extracts an embedded file from the PDF to a destination URI.
     */
    suspend fun extractAttachment(
        context: Context,
        pdfUri: Uri,
        attachmentName: String,
        destUri: Uri
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val names = document.documentCatalog.names
            val embeddedFiles = names?.embeddedFiles
                ?: return@withContext Result.failure(IllegalStateException("No embedded files found"))

            val map = embeddedFiles.names ?: emptyMap()
            var targetFileSpec: PDComplexFileSpecification? = null

            for ((key, spec) in map) {
                if (spec is PDComplexFileSpecification) {
                    if (spec.filename == attachmentName || key == attachmentName) {
                        targetFileSpec = spec
                        break
                    }
                }
            }

            val ef = targetFileSpec?.embeddedFile
                ?: return@withContext Result.failure(IllegalStateException("Attachment not found"))

            val dataBytes = ef.createInputStream().readBytes()

            context.contentResolver.openOutputStream(destUri)?.use { out ->
                out.write(dataBytes)
            } ?: throw IllegalStateException("Cannot open destination stream")

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfAttachmentEngine: Error extracting attachment", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Embeds a new file attachment into the PDF document.
     */
    suspend fun embedAttachment(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        fileToEmbedUri: Uri,
        customFileName: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var srcPdfStream: InputStream? = null
        var attachStream: InputStream? = null
        var document: PDDocument? = null

        try {
            srcPdfStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open source PDF")
            attachStream = context.contentResolver.openInputStream(fileToEmbedUri)
                ?: throw IllegalStateException("Cannot open file to attach")

            document = PDDocument.load(srcPdfStream)
            val fileName = customFileName ?: FileUtils.getFileName(context, fileToEmbedUri) ?: "attachment.dat"
            val fileBytes = attachStream.readBytes()

            val namesDict = document.documentCatalog.names ?: PDDocumentNameDictionary(document.documentCatalog).also {
                document.documentCatalog.names = it
            }

            val embeddedTree = namesDict.embeddedFiles ?: PDEmbeddedFilesNameTreeNode().also {
                namesDict.embeddedFiles = it
            }

            val currentMap = (embeddedTree.names ?: emptyMap()).toMutableMap()

            val fileSpec = PDComplexFileSpecification()
            fileSpec.file = fileName

            val embeddedFile = PDEmbeddedFile(document, ByteArrayInputStream(fileBytes))
            embeddedFile.size = fileBytes.size
            embeddedFile.creationDate = Calendar.getInstance()
            fileSpec.embeddedFile = embeddedFile

            currentMap[fileName] = fileSpec
            embeddedTree.setNames(currentMap)

            val tempFile = File(context.cacheDir, "attached_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "attached.pdf",
                "Embedded File ($fileName)"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfAttachmentEngine: Error embedding attachment", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { srcPdfStream?.close() } catch (_: Exception) {}
            try { attachStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Removes an attachment from the PDF.
     */
    suspend fun removeAttachment(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        attachmentName: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val names = document.documentCatalog.names
            val embeddedFiles = names?.embeddedFiles

            if (embeddedFiles != null) {
                val map = (embeddedFiles.names ?: emptyMap()).toMutableMap()
                val keyToRemove = map.keys.firstOrNull { key ->
                    val spec = map[key]
                    (spec is PDComplexFileSpecification && spec.filename == attachmentName) || key == attachmentName
                }

                if (keyToRemove != null) {
                    map.remove(keyToRemove)
                    embeddedFiles.setNames(map)
                }
            }

            val tempFile = File(context.cacheDir, "rm_attach_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF")

            tempFile.delete()

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfAttachmentEngine: Error removing attachment", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
