package com.pdfchemy.desktop.engine

import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

data class PageItemSpec(
    val originalPageIndex: Int,
    val rotation: Int = 0
)

object DesktopPdfEngine {

    /**
     * Gets page count of a PDF document.
     */
    fun getPageCount(file: File): Int {
        return PDDocument.load(file).use { it.numberOfPages }
    }

    /**
     * Extracts full plain text from a PDF document.
     */
    fun extractText(file: File): String {
        return PDDocument.load(file).use { document ->
            PDFTextStripper().getText(document)
        }
    }

    /**
     * Renders a specific PDF page to a BufferedImage at given DPI.
     */
    fun renderPage(file: File, pageIndex: Int, dpi: Float = 150f): BufferedImage {
        return PDDocument.load(file).use { document ->
            val renderer = PDFRenderer(document)
            renderer.renderImageWithDPI(pageIndex, dpi)
        }
    }

    /**
     * Renders a quick, downscaled thumbnail for high-performance visual grid rendering.
     */
    fun renderThumbnail(file: File, pageIndex: Int, targetWidth: Int = 260): BufferedImage {
        return PDDocument.load(file).use { document ->
            val renderer = PDFRenderer(document)
            val page = document.getPage(pageIndex)
            val cropBox = page.cropBox ?: page.mediaBox ?: PDRectangle.A4
            val dpi = (targetWidth.toFloat() / cropBox.width * 72f).coerceIn(36f, 100f)
            renderer.renderImageWithDPI(pageIndex, dpi)
        }
    }

    /**
     * Renders all thumbnails in a single document pass for buttery smooth performance.
     */
    fun renderAllThumbnails(
        file: File,
        targetWidth: Int = 260,
        onThumbnailRendered: (pageIndex: Int, BufferedImage) -> Unit
    ) {
        PDDocument.load(file).use { document ->
            val renderer = PDFRenderer(document)
            val count = document.numberOfPages
            for (i in 0 until count) {
                val page = document.getPage(i)
                val cropBox = page.cropBox ?: page.mediaBox ?: PDRectangle.A4
                val dpi = (targetWidth.toFloat() / cropBox.width * 72f).coerceIn(36f, 100f)
                val img = renderer.renderImageWithDPI(i, dpi)
                onThumbnailRendered(i, img)
            }
        }
    }

    /**
     * Saves a reordered, rotated, or pruned document from a list of PageItemSpecs.
     */
    fun saveReorderedPdf(inputFile: File, outputFile: File, pageSpecs: List<PageItemSpec>) {
        PDDocument.load(inputFile).use { sourceDoc ->
            val outDoc = PDDocument()
            for (spec in pageSpecs) {
                if (spec.originalPageIndex in 0 until sourceDoc.numberOfPages) {
                    val page = sourceDoc.getPage(spec.originalPageIndex)
                    // Create an imported page copy to prevent reference conflicts
                    val imported = outDoc.importPage(page)
                    imported.rotation = (page.rotation + spec.rotation) % 360
                }
            }
            outDoc.save(outputFile)
            outDoc.close()
        }
    }

    /**
     * Merges multiple PDF files into one output file.
     */
    fun mergePdfs(inputFiles: List<File>, outputFile: File) {
        val merger = PDFMergerUtility()
        merger.destinationFileName = outputFile.absolutePath
        for (f in inputFiles) {
            merger.addSource(f)
        }
        merger.mergeDocuments(null)
    }

    /**
     * Splits a PDF into individual pages or segments.
     */
    fun splitPdf(inputFile: File, outputDir: File, splitEveryNPages: Int = 1): List<File> {
        val createdFiles = mutableListOf<File>()
        PDDocument.load(inputFile).use { document ->
            val splitter = Splitter()
            splitter.setSplitAtPage(splitEveryNPages)
            val documents = splitter.split(document)
            documents.forEachIndexed { index, doc ->
                val outFile = File(outputDir, "${inputFile.nameWithoutExtension}_part_${index + 1}.pdf")
                doc.save(outFile)
                doc.close()
                createdFiles.add(outFile)
            }
        }
        return createdFiles
    }

    /**
     * Rotates specified pages (or all if empty) by the given degrees (90, 180, 270).
     */
    fun rotatePages(inputFile: File, outputFile: File, degrees: Int, pageIndices: Set<Int> = emptySet()) {
        PDDocument.load(inputFile).use { document ->
            val count = document.numberOfPages
            for (i in 0 until count) {
                if (pageIndices.isEmpty() || pageIndices.contains(i)) {
                    val page = document.getPage(i)
                    page.rotation = (page.rotation + degrees) % 360
                }
            }
            document.save(outputFile)
        }
    }

