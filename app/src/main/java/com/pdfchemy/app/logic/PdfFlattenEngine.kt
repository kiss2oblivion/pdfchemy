package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
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
     * Flattens AcroForm fields, signatures, and/or annotations into immutable vector and high-resolution content streams.
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
        var intermediateFile: File? = null
        var finalFile: File? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val acroForm = document.documentCatalog.acroForm

            if (flattenForms && acroForm != null) {
                try {
                    if (acroForm.defaultResources == null) {
                        val dr = com.tom_roush.pdfbox.pdmodel.PDResources()
                        dr.put(COSName.getPDFName("Helv"), com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA)
                        acroForm.defaultResources = dr
                    }
                    acroForm.flatten()
                } catch (e: Exception) {
                    AppLogger.w("AcroForm flatten warning: ${e.message}")
                }
            }

            // Check which pages have non-widget annotations to flatten
            val pagesToFlatten = mutableSetOf<Int>()
            if (flattenAnnotations) {
                for ((idx, page) in document.pages.withIndex()) {
                    val annots = page.annotations ?: emptyList()
                    val hasVisualAnnotations = annots.any { it !is PDAnnotationWidget }
                    if (hasVisualAnnotations) {
                        pagesToFlatten.add(idx)
                    }
                }
            }

            if (pagesToFlatten.isNotEmpty()) {
                // Save intermediate PDF with forms flattened, ready for visual page baking
                intermediateFile = File(context.cacheDir, "intermediate_flatten_${System.currentTimeMillis()}.pdf")
                document.save(intermediateFile)
                document.close()
                document = null

                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(intermediateFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    val baseDoc = PDDocument.load(intermediateFile)
                    val bakedDoc = PDDocument()

                    for (i in 0 until renderer.pageCount) {
                        if (pagesToFlatten.contains(i)) {
                            var renderPage: PdfRenderer.Page? = null
                            var bmp: Bitmap? = null
                            try {
                                renderPage = renderer.openPage(i)
                                val maxDim = 2048
                                val scale = minOf(2f, maxDim.toFloat() / maxOf(renderPage.width, renderPage.height).coerceAtLeast(1))
                                val targetW = (renderPage.width * scale).toInt().coerceAtLeast(1)
                                val targetH = (renderPage.height * scale).toInt().coerceAtLeast(1)
                                bmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)

                                renderPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pdfPage = PDPage(PDRectangle(renderPage.width.toFloat(), renderPage.height.toFloat()))
                                bakedDoc.addPage(pdfPage)

                                val pdImage = JPEGFactory.createFromImage(bakedDoc, bmp, 0.95f)
                                PDPageContentStream(bakedDoc, pdfPage).use { cs ->
                                    cs.drawImage(pdImage, 0f, 0f, renderPage.width.toFloat(), renderPage.height.toFloat())
                                }
                            } finally {
                                bmp?.recycle()
                                renderPage?.close()
                            }
                        } else {
                            if (i < baseDoc.numberOfPages) {
                                bakedDoc.importPage(baseDoc.getPage(i))
                            }
                        }
                    }
                    baseDoc.close()
                    document = bakedDoc
                } catch (e: Exception) {
                    AppLogger.w("PdfFlattenEngine: PdfRenderer unavailable, falling back to direct save: ${e.message}")
                    document = PDDocument.load(intermediateFile)
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            }

            finalFile = File(context.cacheDir, "flattened_${System.currentTimeMillis()}.pdf")
            document?.save(finalFile)

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                finalFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF stream")

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
            intermediateFile?.delete()
            finalFile?.delete()
        }
    }
}
