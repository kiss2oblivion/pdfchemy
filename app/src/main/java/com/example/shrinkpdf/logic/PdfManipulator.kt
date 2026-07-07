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

    suspend fun splitPdf(context: Context, sourceUri: Uri, outputDirectory: DocumentFile, baseName: String) {
        withContext(Dispatchers.IO) {
            var document: PDDocument? = null
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    document = PDDocument.load(inputStream)
                    val splitter = Splitter()
                    val pages = splitter.split(document)
                    
                    pages.forEachIndexed { index, pageDoc ->
                        try {
                            val fileName = "${baseName}_page_${index + 1}.pdf"
                            val newFile = outputDirectory.createFile("application/pdf", fileName)
                        
                            if (newFile != null) {
                                context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                                    pageDoc.save(outputStream)
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
}
