package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class PdfMetadata(
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val creator: String = "",
    val producer: String = ""
)

class PdfMetadataManager {

    suspend fun getMetadata(context: Context, uri: Uri): Result<PdfMetadata> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Failed to open file"))

            document = PDDocument.load(inputStream)
            val info = document.documentInformation

            val metadata = PdfMetadata(
                title = info?.title ?: "",
                author = info?.author ?: "",
                subject = info?.subject ?: "",
                keywords = info?.keywords ?: "",
                creator = info?.creator ?: "",
                producer = info?.producer ?: ""
            )
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document?.close()
            inputStream?.close()
        }
    }

    suspend fun updateMetadata(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        newMetadata: PdfMetadata
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to open source file"))

            document = PDDocument.load(inputStream)
            
            val info = document.documentInformation ?: PDDocumentInformation()
            info.title = newMetadata.title.takeIf { it.isNotBlank() }
            info.author = newMetadata.author.takeIf { it.isNotBlank() }
            info.subject = newMetadata.subject.takeIf { it.isNotBlank() }
            info.keywords = newMetadata.keywords.takeIf { it.isNotBlank() }
            info.creator = newMetadata.creator.takeIf { it.isNotBlank() }
            info.producer = newMetadata.producer.takeIf { it.isNotBlank() }
            
            document.documentInformation = info

            outputStream = context.contentResolver.openOutputStream(destUri)
                ?: return@withContext Result.failure(Exception("Failed to open destination file"))
            
            document.save(outputStream)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document?.close()
            inputStream?.close()
            outputStream?.close()
        }
    }

    suspend fun clearMetadata(
        context: Context,
        sourceUri: Uri,
        destUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to open source file"))

            document = PDDocument.load(inputStream)
            
            // Clear standard fields
            val info = document.documentInformation
            if (info != null) {
                info.title = null
                info.author = null
                info.subject = null
                info.keywords = null
                info.creator = null
                info.producer = null
                // pdfbox allows arbitrary keys, we can get them and remove them
                val keys = info.metadataKeys.toList()
                for (key in keys) {
                    info.setCustomMetadataValue(key, null)
                }
            }
            
            // Remove XML metadata
            val catalog = document.documentCatalog
            if (catalog != null) {
                catalog.metadata = null
            }

            outputStream = context.contentResolver.openOutputStream(destUri)
                ?: return@withContext Result.failure(Exception("Failed to open destination file"))
            
            document.save(outputStream)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document?.close()
            inputStream?.close()
            outputStream?.close()
        }
    }

    suspend fun clearMetadataOverwrite(
        context: Context,
        sourceUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        var tempFile: java.io.File? = null
        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Failed to open source file"))

            document = PDDocument.load(inputStream)
            
            // Clear standard fields
            val info = document.documentInformation
            if (info != null) {
                info.title = null
                info.author = null
                info.subject = null
                info.keywords = null
                info.creator = null
                info.producer = null
                val keys = info.metadataKeys.toList()
                for (key in keys) {
                    info.setCustomMetadataValue(key, null)
                }
            }
            
            // Remove XML metadata
            val catalog = document.documentCatalog
            if (catalog != null) {
                catalog.metadata = null
            }

            tempFile = java.io.File.createTempFile("temp_stripped", ".pdf", context.cacheDir)
            document.save(tempFile)
            document.close()
            document = null
            inputStream.close()
            inputStream = null

            context.contentResolver.openOutputStream(sourceUri, "wt")?.use { outputStream ->
                tempFile.inputStream().use { it.copyTo(outputStream) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document?.close()
            inputStream?.close()
            tempFile?.delete()
        }
    }
}
