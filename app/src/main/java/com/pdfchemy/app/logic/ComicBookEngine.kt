package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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

        try {
            val pfd = context.contentResolver.openFileDescriptor(sourcePdfUri, "r")
                ?: throw IllegalStateException("Cannot open source PDF descriptor")

            val renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            if (totalPages == 0) {
                renderer.close()
                pfd.close()
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            ZipOutputStream(FileOutputStream(tempCbzFile)).use { zipOut ->
                for (pageIndex in 0 until totalPages) {
                    val page = renderer.openPage(pageIndex)
                    val scale = 2 // Crisp resolution
                    val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val entryName = "page_%04d.jpg".format(pageIndex + 1)
                    zipOut.putNextEntry(ZipEntry(entryName))
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, zipOut)
                    zipOut.closeEntry()
                    bitmap.recycle()

                    onProgress(pageIndex + 1, totalPages)
                }
            }

            renderer.close()
            pfd.close()

            context.contentResolver.openOutputStream(destCbzUri)?.use { outStream ->
                tempCbzFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Cannot open destination CBZ output stream")

            tempCbzFile.delete()

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
        var document: PDDocument? = null
        var inputStream: InputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourceCbzUri)
                ?: throw IllegalStateException("Cannot open source CBZ stream")

            document = PDDocument()
            val imageEntries = mutableListOf<Pair<String, ByteArray>>()

            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))) {
                        val buffer = ByteArrayOutputStream()
                        zipIn.copyTo(buffer)
                        imageEntries.add(entry.name to buffer.toByteArray())
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // Natural sort pages by filename
            imageEntries.sortBy { it.first }

            if (imageEntries.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No valid comic images found in CBZ archive"))
            }

            val total = imageEntries.size
            for ((idx, item) in imageEntries.withIndex()) {
                val bytes = item.second
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: continue

                val pageWidth = bitmap.width.toFloat()
                val pageHeight = bitmap.height.toFloat()
                val pageRect = PDRectangle(pageWidth, pageHeight)

                val page = PDPage(pageRect)
                document.addPage(page)

                val pdImage = JPEGFactory.createFromImage(document, bitmap, 0.92f)
                PDPageContentStream(document, page).use { cs ->
                    cs.drawImage(pdImage, 0f, 0f, pageWidth, pageHeight)
                }

                bitmap.recycle()
                onProgress(idx + 1, total)
            }

            val tempFile = File(context.cacheDir, "comic_to_pdf_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF output stream")

            tempFile.delete()

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
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
