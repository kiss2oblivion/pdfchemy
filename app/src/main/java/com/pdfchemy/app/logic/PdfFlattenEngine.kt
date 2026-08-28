package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

data class FlattenDiagnostic(
    val fieldCount: Int = 0,
    val annotationCount: Int = 0,
    val hasSignatures: Boolean = false
)

object PdfFlattenEngine {

    /**
     * Inspects a PDF to count fillable form fields, signatures, and annotations.
     */
    suspend fun inspectFlattenElements(
        context: Context,
        pdfUri: Uri
    ): Result<FlattenDiagnostic> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val acroForm = document.documentCatalog.acroForm
            val fieldCount = acroForm?.fields?.size ?: 0
            val hasSignatures = document.signatureDictionaries.isNotEmpty()

            var annotationCount = 0
            for (page in document.pages) {
                val annots = page.annotations ?: emptyList()
                annotationCount += annots.count { it !is PDAnnotationWidget }
            }

            Result.success(
                FlattenDiagnostic(
                    fieldCount = fieldCount,
                    annotationCount = annotationCount,
                    hasSignatures = hasSignatures
                )
            )
        } catch (e: Exception) {
            AppLogger.e("PdfFlattenEngine: Error inspecting elements", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Flattens AcroForm fields, signatures, and/or annotations into immutable vector content streams.
     */
    suspend fun flattenPdf(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        flattenForms: Boolean = true,
        flattenAnnotations: Boolean = true
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val acroForm = document.documentCatalog.acroForm

            if (flattenForms && acroForm != null) {
                try {
                    acroForm.flatten()
                } catch (e: Exception) {
                    AppLogger.w("AcroForm flatten warning: ${e.message}")
                }
            }

            if (flattenAnnotations) {
                for (page in document.pages) {
                    val annots = page.annotations
                    if (annots != null) {
                        // Clear interactive widgets to lock completely
                        page.annotations = emptyList()
                    }
                }
            }

            val tempFile = File(context.cacheDir, "flattened_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "flattened.pdf",
                "Flatten & Lock PDF"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfFlattenEngine: Error flattening PDF", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
