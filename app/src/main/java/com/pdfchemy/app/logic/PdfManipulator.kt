package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.documentfile.provider.DocumentFile
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object PdfManipulator {

    suspend fun mergePdfs(context: Context, sourceUris: List<Uri>, outputUri: Uri) {
        withContext(Dispatchers.IO) {
            val merger = PDFMergerUtility()
            val inputStreams = mutableListOf<InputStream>()
            
            try {
                // Add all sources
                sourceUris.forEach { uri ->
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        inputStreams.add(inputStream)
                        merger.addSource(inputStream)
                    }
                }
                
                // Merge to output
                context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                    merger.destinationStream = outputStream
                    // Tom-Roush pdfbox-android 2.0.27
                    merger.mergeDocuments(null) 
                }
            } finally {
                inputStreams.forEach { it.close() }
            }
        }
    }

    suspend fun splitPdf(context: Context, sourceUri: Uri, outputDirectory: DocumentFile, baseName: String, pageRange: String? = null) {
        withContext(Dispatchers.IO) {
            var document: PDDocument? = null
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    document = PDDocument.load(inputStream)
                    val splitter = Splitter()
                    val pages = splitter.split(document)
                    
                    val pagesToKeep = parsePageRange(pageRange, pages.size)

                    pages.forEachIndexed { index, pageDoc ->
                        try {
                            if (pagesToKeep.contains(index + 1)) {
                                val fileName = "${baseName}_page_${index + 1}.pdf"
                                val newFile = outputDirectory.createFile("application/pdf", fileName)
                            
                                if (newFile != null) {
                                    context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                        pageDoc.save(outputStream)
                                    }
                                }
                            }
                        } finally {
                            pageDoc.close()
                        }
                    }
                }
            } finally {
                document?.close()
            }
        }
    }

    suspend fun deletePages(context: Context, sourceUri: Uri, destUri: Uri, pageRange: String) {
        withContext(Dispatchers.IO) {
            var document: PDDocument? = null
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    document = PDDocument.load(inputStream)
                    val doc = document ?: return@use
                    val totalPages = doc.numberOfPages
                    val pagesToDelete = parsePageRange(pageRange, totalPages)
                    if (pagesToDelete.size >= totalPages) {
                        throw IllegalArgumentException("Cannot delete all pages. A PDF must contain at least one page.")
                    }

                    // Remove backwards to avoid index shifting
                    for (i in totalPages - 1 downTo 0) {
                        if (pagesToDelete.contains(i + 1)) {
                            doc.removePage(i)
                        }
                    }

                    context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                        doc.save(outputStream)
                    }
                }
            } finally {
                document?.close()
            }
        }
    }

    suspend fun rotatePdf(context: Context, sourceUri: Uri, destUri: Uri, degrees: Int, pageRange: String? = null) {
        withContext(Dispatchers.IO) {
            var document: PDDocument? = null
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    document = PDDocument.load(inputStream)
                    val doc = document ?: return@withContext
                    val totalPages = doc.numberOfPages
                    val pagesToRotate = parsePageRange(pageRange, totalPages)

                    for (i in 0 until totalPages) {
                        if (pagesToRotate.contains(i + 1)) {
                            val page = doc.getPage(i)
                            // Rotation in PDF must be a multiple of 90
                            var newRotation = (page.rotation + degrees) % 360
                            if (newRotation < 0) newRotation += 360
                            page.rotation = newRotation
                        }
                    }

                    context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                        doc.save(outputStream)
                    }
                }
            } finally {
                document?.close()
            }
        }
    }

    suspend fun protectPdf(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        userPassword: String,
        ownerPassword: String = userPassword,
        keyLength: Int = 128
    ) = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                document = PDDocument.load(inputStream)
                val doc = document ?: throw Exception("Failed to load PDF document")
                
                val accessPermission = AccessPermission()
                val protectionPolicy = StandardProtectionPolicy(ownerPassword, userPassword, accessPermission).apply {
                    encryptionKeyLength = keyLength
                    permissions = accessPermission
                }
                
                doc.protect(protectionPolicy)
                
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    doc.save(outputStream)
                } ?: throw Exception("Failed to open destination stream")
            }
        } finally {
            document?.close()
        }
    }

    suspend fun unlockPdf(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        password: String
    ) = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                document = PDDocument.load(inputStream, password)
                val doc = document ?: throw Exception("Failed to load PDF document with provided password")
                
                if (doc.isEncrypted) {
                    doc.isAllSecurityToBeRemoved = true
                }
                
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    doc.save(outputStream)
                } ?: throw Exception("Failed to open destination stream")
            }
        } finally {
            document?.close()
        }
    }

    suspend fun isPdfPasswordProtected(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val doc = PDDocument.load(inputStream, "")
                val isEncrypted = doc.isEncrypted
                doc.close()
                isEncrypted
            } ?: false
        } catch (e: InvalidPasswordException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun convertPdfToImages(
        context: Context,
        sourceUri: Uri,
        outputDirectory: DocumentFile,
        baseName: String,
        formatName: String = "JPEG",
        quality: Int = 90,
        targetWidth: Int = 1440
    ): List<Uri> = withContext(Dispatchers.IO) {
        val outputUris = mutableListOf<Uri>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = if (sourceUri.scheme == "file") {
                val path = sourceUri.path
                if (path != null) ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY) else null
            } else {
                context.contentResolver.openFileDescriptor(sourceUri, "r")
            }

            if (pfd != null) {
                try {
                    renderer = PdfRenderer(pfd)
                    val pageCount = renderer.pageCount
                    
                    val isPng = formatName.equals("PNG", ignoreCase = true)
                    val mimeType = if (isPng) "image/png" else "image/jpeg"
                    val extension = if (isPng) "png" else "jpg"
                    val compressFormat = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                    
                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        try {
                            val originalWidth = page.width.coerceAtLeast(1)
                            val originalHeight = page.height.coerceAtLeast(1)
                            val scale = targetWidth.toFloat() / originalWidth.toFloat()
                            val renderWidth = targetWidth
                            val renderHeight = (originalHeight * scale).toInt().coerceAtLeast(1)
                            
                            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(Color.WHITE)
                            
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            
                            val fileName = "${baseName}_page_${i + 1}.$extension"
                            val newFile = outputDirectory.createFile(mimeType, fileName)
                            if (newFile != null) {
                                val outStream = if (newFile.uri.scheme == "file") {
                                    java.io.FileOutputStream(File(newFile.uri.path ?: ""))
                                } else {
                                    context.contentResolver.openOutputStream(newFile.uri)
                                }
                                outStream?.use { stream ->
                                    bitmap.compress(compressFormat, quality, stream)
                                }
                                outputUris.add(newFile.uri)
                            }
                            bitmap.recycle()
                        } finally {
                            page.close()
                        }
                    }
                } catch (e: Exception) {
                    com.pdfchemy.app.utils.AppLogger.e("Failed to render PDF pages: ${e.message}", e)
                }
            }
        } finally {
            renderer?.close()
            pfd?.close()
        }
        outputUris
    }

    private fun parsePageRange(rangeStr: String?, totalPages: Int): Set<Int> {
        if (rangeStr.isNullOrBlank()) {
            return (1..totalPages).toSet()
        }
        val pages = mutableSetOf<Int>()
        val parts = rangeStr.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val bounds = trimmed.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        pages.addAll((start..end).toList())
                    }
                }
            } else {
                val single = trimmed.toIntOrNull()
                if (single != null) {
                    pages.add(single)
                }
            }
        }
        return pages.filter { it in 1..totalPages }.toSet()
    }
}

