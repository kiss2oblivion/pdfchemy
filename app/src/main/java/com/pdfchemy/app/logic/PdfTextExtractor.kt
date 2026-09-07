package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import com.pdfchemy.app.utils.AppLogger
import java.io.OutputStreamWriter

object PdfTextExtractor {

    suspend fun extractText(context: Context, sourceUri: Uri, destUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            var extractedText = extractUsingPdfBox(context, sourceUri)

            // If the extracted text is too short, it might be a scanned document.
            // Fallback to OCR using ML Kit.
            if (extractedText.trim().length < 50) {
                extractedText = extractUsingOcr(context, sourceUri)
            }

            if (extractedText.isNotBlank()) {
                context.contentResolver.openOutputStream(destUri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(extractedText)
                    }
                }
                true
            } else {
                false
            }
        }
    }

    private fun extractUsingPdfBox(context: Context, sourceUri: Uri): String {
        var document: PDDocument? = null
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                stripper.getText(document)
            } ?: ""
        } catch (e: Exception) {
            AppLogger.e("Error during PDF text extraction", e)
            ""
        } finally {
            document?.close()
        }
    }

    /**
     * Extracts text from all pages in a single linear O(N) pass using a customized PDFTextStripper.
     * Replaces the quadratic O(N^2) page-tree traversal loops previously found across various modules.
     */
    fun extractAllPagesText(document: PDDocument): List<String> {
        val totalPages = document.numberOfPages
        if (totalPages == 0) return emptyList()

        val pagesText = ArrayList<String>(totalPages)
        var currentWriter = java.io.StringWriter()

        val stripper = object : PDFTextStripper() {
            override fun startPage(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
                currentWriter = java.io.StringWriter()
                output = currentWriter
            }

            override fun endPage(page: com.tom_roush.pdfbox.pdmodel.PDPage) {
                output.flush()
                pagesText.add(currentWriter.toString())
            }
        }

        stripper.startPage = 1
        stripper.endPage = totalPages
        try {
            stripper.writeText(document, java.io.StringWriter())
        } catch (e: Exception) {
            AppLogger.e("PdfTextExtractor: Error during single-pass page extraction", e)
        }
        return pagesText
    }

    private suspend fun extractUsingOcr(context: Context, sourceUri: Uri): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val stringBuilder = java.lang.StringBuilder()

        var fileDescriptor: ParcelFileDescriptor? = null
        var pdfRenderer: PdfRenderer? = null

        try {
            fileDescriptor = context.contentResolver.openFileDescriptor(sourceUri, "r")
            if (fileDescriptor != null) {
                pdfRenderer = PdfRenderer(fileDescriptor)
                val pageCount = pdfRenderer.pageCount

                for (i in 0 until pageCount) {
                    var page: PdfRenderer.Page? = null
                    var bitmap: Bitmap? = null
                    try {
                        page = pdfRenderer.openPage(i)
                        // Render the page to a bitmap (using a higher resolution for better OCR)
                        val width = context.resources.displayMetrics.densityDpi / 72 * page.width
                        val height = context.resources.displayMetrics.densityDpi / 72 * page.height
                        
                        bitmap = Bitmap.createBitmap(
                            if (width > 0) width else page.width * 2,
                            if (height > 0) height else page.height * 2,
                            Bitmap.Config.ARGB_8888
                        )
                        
                        // White background
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        
                        val image = InputImage.fromBitmap(bitmap, 0)
                        
                        try {
                            val result = recognizer.process(image).await()
                            stringBuilder.append(result.text).append("\n\n")
                        } catch (e: Exception) {
                            AppLogger.e("Error during PDF text extraction", e)
                        }
                    } finally {
                        bitmap?.recycle()
                        page?.close()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Error during PDF text extraction", e)
        } finally {
            try { pdfRenderer?.close() } catch (_: Exception) {}
            try { fileDescriptor?.close() } catch (_: Exception) {}
            try { recognizer.close() } catch (_: Exception) {}
        }

        return stringBuilder.toString()
    }
}