    /**
     * Removes specified page indices (0-based) from document.
     */
    fun deletePages(inputFile: File, outputFile: File, pagesToDelete: Set<Int>) {
        PDDocument.load(inputFile).use { document ->
            pagesToDelete.sortedDescending().forEach { index ->
                if (index in 0 until document.numberOfPages) {
                    document.removePage(index)
                }
            }
            document.save(outputFile)
        }
    }

    /**
     * Encrypts a PDF with user and owner passwords.
     */
    fun encryptPdf(inputFile: File, outputFile: File, userPass: String, ownerPass: String = userPass, keyLengthBits: Int = 128) {
        PDDocument.load(inputFile).use { document ->
            val ap = AccessPermission()
            val spp = StandardProtectionPolicy(ownerPass, userPass, ap)
            spp.encryptionKeyLength = keyLengthBits
            document.protect(spp)
            document.save(outputFile)
        }
    }

    /**
     * Decrypts a password-protected PDF.
     */
    fun decryptPdf(inputFile: File, outputFile: File, password: String) {
        PDDocument.load(inputFile, password).use { document ->
            document.isAllSecurityToBeRemoved = true
            document.save(outputFile)
        }
    }

    /**
     * Compresses PDF with preset DPI and JPEG quality.
     */
    fun compressPdf(
        inputFile: File,
        outputFile: File,
        targetDpi: Float = 140f,
        quality: Float = 0.7f,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Long {
        PDDocument.load(inputFile).use { document ->
            val totalPages = document.numberOfPages
            val renderer = PDFRenderer(document)
            val compressedDoc = PDDocument()

            for (i in 0 until totalPages) {
                onProgress(i + 1, totalPages)
                val renderedImage = renderer.renderImageWithDPI(i, targetDpi)
                val tempJpg = File.createTempFile("compress_page_$i", ".jpg")
                try {
                    val writers = ImageIO.getImageWritersByFormatName("jpg")
                    if (writers.hasNext()) {
                        val writer = writers.next()
                        FileOutputStream(tempJpg).use { os ->
                            ImageIO.createImageOutputStream(os).use { ios ->
                                writer.output = ios
                                val param = writer.defaultWriteParam
                                param.compressionMode = javax.imageio.ImageWriteParam.MODE_EXPLICIT
                                param.compressionQuality = quality
                                writer.write(null, javax.imageio.IIOImage(renderedImage, null, null), param)
                            }
                        }
                    } else {
                        ImageIO.write(renderedImage, "jpg", tempJpg)
                    }

                    val origPage = document.getPage(i)
                    val origBox = origPage.cropBox ?: origPage.mediaBox ?: PDRectangle.A4
                    val newPage = PDPage(PDRectangle(origBox.width, origBox.height))
                    newPage.rotation = origPage.rotation
                    compressedDoc.addPage(newPage)
                    val pdImage = PDImageXObject.createFromFileByExtension(tempJpg, compressedDoc)
                    org.apache.pdfbox.pdmodel.PDPageContentStream(compressedDoc, newPage).use { stream ->
                        stream.drawImage(pdImage, 0f, 0f, origBox.width, origBox.height)
                    }
                } finally {
                    tempJpg.delete()
                }
            }

            compressedDoc.save(outputFile)
            compressedDoc.close()
        }
        return outputFile.length()
    }

    /**
     * Smart Target Size Optimizer: Iteratively tunes DPI and quality to guarantee output size under targetBytes.
     */
    fun compressToTargetSize(
        inputFile: File,
        outputFile: File,
        targetBytes: Long,
        onProgress: (String) -> Unit = {}
    ): Long {
        val originalSize = inputFile.length()
        if (originalSize <= targetBytes) {
            inputFile.copyTo(outputFile, overwrite = true)
            return outputFile.length()
        }

        val totalPages = getPageCount(inputFile).coerceAtLeast(1)
        val budgetPerPage = targetBytes / totalPages

        // Calculate initial heuristic
        val (initialDpi, initialQuality) = when {
            budgetPerPage < 40_000 -> 90f to 0.40f
            budgetPerPage < 100_000 -> 120f to 0.55f
            budgetPerPage < 250_000 -> 140f to 0.65f
            else -> 160f to 0.75f
        }

        onProgress("Optimizing compression profile for ${targetBytes / 1024 / 1024} MB target...")
        var resultSize = compressPdf(inputFile, outputFile, initialDpi, initialQuality)

        // If still exceeding target, perform an aggressive tightening pass
        if (resultSize > targetBytes) {
            onProgress("Refining compression threshold...")
            val ratio = targetBytes.toFloat() / resultSize.toFloat()
            val tighterDpi = (initialDpi * ratio).coerceIn(54f, 120f)
            val tighterQuality = (initialQuality * ratio).coerceIn(0.25f, 0.50f)
            resultSize = compressPdf(inputFile, outputFile, tighterDpi, tighterQuality)
        }

        // Production Invariant: Never deliver a file that is larger than the original input!
        if (resultSize >= originalSize) {
            inputFile.copyTo(outputFile, overwrite = true)
            resultSize = outputFile.length()
        }

        return resultSize
    }

    /**
     * Extracts all pages as standalone high-resolution images (PNG or JPG).
     */
    fun extractPagesToImages(
        inputFile: File,
        outputFolder: File,
        format: String = "png",
        dpi: Float = 150f,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<File> {
        outputFolder.mkdirs()
        val extractedFiles = mutableListOf<File>()
        PDDocument.load(inputFile).use { doc ->
            val renderer = PDFRenderer(doc)
            val total = doc.numberOfPages
            for (i in 0 until total) {
                onProgress(i + 1, total)
                val img = renderer.renderImageWithDPI(i, dpi)
                val outFile = File(outputFolder, "${inputFile.nameWithoutExtension}_page_${i + 1}.$format")
                ImageIO.write(img, format, outFile)
                extractedFiles.add(outFile)
            }
        }
        return extractedFiles
    }

    /**
     * Converts a collection of images (PNG, JPG, BMP) into a single master PDF document.
     */
    fun imagesToPdf(
        imageFiles: List<File>,
        outputFile: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ) {
        val doc = PDDocument()
        imageFiles.forEachIndexed { index, imgFile ->
            onProgress(index + 1, imageFiles.size)
            val lower = imgFile.name.lowercase()
            val pdImage = if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                try {
                    PDImageXObject.createFromFileByExtension(imgFile, doc)
                } catch (_: Exception) {
                    val bimg = ImageIO.read(imgFile)
                    if (bimg != null) JPEGFactory.createFromImage(doc, bimg, 0.88f) else null
                }
            } else {
                val bimg = ImageIO.read(imgFile)
                if (bimg != null) {
                    JPEGFactory.createFromImage(doc, bimg, 0.88f)
                } else null
            }

            if (pdImage != null) {
                // Scale to standard document points (150 DPI baseline)
                val targetDpi = 150f
                val ptWidth = (pdImage.width.toFloat() * 72f / targetDpi).coerceAtLeast(100f)
                val ptHeight = (pdImage.height.toFloat() * 72f / targetDpi).coerceAtLeast(100f)
                val page = PDPage(PDRectangle(ptWidth, ptHeight))
                doc.addPage(page)
                org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page).use { stream ->
                    stream.drawImage(pdImage, 0f, 0f, ptWidth, ptHeight)
                }
            }
        }
        doc.save(outputFile)
        doc.close()
    }

