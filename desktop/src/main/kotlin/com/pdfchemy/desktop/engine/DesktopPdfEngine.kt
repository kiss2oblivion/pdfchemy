package com.pdfchemy.desktop.engine

import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import javax.imageio.ImageIO

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
            // Delete from highest index down to avoid shifting
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
     * Compresses PDF by re-encoding pages at target DPI and JPEG quality.
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
                    // Write compressed JPEG
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

                    // Create new page in compressed document
                    val newPage = PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle(renderedImage.width.toFloat(), renderedImage.height.toFloat()))
                    compressedDoc.addPage(newPage)
                    val pdImage = org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromFileByExtension(tempJpg, compressedDoc)
                    org.apache.pdfbox.pdmodel.PDPageContentStream(compressedDoc, newPage).use { stream ->
                        stream.drawImage(pdImage, 0f, 0f, renderedImage.width.toFloat(), renderedImage.height.toFloat())
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
}
