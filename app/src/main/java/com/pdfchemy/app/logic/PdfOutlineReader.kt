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

data class ReflowDocumentData(
    val sections: List<ReflowSection>,
    val bookmarks: List<OutlineBookmark>,
    val isScannedOnly: Boolean = false,
    val totalPages: Int = 0
)

object PdfOutlineReader {

    private fun isEpub(context: Context, uri: Uri): Boolean {
        val name = com.pdfchemy.app.utils.FileUtils.getFileName(context, uri) ?: uri.lastPathSegment ?: ""
        if (name.endsWith(".epub", ignoreCase = true)) return true
        val type = try { context.contentResolver.getType(uri) } catch (_: Exception) { null }
        return type?.contains("epub", ignoreCase = true) == true
    }

    /**
     * Unified single-pass extractor for both outline bookmarks and reflowable text paragraphs.
     * Prevents unzipping EPUB archives twice and parsing PDF page trees repeatedly.
     */
    suspend fun loadReflowDocument(context: Context, sourceUri: Uri): ReflowDocumentData = withContext(Dispatchers.IO) {
        if (isEpub(context, sourceUri)) {
            val chapters = extractEpubChapters(context, sourceUri)
            val bookmarks = chapters.mapIndexed { index, pair ->
                OutlineBookmark(title = pair.first, pageNumber = index + 1)
            }
            val sections = chapters.mapIndexed { index, pair ->
                ReflowSection(pageNumber = index + 1, title = pair.first, paragraphs = pair.second)
            }
            return@withContext ReflowDocumentData(sections, bookmarks, isScannedOnly = false, totalPages = sections.size)
        }

        val sections = mutableListOf<ReflowSection>()
        val bookmarks = mutableListOf<OutlineBookmark>()
        var isScannedOnly = false
        var totalPages = 0
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                doc = PDDocument.load(stream, com.tom_roush.pdfbox.io.MemoryUsageSetting.setupTempFileOnly())
                if (doc != null) {
                    totalPages = doc!!.numberOfPages
                    // 1. Extract outline
                    val catalog = doc?.documentCatalog
                    val outline = catalog?.documentOutline
                    if (outline != null) {
                        val pageIndexMap = HashMap<com.tom_roush.pdfbox.cos.COSBase, Int>(doc!!.numberOfPages)
                        for ((idx, p) in doc!!.pages.withIndex()) {
                            pageIndexMap[p.cosObject] = idx
                        }
                        bookmarks.addAll(parseOutlineNode(doc!!, outline, pageIndexMap))
                    }

                    // 2. Extract reflow paragraphs in the same pass
                    val allPagesText = PdfTextExtractor.extractAllPagesText(doc!!)
                    val hasAnyText = allPagesText.any { it.trim().isNotEmpty() }
                    isScannedOnly = totalPages > 0 && !hasAnyText

                    for ((idx, rawText) in allPagesText.withIndex()) {
                        val pageNum = idx + 1
                        val trimmed = rawText.trim()
                        if (trimmed.isNotEmpty()) {
                            val paragraphs = trimmed
                                .split(Regex("\n\n+"))
                                .map { it.replace(Regex("\n+"), " ").trim() }
                                .filter { it.isNotBlank() }

                            sections.add(
                                ReflowSection(
                                    pageNumber = pageNum,
                                    title = "Page $pageNum",
                                    paragraphs = paragraphs
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to extract reflow document: ${e.message}", e)
        } finally {
            doc?.close()
        }
        ReflowDocumentData(sections, bookmarks, isScannedOnly, totalPages)
    }

    private fun parseOutlineNode(
        doc: PDDocument,
        node: PDOutlineNode,
        pageIndexMap: Map<com.tom_roush.pdfbox.cos.COSBase, Int>,
        visitedNodes: MutableSet<com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem> = mutableSetOf(),
        depth: Int = 0
    ): List<OutlineBookmark> {
        if (depth > 50) return emptyList()
        val result = mutableListOf<OutlineBookmark>()
        var currentItem = node.firstChild
        while (currentItem != null && visitedNodes.add(currentItem)) {
            val title = currentItem.title ?: "Untitled Section"
            var pageIndex = 0
            try {
                val page = currentItem.findDestinationPage(doc)
                if (page != null) {
                    pageIndex = pageIndexMap[page.cosObject] ?: 0
                }
            } catch (_: Exception) {
                // Default to first page if destination is not direct
            }

            val children = if (currentItem.hasChildren()) {
                parseOutlineNode(doc, currentItem, pageIndexMap, visitedNodes, depth + 1)
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

    suspend fun extractOutline(context: Context, sourceUri: Uri): List<OutlineBookmark> =
        loadReflowDocument(context, sourceUri).bookmarks

    suspend fun extractReflowContent(context: Context, sourceUri: Uri): List<ReflowSection> =
        loadReflowDocument(context, sourceUri).sections

    private fun extractEpubChapters(context: Context, sourceUri: Uri): List<Pair<String, List<String>>> {
        val chapters = mutableListOf<Pair<String, List<String>>>()
        var tempFile: java.io.File? = null
        var zip: java.util.zip.ZipFile? = null
        try {
            tempFile = java.io.File(context.cacheDir, "epub_read_${System.currentTimeMillis()}.epub")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            } ?: return emptyList()

            zip = java.util.zip.ZipFile(tempFile)
            var opfPath = "OEBPS/content.opf"
            val containerEntry = zip.getEntry("META-INF/container.xml")
            if (containerEntry != null) {
                val containerText = zip.getInputStream(containerEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val match = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(containerText)
                if (match != null) opfPath = match.groupValues[1]
            }

            val opfEntry = zip.getEntry(opfPath)
            val opfBaseUri = java.net.URI.create("file:///" + opfPath.replace(" ", "%20"))
            val spineItems = mutableListOf<String>()

            if (opfEntry != null) {
                val opfText = zip.getInputStream(opfEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val itemMap = mutableMapOf<String, String>()
                // Flexible regex for opf:item or item
                val itemRegex = Regex("""<(?:opf:)?item\s+([^>]+)>""", RegexOption.IGNORE_CASE)
                for (m in itemRegex.findAll(opfText)) {
                    val attrs = m.groupValues[1]
                    val idMatch = Regex("""id\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
                    val hrefMatch = Regex("""href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(attrs)
                    if (idMatch != null && hrefMatch != null) {
                        val id = idMatch.groupValues[1]
                        var href = hrefMatch.groupValues[1]
                        
                        // Clean href (remove anchors)
                        href = href.substringBefore('#')
                        // URL decode
                        href = java.net.URLDecoder.decode(href, "UTF-8")
                        
                        // Resolve relative path
                        val resolvedUri = opfBaseUri.resolve(href.replace(" ", "%20"))
                        var resolvedPath = resolvedUri.path
                        if (resolvedPath.startsWith("/")) resolvedPath = resolvedPath.substring(1)
                        
                        // URL decode the final path just in case the zip entry uses decoded names
                        resolvedPath = java.net.URLDecoder.decode(resolvedPath, "UTF-8")
                        
                        itemMap[id] = resolvedPath
                    }
                }

                val itemrefRegex = Regex("""<(?:opf:)?itemref\s+[^>]*idref\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
                for (m in itemrefRegex.findAll(opfText)) {
                    val resolved = itemMap[m.groupValues[1]]
                    if (resolved != null && (resolved.endsWith(".xhtml", true) || resolved.endsWith(".html", true) || resolved.endsWith(".htm", true))) {
                        if (!resolved.contains("nav.xhtml", true) && !resolved.contains("toc.xhtml", true)) {
                            spineItems.add(resolved)
                        }
                    }
                }
            }

            if (spineItems.isEmpty()) {
                val allHtml = zip.entries().toList().filter { 
                    it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) 
                }.filter { !it.name.contains("nav.xhtml", true) && !it.name.contains("toc.xhtml", true) }.sortedBy { it.name }
                for (entry in allHtml) spineItems.add(entry.name)
            }

            for ((idx, path) in spineItems.withIndex()) {
                val entry = zip.getEntry(path) ?: continue
                val html = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }
                
                // Enhanced title extraction
                val titleMatch = Regex("""<(?:h[1-3]|title)[^>]*>(.*?)</(?:h[1-3]|title)>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
                var chapTitle = titleMatch?.groupValues?.get(1)?.replace(Regex("""<[^>]+>"""), "")?.trim() ?: "Chapter ${idx + 1}"
                
                chapTitle = decodeHtmlEntities(chapTitle)

                var clean = html.replace(Regex("""<head.*?</head>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                clean = clean.replace(Regex("""<style.*?</style>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                clean = clean.replace(Regex("""<script.*?</script>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
                clean = clean.replace(Regex("""<(p|h[1-6]|div|li|tr)[^>]*>""", RegexOption.IGNORE_CASE), "\n")
                clean = clean.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
                clean = clean.replace(Regex("""<[^>]+>"""), "")
                
                clean = decodeHtmlEntities(clean)
                
                val paragraphs = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
                
                // Descriptive chapter title for generic "Page X" or "Chapter X"
                if ((chapTitle.startsWith("Chapter", ignoreCase = true) || chapTitle.startsWith("Page", ignoreCase = true) || chapTitle.length < 2) && paragraphs.isNotEmpty()) {
                    val firstLine = paragraphs.first()
                    if (firstLine.length in 3..100) {
                        chapTitle = firstLine
                    }
                }

                if (paragraphs.isNotEmpty()) {
                    chapters.add(chapTitle to paragraphs)
                }
            }
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to parse EPUB: ${e.message}", e)
        } finally {
            try { zip?.close() } catch (_: Exception) {}
            tempFile?.delete()
        }
        return chapters
    }

    private fun decodeHtmlEntities(text: String): String {
        var decoded = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&bull;", "•")
        
        // Hex entities
        decoded = Regex("""&#x([0-9a-fA-F]+);""").replace(decoded) {
            try { it.groupValues[1].toInt(16).toChar().toString() } catch (e: Exception) { it.value }
        }
        // Numeric entities
        decoded = Regex("""&#([0-9]+);""").replace(decoded) {
            try { it.groupValues[1].toInt().toChar().toString() } catch (e: Exception) { it.value }
        }
        return decoded
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