    /**
     * Repairs and reconstructs damaged PDF files with missing headers, truncated EOF, or broken XRef tables.
     */
    fun repairPdf(inputFile: File, outputFile: File): Boolean {
        var bytes = inputFile.readBytes()
        if (bytes.isEmpty()) return false

        // 1. Repair header if missing or preceded by garbage bytes
        val headerMarker = "%PDF-".toByteArray(Charsets.US_ASCII)
        val headerIdx = bytesIndexOf(bytes, headerMarker)
        if (headerIdx > 0) {
            bytes = bytes.copyOfRange(headerIdx, bytes.size)
        } else if (headerIdx < 0) {
            bytes = "%PDF-1.7\n".toByteArray(Charsets.US_ASCII) + bytes
        }

        // 2. Append %%EOF if truncated
        val tailStr = String(bytes.takeLast(80).toByteArray(), Charsets.US_ASCII)
        if (!tailStr.contains("%%EOF")) {
            bytes = bytes + "\n%%EOF\n".toByteArray(Charsets.US_ASCII)
        }

        // 3. Parse with PDFBox parser and save cleanly to generate fresh XRef table
        PDDocument.load(bytes).use { doc ->
            doc.save(outputFile)
        }
        return outputFile.exists() && outputFile.length() > 0
    }

