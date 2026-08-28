package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfToEpubEngine {

    /**
     * Converts a PDF into a standard, fully reflowable EPUB 3.0 e-book archive.
     */
    suspend fun pdfToEpub(
        context: Context,
        sourcePdfUri: Uri,
        destEpubUri: Uri,
        bookTitle: String = "Untitled E-Book",
        authorName: String = "Unknown Author"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            if (pageCount == 0) {
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            val chapters = mutableListOf<String>()
            val stripper = PDFTextStripper()

            for (pageIdx in 0 until pageCount) {
                stripper.startPage = pageIdx + 1
                stripper.endPage = pageIdx + 1
                val text = stripper.getText(document).trim()
                chapters.add(if (text.isNotBlank()) text else "Page ${pageIdx + 1}")
            }

            document.close()
            document = null

            // Build EPUB package in temp file
            val tempEpubFile = File(context.cacheDir, "ebook_${System.currentTimeMillis()}.epub")
            val bookId = "urn:uuid:" + UUID.randomUUID().toString()

            ZipOutputStream(FileOutputStream(tempEpubFile)).use { zip ->
                // 1. mimetype (must be uncompressed/stored)
                val mimeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)
                val mimeEntry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = mimeBytes.size.toLong()
                    compressedSize = mimeBytes.size.toLong()
                    val crc = CRC32()
                    crc.update(mimeBytes)
                    setCrc(crc.value)
                }
                zip.putNextEntry(mimeEntry)
                zip.write(mimeBytes)
                zip.closeEntry()

                // 2. META-INF/container.xml
                val containerXml = """<?xml version="1.0" encoding="UTF-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""
                zip.putNextEntry(ZipEntry("META-INF/container.xml"))
                zip.write(containerXml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 3. OEBPS/stylesheet.css
                val css = """body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Georgia, serif;
  line-height: 1.6;
  margin: 5% 8%;
  color: #1a1a1a;
  background-color: #ffffff;
}
h1, h2, h3 { color: #111; margin-top: 1.5em; }
p { margin-bottom: 1.2em; text-align: justify; }
@media (prefers-color-scheme: dark) {
  body { color: #e0e0e0; background-color: #121212; }
  h1, h2, h3 { color: #ffffff; }
}"""
                zip.putNextEntry(ZipEntry("OEBPS/stylesheet.css"))
                zip.write(css.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 4. OEBPS/chapter_*.xhtml files
                val chapterFiles = mutableListOf<String>()
                for ((idx, chapterText) in chapters.withIndex()) {
                    val chapNum = idx + 1
                    val chapFileName = "chapter_$chapNum.xhtml"
                    chapterFiles.add(chapFileName)

                    val paragraphsHtml = chapterText.lines()
                        .filter { it.isNotBlank() }
                        .joinToString("\n") { "<p>${escapeHtml(it.trim())}</p>" }

                    val xhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops" xml:lang="en">
<head>
  <title>Chapter $chapNum</title>
  <link rel="stylesheet" type="text/css" href="stylesheet.css"/>
</head>
<body>
  <h2>Page $chapNum</h2>
  $paragraphsHtml
</body>
</html>"""
                    zip.putNextEntry(ZipEntry("OEBPS/$chapFileName"))
                    zip.write(xhtml.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }

                // 5. OEBPS/nav.xhtml (EPUB 3 Navigation)
                val navLi = chapterFiles.mapIndexed { idx, fn ->
                    """<li><a href="$fn">Page ${idx + 1}</a></li>"""
                }.joinToString("\n")

                val navXhtml = """<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops">
<head><title>Table of Contents</title><link rel="stylesheet" type="text/css" href="stylesheet.css"/></head>
<body>
  <nav epub:type="toc" id="toc">
    <h1>Table of Contents</h1>
    <ol>
      $navLi
    </ol>
  </nav>
</body>
</html>"""
                zip.putNextEntry(ZipEntry("OEBPS/nav.xhtml"))
                zip.write(navXhtml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                // 6. OEBPS/content.opf
                val manifestItems = chapterFiles.mapIndexed { idx, fn ->
                    """<item id="chap_${idx + 1}" href="$fn" media-type="application/xhtml+xml"/>"""
                }.joinToString("\n    ")

                val spineItems = chapterFiles.mapIndexed { idx, _ ->
                    """<itemref idref="chap_${idx + 1}"/>"""
                }.joinToString("\n    ")

                val opf = """<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="BookId">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
    <dc:identifier id="BookId">$bookId</dc:identifier>
    <dc:title>${escapeHtml(bookTitle)}</dc:title>
    <dc:creator>${escapeHtml(authorName)}</dc:creator>
    <dc:language>en</dc:language>
    <meta property="dcterms:modified">2026-08-25T12:00:00Z</meta>
  </metadata>
  <manifest>
    <item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/>
    <item id="css" href="stylesheet.css" media-type="text/css"/>
    $manifestItems
  </manifest>
  <spine>
    $spineItems
  </spine>
</package>"""
                zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
                zip.write(opf.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            // Stream to destUri
            context.contentResolver.openOutputStream(destEpubUri)?.use { outStream ->
                tempEpubFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream)
                }
            } ?: throw IllegalStateException("Cannot open destination EPUB stream")

            tempEpubFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destEpubUri,
                FileUtils.getFileName(context, destEpubUri) ?: "book.epub",
                "PDF to EPUB 3.0"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfToEpubEngine: Error converting PDF to EPUB", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
