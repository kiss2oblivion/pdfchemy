package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ComicBookEngine {

    /**
     * Converts a PDF (Manga/Comic) into a standard CBZ (Comic Book Zip) archive.
     */
    suspend fun pdfToCbz(
        context: Context,
        sourcePdfUri: Uri,
        destCbzUri: Uri,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val tempCbzFile = File(context.cacheDir, "comic_${System.currentTimeMillis()}.cbz")
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = context.contentResolver.openFileDescriptor(sourcePdfUri, "r")
                ?: throw IllegalStateException("Cannot open source PDF descriptor")

            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages == 0) {
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            ZipOutputStream(FileOutputStream(tempCbzFile)).use { zipOut ->
                for (pageIndex in 0 until totalPages) {
                    val page = renderer.openPage(pageIndex)
                    try {
                        val maxDim = 2048
                        val scale = minOf(2f, maxDim.toFloat() / maxOf(page.width, page.height).coerceAtLeast(1))
                        val targetW = (page.width * scale).toInt().coerceAtLeast(1)
                        val targetH = (page.height * scale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.RGB_565)
                        try {
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val entryName = "page_%04d.jpg".format(pageIndex + 1)
                            zipOut.putNextEntry(ZipEntry(entryName))
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, zipOut)
                            zipOut.closeEntry()
                        } finally {
                            bitmap.recycle()
                        }
                    } finally {
                        page.close()
                    }

                    onProgress(pageIndex + 1, totalPages)
                }
            }

            context.contentResolver.openOutputStream(destCbzUri)?.use { outStream ->
                tempCbzFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Cannot open destination CBZ output stream")

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destCbzUri,
                FileUtils.getFileName(context, destCbzUri) ?: "comic.cbz",
                "PDF to Comic CBZ"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("ComicBookEngine: Error converting PDF to CBZ", e)
            Result.failure(e)
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            if (tempCbzFile.exists()) {
                tempCbzFile.delete()
            }
        }
    }

    /**
     * Converts a CBZ (Comic Book Zip) archive into a formatted PDF document.
     */
    suspend fun cbzToPdf(
        context: Context,
        sourceCbzUri: Uri,
        destPdfUri: Uri,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        val tempCbzFile = File(context.cacheDir, "source_comic_${System.currentTimeMillis()}.cbz")
        val tempPdfFile = File(context.cacheDir, "comic_to_pdf_${System.currentTimeMillis()}.pdf")
        var zipFile: ZipFile? = null
        var document: PDDocument? = null

        try {
            // Stream source CBZ to a temporary cache file to avoid loading archive contents into RAM
            context.contentResolver.openInputStream(sourceCbzUri)?.use { input ->
                tempCbzFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Cannot open source CBZ stream")

            zipFile = ZipFile(tempCbzFile)
            val entries = zipFile.entries().asSequence()
                .filter { entry ->
                    if (entry.isDirectory) false
                    else {
                        val name = entry.name.lowercase()
                        name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
                    }
                }
                .sortedBy { it.name }
                .toList()

            if (entries.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No valid comic images found in CBZ archive"))
            }

            document = PDDocument()
            val total = entries.size
            for ((idx, entry) in entries.withIndex()) {
                // 1. Decode bounds first to prevent decompression bomb / OOM attacks
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                zipFile.getInputStream(entry).use { entryStream ->
                    BitmapFactory.decodeStream(entryStream, null, boundsOptions)
                }
                val rawW = boundsOptions.outWidth
                val rawH = boundsOptions.outHeight
                if (rawW <= 0 || rawH <= 0 || rawW > 8192 || rawH > 8192) {
                    AppLogger.w("ComicBookEngine: Skipping invalid or excessively large comic image in entry '${entry.name}' (${rawW}x${rawH})")
                    continue
                }

                // Downsample if dimension exceeds max safe page dimension (2560px)
                val maxDim = maxOf(rawW, rawH)
                var sampleSize = 1
                while (maxDim / sampleSize > 2560) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }

                zipFile.getInputStream(entry).use { entryStream ->
                    val bitmap = BitmapFactory.decodeStream(entryStream, null, decodeOptions)
                    if (bitmap != null) {
                        try {
                            val pageWidth = bitmap.width.toFloat()
                            val pageHeight = bitmap.height.toFloat()
                            val pageRect = PDRectangle(pageWidth, pageHeight)

                            val page = PDPage(pageRect)
                            document.addPage(page)

                            val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.92f)
                            PDPageContentStream(document, page).use { cs ->
                                cs.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                onProgress(idx + 1, total)
            }

            document.save(tempPdfFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempPdfFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF output stream")

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "comic.pdf",
                "Comic CBZ to PDF"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("ComicBookEngine: Error converting CBZ to PDF", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { zipFile?.close() } catch (_: Exception) {}
            if (tempCbzFile.exists()) tempCbzFile.delete()
            if (tempPdfFile.exists()) tempPdfFile.delete()
        }
    }
}