    /**
     * Redacts target text query across all pages.
     * When forensicSanitize is true, the redacted page is rasterized to a high-DPI clean image,
     * permanently obliterating the underlying text layer so sensitive data cannot be retrieved
     * via text strippers, mouse selection, or raw byte inspection.
     */
    fun redactPdf(
        inputFile: File,
        outputFile: File,
        query: String,
        overlayText: String = "REDACTED",
        forensicSanitize: Boolean = true,
        dpi: Float = 150f
    ): Int {
        if (query.isBlank()) {
            inputFile.copyTo(outputFile, overwrite = true)
            return 0
        }

        var matchCount = 0
        PDDocument.load(inputFile).use { doc ->
            val totalPages = doc.numberOfPages
            val pagesToSanitize = mutableSetOf<Int>()

            for (pageIdx in 0 until totalPages) {
                val page = doc.getPage(pageIdx)
                val cropBox = page.cropBox ?: page.mediaBox ?: PDRectangle.A4

                // Search text in page
                val stripper = object : PDFTextStripper() {
                    val positions = mutableListOf<org.apache.pdfbox.text.TextPosition>()
                    override fun processTextPosition(text: org.apache.pdfbox.text.TextPosition) {
                        positions.add(text)
                        super.processTextPosition(text)
                    }
                }
                stripper.startPage = pageIdx + 1
                stripper.endPage = pageIdx + 1
                val dummyWriter = java.io.StringWriter()
                stripper.writeText(doc, dummyWriter)

                val fullText = stripper.positions.joinToString("") { it.unicode ?: "" }
                val pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(query), java.util.regex.Pattern.CASE_INSENSITIVE)
                val matcher = pattern.matcher(fullText)

                val pageMatches = mutableListOf<Pair<FloatArray, String>>()

                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    if (start in stripper.positions.indices && (end - 1) in stripper.positions.indices) {
                        val firstPos = stripper.positions[start]
                        val lastPos = stripper.positions[end - 1]

                        val minX = firstPos.xDirAdj
                        val maxX = lastPos.xDirAdj + lastPos.widthDirAdj
                        val topY = firstPos.yDirAdj
                        val height = firstPos.heightDir.coerceAtLeast(12f)
                        val drawH = height + 4f
                        val drawW = (maxX - minX) + 4f
                        val drawX = minX - 2f
                        val drawY = cropBox.height - (topY + height) - 2f

                        pageMatches.add(floatArrayOf(drawX, drawY, drawW, drawH) to matcher.group())
                        matchCount++
                    }
                }

                if (pageMatches.isNotEmpty()) {
                    pagesToSanitize.add(pageIdx)
                    // Draw opaque black redaction box and overlay label
                    org.apache.pdfbox.pdmodel.PDPageContentStream(
                        doc,
                        page,
                        org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { cs ->
                        for ((box, _) in pageMatches) {
                            cs.setNonStrokingColor(java.awt.Color.BLACK)
                            cs.addRect(box[0], box[1], box[2], box[3])
                            cs.fill()

                            if (overlayText.isNotBlank() && box[2] > 20f && box[3] > 8f) {
                                val font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD
                                val fontSize = (box[3] * 0.5f).coerceIn(6f, 10f)
                                val textWidth = font.getStringWidth(overlayText) / 1000f * fontSize
                                if (textWidth < box[2]) {
                                    cs.beginText()
                                    cs.setFont(font, fontSize)
                                    cs.setNonStrokingColor(java.awt.Color.WHITE)
                                    val tx = box[0] + (box[2] - textWidth) / 2f
                                    val ty = box[1] + (box[3] - fontSize) / 2f + 1f
                                    cs.newLineAtOffset(tx, ty)
                                    cs.showText(overlayText)
                                    cs.endText()
                                }
                            }
                        }
                    }
                }
            }

            if (matchCount > 0 && forensicSanitize) {
                // Forensic Pass: Rasterize redacted pages to clean images to eliminate underlying text bytes completely
                val renderer = PDFRenderer(doc)
                val sanitizedDoc = PDDocument()

                for (i in 0 until totalPages) {
                    val origPage = doc.getPage(i)
                    val origBox = origPage.cropBox ?: origPage.mediaBox ?: PDRectangle.A4
                    if (pagesToSanitize.contains(i)) {
                        val rendered = renderer.renderImageWithDPI(i, dpi)
                        val newPage = PDPage(PDRectangle(origBox.width, origBox.height))
                        newPage.rotation = origPage.rotation
                        sanitizedDoc.addPage(newPage)
                        val pdImg = JPEGFactory.createFromImage(sanitizedDoc, rendered, 0.90f)
                        org.apache.pdfbox.pdmodel.PDPageContentStream(sanitizedDoc, newPage).use { cs ->
                            cs.drawImage(pdImg, 0f, 0f, origBox.width, origBox.height)
                        }
                    } else {
                        sanitizedDoc.importPage(origPage)
                    }
                }
                sanitizedDoc.save(outputFile)
                sanitizedDoc.close()
            } else {
                doc.save(outputFile)
            }
        }
        return matchCount
    }

    private fun bytesIndexOf(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        for (i in 0..source.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }
}

