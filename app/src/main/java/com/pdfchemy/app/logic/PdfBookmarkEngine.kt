package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

data class BookmarkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val pageIndex: Int // 0-based
)

object PdfBookmarkEngine {

    /**
     * Reads all bookmarks / table of contents entries from a PDF.
     */
    suspend fun readBookmarks(
        context: Context,
        pdfUri: Uri
    ): Result<List<BookmarkItem>> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val outline = document.documentCatalog.documentOutline
            val result = mutableListOf<BookmarkItem>()

            if (outline != null) {
                var current = outline.firstChild
                while (current != null) {
                    val title = current.title ?: "Untitled Bookmark"
                    var pageIndex = 0
                    try {
                        val dest = current.destination
                        val destPage = if (dest is com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination) {
                            dest.page
                        } else null

                        if (destPage != null) {
                            pageIndex = document.pages.indexOf(destPage).coerceAtLeast(0)
                        }
                    } catch (_: Exception) {}

                    result.add(BookmarkItem(title = title, pageIndex = pageIndex))
                    current = current.nextSibling
                }
            }

            Result.success(result)
        } catch (e: Exception) {
            AppLogger.e("PdfBookmarkEngine: Error reading bookmarks", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Rebuilds and writes bookmarks into the destination PDF document.
     */
    suspend fun writeBookmarks(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        bookmarks: List<BookmarkItem>
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val totalPages = document.numberOfPages
            if (totalPages == 0) {
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            if (bookmarks.isEmpty()) {
                document.documentCatalog.documentOutline = null
            } else {
                val outline = PDDocumentOutline()
                document.documentCatalog.documentOutline = outline

                for (item in bookmarks) {
                    val safePageIdx = item.pageIndex.coerceIn(0, totalPages - 1)
                    val page = document.getPage(safePageIdx)

                    val outlineItem = PDOutlineItem().apply {
                        title = item.title
                        val dest = PDPageFitWidthDestination().apply {
                            this.page = page
                            top = 0
                        }
                        destination = dest
                    }
                    outline.addLast(outlineItem)
                }
                outline.openNode()
            }

            val tempFile = File(context.cacheDir, "bmark_${System.currentTimeMillis()}.pdf")
            document.save(tempFile)
            document.close()
            document = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "bookmarked.pdf",
                "Updated PDF Bookmarks"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfBookmarkEngine: Error writing bookmarks", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
