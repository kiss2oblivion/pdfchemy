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

    private fun isEpub(context: Context, uri: Uri): Boolean {
        val name = com.pdfchemy.app.utils.FileUtils.getFileName(context, uri) ?: uri.lastPathSegment ?: ""
        if (name.endsWith(".epub", ignoreCase = true)) return true
        val type = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
        return type?.contains("epub", ignoreCase = true) == true
    }

    suspend fun extractOutline(context: Context, sourceUri: Uri): List<OutlineBookmark> = withContext(Dispatchers.IO) {
        if (isEpub(context, sourceUri)) {
            return@withContext extractEpubOutline(context, sourceUri)
        }
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
        if (isEpub(context, sourceUri)) {
            return@withContext extractEpubReflowContent(context, sourceUri)
        }
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

    private fun extractEpubChapters(context: Context, sourceUri: Uri): List<Pair<String, List<String>>> {
        val chapters = mutableListOf<Pair<String, List<String>>>()
        var tempFile: java.io.File? = null
        try {
            tempFile = java.io.File(context.cacheDir, "epub_read_${System.currentTimeMillis()}.epub")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: return emptyList()

            val zip = java.util.zip.ZipFile(tempFile)
            var opfPath = "OEBPS/content.opf"
            val containerEntry = zip.getEntry("META-INF/container.xml")
            if (containerEntry != null) {
                val containerText = zip.getInputStream(containerEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val match = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(containerText)
                if (match != null) opfPath = match.groupValues[1]
            }

            val opfEntry = zip.getEntry(opfPath)
            val opfDir = if (opfPath.contains('/')) opfPath.substringBeforeLast('/') + "/" else ""
            val spineItems = mutableListOf<String>()

            if (opfEntry != null) {
                val opfText = zip.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val itemMap = mutableMapOf<String, String>()
                val itemRegex = Regex("""<item\s+[^>]*id\s*=\s*["']([^"']+)["'][^>]*href\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                for (m in itemRegex.findAll(opfText)) {
                    itemMap[m.groupValues[1]] = opfDir + m.groupValues[2]
                }
                val itemRegex2 = Regex("""<item\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*id\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                for (m in itemRegex2.findAll(opfText)) {
                    itemMap[m.groupValues[2]] = opfDir + m.groupValues[1]
                }

                val itemrefRegex = Regex("""<itemref\s+[^>]*idref\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                for (m in itemrefRegex.findAll(opfText)) {
                    val resolved = itemMap[m.groupValues[1]]
                    if (resolved != null && (resolved.endsWith(".xhtml", true) || resolved.endsWith(".html", true) || resolved.endsWith(".htm", true))) {
                        if (!resolved.contains("nav.xhtml", true)) {
                            spineItems.add(resolved)
                        }
                    }
                }
            }

            if (spineItems.isEmpty()) {
                val allHtml = zip.entries().toList().filter { 
                    it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) 
                }.filter { !it.name.contains("nav.xhtml", true) }.sortedBy { it.name }
                for (entry in allHtml) spineItems.add(entry.name)
            }

            for ((idx, path) in spineItems.withIndex()) {
                val entry = zip.getEntry(path) ?: continue
                val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                
                val titleMatch = Regex("""<(?:h[1-3]|title)[^>]*>([^<]+)</(?:h[1-3]|title)>""", RegexOption.IGNORE_CASE).find(html)
                val chapTitle = titleMatch?.groupValues?.get(1)?.trim() ?: "Chapter ${idx + 1}"

                var clean = html.replace(Regex("""<head.*?</head>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                clean = clean.replace(Regex("""<style.*?</style>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                clean = clean.replace(Regex("""<script.*?</script>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                clean = clean.replace(Regex("""<(p|h[1-6]|div|li)[^>]*>""", RegexOption.IGNORE_CASE), "\n")
                clean = clean.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                clean = clean.replace(Regex("""<[^>]+>"""), "")
                clean = clean.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
                val paragraphs = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (paragraphs.isNotEmpty()) {
                    chapters.add(chapTitle to paragraphs)
                }
            }
            zip.close()
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to parse EPUB: ${e.message}", e)
        } finally {
            tempFile?.delete()
        }
        return chapters
    }

    private fun extractEpubOutline(context: Context, sourceUri: Uri): List<OutlineBookmark> {
        val chapters = extractEpubChapters(context, sourceUri)
        return chapters.mapIndexed { index, pair ->
            OutlineBookmark(
                title = pair.first,
                pageNumber = index + 1
            )
        }
    }

    private fun extractEpubReflowContent(context: Context, sourceUri: Uri): List<ReflowSection> {
        val chapters = extractEpubChapters(context, sourceUri)
        return chapters.mapIndexed { index, pair ->
            ReflowSection(
                pageNumber = index + 1,
                title = pair.first,
                paragraphs = pair.second
            )
        }
    }
}
