package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class OutlineBookmark(
    val title: String,
    val pageNumber: Int,
    val children: List<OutlineBookmark> = emptyList()
)

data class ReflowSection(
    val pageNumber: Int,
    val title: String?,
    val paragraphs: List<String>
)

object PdfOutlineReader {

    suspend fun extractOutline(context: Context, sourceUri: Uri): List<OutlineBookmark> = withContext(Dispatchers.IO) {
        val list = mutableListOf<OutlineBookmark>()
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                doc = PDDocument.load(stream)
                val catalog = doc?.documentCatalog
                val outline = catalog?.documentOutline
                if (outline != null) {
                    list.addAll(parseOutlineNode(doc!!, outline))
                }
            }
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to extract PDF outline: ${e.message}", e)
        } finally {
            doc?.close()
        }
        list
    }

    private fun parseOutlineNode(doc: PDDocument, node: PDOutlineNode): List<OutlineBookmark> {
        val result = mutableListOf<OutlineBookmark>()
        var currentItem = node.firstChild
        while (currentItem != null) {
            val title = currentItem.title ?: "Untitled Section"
            var pageIndex = 0
            try {
                val page = currentItem.findDestinationPage(doc)
                if (page != null) {
                    pageIndex = doc.pages.indexOf(page)
                }
            } catch (e: Exception) {
                // Default to first page if destination is not direct
            }

            val children = if (currentItem.hasChildren()) {
                parseOutlineNode(doc, currentItem)
            } else {
                emptyList()
            }

            result.add(
                OutlineBookmark(
                    title = title,
                    pageNumber = (pageIndex + 1).coerceAtLeast(1),
                    children = children
                )
            )
            currentItem = currentItem.nextSibling
        }
        return result
    }

    suspend fun extractReflowContent(context: Context, sourceUri: Uri): List<ReflowSection> = withContext(Dispatchers.IO) {
        val sections = mutableListOf<ReflowSection>()
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                doc = PDDocument.load(stream)
                if (doc != null) {
                    val totalPages = doc!!.numberOfPages
                    val stripper = PDFTextStripper()
                    
                    for (i in 1..totalPages) {
                        stripper.startPage = i
                        stripper.endPage = i
                        val rawText = stripper.getText(doc).trim()
                        if (rawText.isNotEmpty()) {
                            // Split by double newlines or indentations to construct paragraphs
                            val paragraphs = rawText
                                .split(Regex("\n\n+"))
                                .map { it.replace(Regex("\n+"), " ").trim() }
                                .filter { it.isNotBlank() }

                            sections.add(
                                ReflowSection(
                                    pageNumber = i,
                                    title = "Page $i",
                                    paragraphs = paragraphs
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to extract reflow content: ${e.message}", e)
        } finally {
            doc?.close()
        }
        sections
    }
}
