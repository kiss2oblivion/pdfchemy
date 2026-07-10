package com.example.shrinkpdf.logic

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
