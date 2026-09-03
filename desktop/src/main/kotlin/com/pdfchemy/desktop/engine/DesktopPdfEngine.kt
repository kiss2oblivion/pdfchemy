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
            val tighterDpi = (initialDpi * ratio).coerceIn(72f, 130f)
            val tighterQuality = (initialQuality * ratio).coerceIn(0.30f, 0.60f)
            resultSize = compressPdf(inputFile, outputFile, tighterDpi, tighterQuality)
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
}
