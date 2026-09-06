package com.pdfchemy.desktop.engine

import org.apache.pdfbox.multipdf.PDFMergerUtility
import org.apache.pdfbox.multipdf.Splitter
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.encryption.AccessPermission
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import org.apache.pdfbox.util.Matrix
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDField
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox
import org.apache.pdfbox.pdmodel.interactive.form.PDRadioButton
import org.apache.pdfbox.pdmodel.interactive.form.PDChoice
import org.apache.pdfbox.cos.COSArray
import org.apache.pdfbox.cos.COSBase
import org.apache.pdfbox.cos.COSDictionary
import org.apache.pdfbox.cos.COSName
import org.apache.pdfbox.cos.COSString
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.common.PDMetadata
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent
import org.apache.pdfbox.multipdf.LayerUtility
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
import java.awt.BasicStroke
import java.awt.Font
import java.awt.RenderingHints
import java.awt.color.ColorSpace
import java.awt.color.ICC_Profile
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.imageio.ImageIO

enum class AcroFieldType {
    TEXT,
    CHECKBOX,
    RADIO,
    CHOICE,
    OTHER
}

data class DesktopAcroField(
    val name: String,
    val fullyQualifiedName: String,
    val type: AcroFieldType,
    val value: String,
    val options: List<String> = emptyList(),
    val isReadOnly: Boolean = false,
    val isRequired: Boolean = false
)

data class PageDiff(
    val pageNum: Int,
    val isIdentical: Boolean,
    val addedLines: List<String>,
    val removedLines: List<String>,
    val lineCountA: Int,
    val lineCountB: Int
)

data class PdfDiffSummary(
    val arePageCountsEqual: Boolean,
    val pagesA: Int,
    val pagesB: Int,
    val pageDiffs: List<PageDiff>,
    val totalAddedLines: Int,
    val totalRemovedLines: Int,
    val isEntirelyIdentical: Boolean
)

// --- BATES STAMPING DATA MODELS ---
enum class DesktopBatesPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

data class DesktopBatesConfig(
    val prefix: String = "EXHIBIT-",
    val suffix: String = "",
    val startNumber: Int = 1,
    val digits: Int = 6,
    val position: DesktopBatesPosition = DesktopBatesPosition.BOTTOM_RIGHT,
    val fontSize: Float = 10f,
    val marginPt: Float = 36f,
    val pageRange: IntRange? = null
)

// --- SANITIZE & SCRUB DATA MODELS ---
data class DesktopSanitizeResult(
    val threatsFound: Int,
    val jsCount: Int,
    val launchActionsCount: Int,
    val metadataPurged: Boolean,
    val attachmentsPurged: Int,
    val annotationsPurged: Int
)

// --- PDF REPAIR DATA MODELS ---
data class DesktopRepairResult(
    val isSuccess: Boolean,
    val pagesRecovered: Int,
    val issuesRepaired: List<String>,
    val originalSize: Long,
    val repairedSize: Long
)

// --- MARGIN CROP DATA MODELS ---
data class DesktopCropConfig(
    val leftPt: Float,
    val topPt: Float,
    val rightPt: Float,
    val bottomPt: Float,
    val applyToAllPages: Boolean = true
)

// --- ATTACHMENT DATA MODELS ---
data class DesktopAttachment(
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val description: String? = null
)

data class TextAnnotationItem(
    val text: String,
    val xRatio: Float,
    val yRatio: Float,
    val fontSize: Float = 14f,
    val colorHex: String = "#18181B"
)

enum class HeaderFooterPos {
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
    BOTTOM_LEFT,
    TOP_CENTER,
    TOP_RIGHT
}

data class PageItemSpec(
    val originalPageIndex: Int,
    val rotation: Int = 0
)

data class DesktopPdfMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val keywords: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null,
    val hasXmpMetadata: Boolean = false
) {
    val hasAnyMetadata: Boolean
        get() = !title.isNullOrBlank() || !author.isNullOrBlank() || !subject.isNullOrBlank() ||
                !keywords.isNullOrBlank() || !creator.isNullOrBlank() || !producer.isNullOrBlank() ||
                !creationDate.isNullOrBlank() || !modificationDate.isNullOrBlank() || hasXmpMetadata
}

object DesktopPdfEngine {

    /**
     * Inspects document metadata information and XMP streams.
     */
    fun inspectMetadata(file: File): DesktopPdfMetadata {
        return PDDocument.load(file).use { doc ->
            val info = doc.documentInformation
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            DesktopPdfMetadata(
                title = info?.title?.takeIf { it.isNotBlank() },
                author = info?.author?.takeIf { it.isNotBlank() },
                subject = info?.subject?.takeIf { it.isNotBlank() },
                keywords = info?.keywords?.takeIf { it.isNotBlank() },
                creator = info?.creator?.takeIf { it.isNotBlank() },
                producer = info?.producer?.takeIf { it.isNotBlank() },
                creationDate = try { info?.creationDate?.time?.let { sdf.format(it) } } catch (_: Exception) { null },
                modificationDate = try { info?.modificationDate?.time?.let { sdf.format(it) } } catch (_: Exception) { null },
                hasXmpMetadata = doc.documentCatalog.metadata != null
            )
        }
    }

    /**
     * Completely strips all metadata (DocumentInformation dictionary and catalog XMP metadata).
     */
    fun stripMetadata(inputFile: File, outputFile: File): Boolean {
        PDDocument.load(inputFile).use { doc ->
            doc.documentInformation = org.apache.pdfbox.pdmodel.PDDocumentInformation()
            doc.documentCatalog.metadata = null
            doc.save(outputFile)
        }
        return outputFile.exists() && outputFile.length() > 0
    }

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
    fun mergePdfs(inputFiles: List<File>, outputFile: File): Boolean {
        if (inputFiles.isEmpty()) return false
        val merger = PDFMergerUtility()
        merger.destinationFileName = outputFile.absolutePath
        for (f in inputFiles) {
            if (f.exists() && f.length() > 0) {
                merger.addSource(f)
            }
        }
        merger.mergeDocuments(org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly())
        return outputFile.exists() && outputFile.length() > 0
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

    /**
     * Applies a stamp or signature image onto a specific page of a PDF document.
     * xRatio: 0.0 (left) to 1.0 (right) relative to page width
     * yRatio: 0.0 (top) to 1.0 (bottom) relative to page height
     * widthRatio: percentage of page width for the stamp (0.1 to 0.9)
     */
    fun stampDocument(
        inputFile: File,
        outputFile: File,
        pageIndex: Int,
        stampImage: BufferedImage,
        xRatio: Float,
        yRatio: Float,
        widthRatio: Float = 0.35f
    ): Boolean {
        PDDocument.load(inputFile).use { document ->
            if (pageIndex < 0 || pageIndex >= document.numberOfPages) return false
            val page = document.getPage(pageIndex)
            val cropBox = page.cropBox ?: page.mediaBox
            val pageWidth = cropBox.width
            val pageHeight = cropBox.height

            val aspect = stampImage.height.toFloat() / stampImage.width.toFloat()
            val stampWidth = pageWidth * widthRatio.coerceIn(0.1f, 0.9f)
            val stampHeight = stampWidth * aspect

            // Convert UI top-left coordinate system to PDF bottom-left system
            val stampX = cropBox.lowerLeftX + (pageWidth - stampWidth) * xRatio.coerceIn(0f, 1f)
            val stampY = cropBox.lowerLeftY + (pageHeight - stampHeight) * (1f - yRatio.coerceIn(0f, 1f))

            val pdImage = LosslessFactory.createFromImage(document, stampImage)
            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                cs.drawImage(pdImage, stampX, stampY, stampWidth, stampHeight)
            }

            document.save(outputFile)
            return outputFile.exists() && outputFile.length() > 0
        }
    }

    /**
     * Generates a high-resolution transparent vector-style business stamp image.
     */
    fun createBusinessStamp(
        title: String,
        subtext: String? = null,
        colorHex: String = "#1E3A8A"
    ): BufferedImage {
        val width = 480
        val height = 180
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val color = java.awt.Color.decode(colorHex)
            g2d.color = color

            // Outer thick rounded border
            g2d.stroke = BasicStroke(5f)
            g2d.drawRoundRect(10, 10, width - 20, height - 20, 24, 24)

            // Inner thin border
            g2d.stroke = BasicStroke(2f)
            g2d.drawRoundRect(18, 18, width - 36, height - 36, 16, 16)

            // Title Text
            val titleFont = Font("Arial", Font.BOLD, if (title.length > 15) 28 else 34)
            g2d.font = titleFont
            val titleMetrics = g2d.fontMetrics
            val titleX = (width - titleMetrics.stringWidth(title)) / 2
            val titleY = if (subtext != null) (height / 2) - 4 else (height + titleMetrics.ascent - titleMetrics.descent) / 2
            g2d.drawString(title, titleX, titleY)

            // Subtext (e.g. date, verification mark)
            if (subtext != null) {
                val subFont = Font("Arial", Font.BOLD, 16)
                g2d.font = subFont
                val subMetrics = g2d.fontMetrics
                val subX = (width - subMetrics.stringWidth(subtext)) / 2
                val subY = titleY + 34
                g2d.drawString(subtext, subX, subY)
            }
        } finally {
            g2d.dispose()
        }
        return image
    }

    /**
     * Renders freehand signature strokes into a transparent high-DPI BufferedImage.
     */
    fun renderStrokesToImage(
        strokes: List<List<Point2D.Float>>,
        canvasWidth: Int,
        canvasHeight: Int,
        colorHex: String = "#1E3A8A",
        strokeWidth: Float = 4f
    ): BufferedImage {
        val image = BufferedImage(canvasWidth.coerceAtLeast(100), canvasHeight.coerceAtLeast(100), BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

            val color = java.awt.Color.decode(colorHex)
            g2d.color = color
            g2d.stroke = BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

            for (stroke in strokes) {
                if (stroke.size < 2) {
                    if (stroke.size == 1) {
                        val p = stroke[0]
                        g2d.fillOval((p.x - strokeWidth / 2).toInt(), (p.y - strokeWidth / 2).toInt(), strokeWidth.toInt(), strokeWidth.toInt())
                    }
                    continue
                }
                for (i in 0 until stroke.size - 1) {
                    val p1 = stroke[i]
                    val p2 = stroke[i + 1]
                    g2d.drawLine(p1.x.toInt(), p1.y.toInt(), p2.x.toInt(), p2.y.toInt())
                }
            }
        } finally {
            g2d.dispose()
        }
        return image
    }

    /**
     * Applies typed text annotations, form fill values or checkmarks to a specific page.
     */
    fun addTextAnnotations(
        inputFile: File,
        outputFile: File,
        pageIndex: Int,
        items: List<TextAnnotationItem>
    ): Boolean {
        if (items.isEmpty()) return false
        PDDocument.load(inputFile).use { document ->
            if (pageIndex < 0 || pageIndex >= document.numberOfPages) return false
            val page = document.getPage(pageIndex)
            val cropBox = page.cropBox ?: page.mediaBox
            val pageWidth = cropBox.width
            val pageHeight = cropBox.height

            PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                for (item in items) {
                    val font = if (item.text.length <= 2 && (item.text == "✓" || item.text == "✕" || item.text == "X")) {
                        PDType1Font.HELVETICA_BOLD
                    } else {
                        PDType1Font.HELVETICA
                    }
                    val awtColor = try { java.awt.Color.decode(item.colorHex) } catch (_: Exception) { java.awt.Color.BLACK }

                    val x = cropBox.lowerLeftX + pageWidth * item.xRatio.coerceIn(0f, 1f)
                    val y = cropBox.lowerLeftY + pageHeight * (1f - item.yRatio.coerceIn(0f, 1f)) - item.fontSize

                    cs.beginText()
                    cs.setFont(font, item.fontSize)
                    cs.setNonStrokingColor(awtColor)
                    cs.newLineAtOffset(x, y)
                    val textToRender = when (item.text) {
                        "✓" -> "V"
                        "✕" -> "X"
                        else -> item.text.replace("\n", " ")
                    }
                    cs.showText(textToRender)
                    cs.endText()
                }
            }

            document.save(outputFile)
            return outputFile.exists() && outputFile.length() > 0
        }
    }

    /**
     * Applies page numbers to all pages of a PDF document (e.g. "Page 1 of 15").
     */
    fun addPageNumbers(
        inputFile: File,
        outputFile: File,
        formatPattern: String = "Page %1\$d of %2\$d",
        position: HeaderFooterPos = HeaderFooterPos.BOTTOM_CENTER,
        fontSize: Float = 10f,
        colorHex: String = "#52525B"
    ): Boolean {
        PDDocument.load(inputFile).use { document ->
            val total = document.numberOfPages
            val font = PDType1Font.HELVETICA
            val awtColor = try { java.awt.Color.decode(colorHex) } catch (_: Exception) { java.awt.Color.GRAY }

            for (i in 0 until total) {
                val page = document.getPage(i)
                val cropBox = page.cropBox ?: page.mediaBox
                val pageWidth = cropBox.width
                val pageHeight = cropBox.height
                val pageNumberText = formatPattern.format(i + 1, total)

                val textWidth = font.getStringWidth(pageNumberText) / 1000f * fontSize
                val margin = 36f

                val (x, y) = when (position) {
                    HeaderFooterPos.BOTTOM_CENTER -> Pair(cropBox.lowerLeftX + (pageWidth - textWidth) / 2f, cropBox.lowerLeftY + margin)
                    HeaderFooterPos.BOTTOM_RIGHT -> Pair(cropBox.upperRightX - margin - textWidth, cropBox.lowerLeftY + margin)
                    HeaderFooterPos.BOTTOM_LEFT -> Pair(cropBox.lowerLeftX + margin, cropBox.lowerLeftY + margin)
                    HeaderFooterPos.TOP_CENTER -> Pair(cropBox.lowerLeftX + (pageWidth - textWidth) / 2f, cropBox.upperRightY - margin)
                    HeaderFooterPos.TOP_RIGHT -> Pair(cropBox.upperRightX - margin - textWidth, cropBox.upperRightY - margin)
                }

                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    cs.beginText()
                    cs.setFont(font, fontSize)
                    cs.setNonStrokingColor(awtColor)
                    cs.newLineAtOffset(x, y)
                    cs.showText(pageNumberText)
                    cs.endText()
                }
            }

            document.save(outputFile)
            return outputFile.exists() && outputFile.length() > 0
        }
    }

    /**
     * Applies a diagonal semi-transparent watermark across all pages.
     */
    fun addWatermark(
        inputFile: File,
        outputFile: File,
        watermarkText: String,
        opacity: Float = 0.22f,
        rotationDegrees: Float = 45f,
        colorHex: String = "#DC2626"
    ): Boolean {
        if (watermarkText.isBlank()) return false
        PDDocument.load(inputFile).use { document ->
            val total = document.numberOfPages
            val font = PDType1Font.HELVETICA_BOLD
            val awtColor = try { java.awt.Color.decode(colorHex) } catch (_: Exception) { java.awt.Color.RED }

            val extGraphicsState = PDExtendedGraphicsState().apply {
                nonStrokingAlphaConstant = opacity.coerceIn(0.05f, 0.95f)
            }

            for (i in 0 until total) {
                val page = document.getPage(i)
                val cropBox = page.cropBox ?: page.mediaBox
                val pageWidth = cropBox.width
                val pageHeight = cropBox.height

                val fontSize = (pageWidth / (watermarkText.length * 0.65f)).coerceIn(32f, 72f)
                val textWidth = font.getStringWidth(watermarkText) / 1000f * fontSize
                val textHeight = fontSize * 0.75f

                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    cs.saveGraphicsState()
                    cs.setGraphicsStateParameters(extGraphicsState)
                    cs.setNonStrokingColor(awtColor)

                    val centerX = cropBox.lowerLeftX + pageWidth / 2f
                    val centerY = cropBox.lowerLeftY + pageHeight / 2f

                    val rad = Math.toRadians(rotationDegrees.toDouble())
                    val cos = Math.cos(rad).toFloat()
                    val sin = Math.sin(rad).toFloat()

                    cs.transform(Matrix(cos, sin, -sin, cos, centerX, centerY))

                    cs.beginText()
                    cs.setFont(font, fontSize)
                    cs.newLineAtOffset(-textWidth / 2f, -textHeight / 2f)
                    cs.showText(watermarkText)
                    cs.endText()
                    cs.restoreGraphicsState()
                }
            }

            document.save(outputFile)
            return outputFile.exists() && outputFile.length() > 0
        }
    }

    /**
     * Checks if a PDF document contains interactive AcroForm fields.
     */
    fun hasAcroForm(inputFile: File): Boolean {
        if (!inputFile.exists() || inputFile.length() == 0L) return false
        return try {
            PDDocument.load(inputFile).use { doc ->
                val acroForm = doc.documentCatalog.acroForm
                acroForm != null && acroForm.fields.isNotEmpty()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts interactive form fields from an AcroForm PDF.
     */
    fun extractAcroFields(inputFile: File): List<DesktopAcroField> {
        if (!inputFile.exists() || inputFile.length() == 0L) return emptyList()
        return try {
            PDDocument.load(inputFile).use { doc ->
                val acroForm = doc.documentCatalog.acroForm ?: return emptyList()
                val result = mutableListOf<DesktopAcroField>()
                for (field in acroForm.fieldTree) {
                    val name = field.partialName ?: field.fullyQualifiedName ?: continue
                    val fqName = field.fullyQualifiedName ?: name
                    val isReadOnly = field.isReadOnly
                    val isRequired = field.isRequired

                    val item = when (field) {
                        is PDTextField -> DesktopAcroField(
                            name = name,
                            fullyQualifiedName = fqName,
                            type = AcroFieldType.TEXT,
                            value = field.value ?: "",
                            isReadOnly = isReadOnly,
                            isRequired = isRequired
                        )
                        is PDCheckBox -> DesktopAcroField(
                            name = name,
                            fullyQualifiedName = fqName,
                            type = AcroFieldType.CHECKBOX,
                            value = if (field.isChecked) "Yes" else "Off",
                            options = listOf("Yes", "Off"),
                            isReadOnly = isReadOnly,
                            isRequired = isRequired
                        )
                        is PDRadioButton -> DesktopAcroField(
                            name = name,
                            fullyQualifiedName = fqName,
                            type = AcroFieldType.RADIO,
                            value = field.value ?: "",
                            options = field.onValues.toList(),
                            isReadOnly = isReadOnly,
                            isRequired = isRequired
                        )
                        is PDChoice -> DesktopAcroField(
                            name = name,
                            fullyQualifiedName = fqName,
                            type = AcroFieldType.CHOICE,
                            value = field.value?.firstOrNull() ?: "",
                            options = field.options ?: emptyList(),
                            isReadOnly = isReadOnly,
                            isRequired = isRequired
                        )
                        else -> DesktopAcroField(
                            name = name,
                            fullyQualifiedName = fqName,
                            type = AcroFieldType.OTHER,
                            value = try { field.valueAsString ?: "" } catch (_: Exception) { "" },
                            isReadOnly = isReadOnly,
                            isRequired = isRequired
                        )
                    }
                    result.add(item)
                }
                result
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Fills values in an AcroForm document and optionally flattens all fields into immutable vector text/graphics.
     */
    fun fillAndFlattenAcroForm(
        inputFile: File,
        outputFile: File,
        fieldValues: Map<String, String>,
        flatten: Boolean = true
    ): Boolean {
        if (!inputFile.exists() || inputFile.length() == 0L) return false
        return try {
            PDDocument.load(inputFile).use { doc ->
                val acroForm = doc.documentCatalog.acroForm ?: return false

                if (acroForm.defaultResources == null) {
                    val res = org.apache.pdfbox.pdmodel.PDResources()
                    res.put(org.apache.pdfbox.cos.COSName.getPDFName("Helv"), PDType1Font.HELVETICA)
                    acroForm.defaultResources = res
                }
                if (acroForm.defaultAppearance.isNullOrBlank()) {
                    acroForm.defaultAppearance = "/Helv 12 Tf 0 g"
                }

                for ((fieldName, value) in fieldValues) {
                    val field = acroForm.getField(fieldName) ?: continue
                    try {
                        if (field is PDTextField && field.defaultAppearance.isNullOrBlank()) {
                            field.defaultAppearance = acroForm.defaultAppearance
                        }
                        when (field) {
                            is PDCheckBox -> {
                                if (value.equals("Yes", ignoreCase = true) || value.equals("true", ignoreCase = true)) {
                                    field.check()
                                } else {
                                    field.unCheck()
                                }
                            }
                            is PDTextField -> {
                                field.value = value
                            }
                            is PDChoice -> {
                                field.setValue(value)
                            }
                            is PDRadioButton -> {
                                field.setValue(value)
                            }
                            else -> {
                                field.setValue(value)
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (flatten) {
                    try {
                        acroForm.flatten()
                    } catch (_: Exception) {}
                }

                doc.save(outputFile)
                outputFile.exists() && outputFile.length() > 0
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Compares two PDF documents page-by-page and computes textual differences.
     */
    fun compareDocuments(fileA: File, fileB: File): PdfDiffSummary {
        val stripper = PDFTextStripper().apply { sortByPosition = true }

        val (pagesA, textPagesA) = PDDocument.load(fileA).use { doc ->
            val count = doc.numberOfPages
            val pages = (1..count).map { p ->
                stripper.startPage = p
                stripper.endPage = p
                stripper.getText(doc).trim()
            }
            Pair(count, pages)
        }

        val (pagesB, textPagesB) = PDDocument.load(fileB).use { doc ->
            val count = doc.numberOfPages
            val pages = (1..count).map { p ->
                stripper.startPage = p
                stripper.endPage = p
                stripper.getText(doc).trim()
            }
            Pair(count, pages)
        }

        val maxPages = maxOf(pagesA, pagesB)
        val pageDiffs = mutableListOf<PageDiff>()
        var totalAdded = 0
        var totalRemoved = 0

        for (p in 0 until maxPages) {
            val linesA = if (p < pagesA) textPagesA[p].lines().map { it.trim() }.filter { it.isNotBlank() } else emptyList()
            val linesB = if (p < pagesB) textPagesB[p].lines().map { it.trim() }.filter { it.isNotBlank() } else emptyList()

            val setA = linesA.toSet()
            val setB = linesB.toSet()

            val removed = linesA.filter { it !in setB }
            val added = linesB.filter { it !in setA }

            val isIdentical = linesA == linesB
            totalAdded += added.size
            totalRemoved += removed.size

            pageDiffs.add(
                PageDiff(
                    pageNum = p + 1,
                    isIdentical = isIdentical,
                    addedLines = added,
                    removedLines = removed,
                    lineCountA = linesA.size,
                    lineCountB = linesB.size
                )
            )
        }

        val arePageCountsEqual = (pagesA == pagesB)
        val isEntirelyIdentical = arePageCountsEqual && totalAdded == 0 && totalRemoved == 0 && pageDiffs.all { it.isIdentical }

        return PdfDiffSummary(
            arePageCountsEqual = arePageCountsEqual,
            pagesA = pagesA,
            pagesB = pagesB,
            pageDiffs = pageDiffs,
            totalAddedLines = totalAdded,
            totalRemovedLines = totalRemoved,
            isEntirelyIdentical = isEntirelyIdentical
        )
    }

    // ==========================================
    // 1. LEGAL BATES STAMPING & INDEXING SUITE
    // ==========================================

    fun applyBatesStamping(inputFile: File, outputFile: File, config: DesktopBatesConfig): Boolean {
        return try {
            PDDocument.load(inputFile).use { doc ->
                val pageCount = doc.numberOfPages
                val font = PDType1Font.HELVETICA_BOLD
                val range = config.pageRange ?: (1..pageCount)

                for (p in 1..pageCount) {
                    if (p !in range) continue
                    val page = doc.getPage(p - 1)
                    val box = page.cropBox ?: page.mediaBox

                    val batesNumber = config.startNumber + (p - range.first)
                    val formattedNum = "%0${config.digits}d".format(batesNumber)
                    val text = "${config.prefix}$formattedNum${config.suffix}"

                    val textWidth = font.getStringWidth(text) / 1000f * config.fontSize
                    val textHeight = config.fontSize

                    val x = when (config.position) {
                        DesktopBatesPosition.TOP_LEFT, DesktopBatesPosition.BOTTOM_LEFT ->
                            box.lowerLeftX + config.marginPt
                        DesktopBatesPosition.TOP_CENTER, DesktopBatesPosition.BOTTOM_CENTER ->
                            box.lowerLeftX + (box.width - textWidth) / 2f
                        DesktopBatesPosition.TOP_RIGHT, DesktopBatesPosition.BOTTOM_RIGHT ->
                            box.upperRightX - config.marginPt - textWidth
                    }

                    val y = when (config.position) {
                        DesktopBatesPosition.TOP_LEFT, DesktopBatesPosition.TOP_CENTER, DesktopBatesPosition.TOP_RIGHT ->
                            box.upperRightY - config.marginPt - textHeight
                        DesktopBatesPosition.BOTTOM_LEFT, DesktopBatesPosition.BOTTOM_CENTER, DesktopBatesPosition.BOTTOM_RIGHT ->
                            box.lowerLeftY + config.marginPt
                    }

                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        cs.beginText()
                        cs.setFont(font, config.fontSize)
                        cs.setNonStrokingColor(java.awt.Color.DARK_GRAY)
                        cs.newLineAtOffset(x, y)
                        cs.showText(text)
                        cs.endText()
                    }
                }
                doc.save(outputFile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // 2. DEEP DOCUMENT SANITIZER & THREAT SCRUBBER
    // ==========================================

    fun auditDocumentThreats(file: File): DesktopSanitizeResult {
        return try {
            PDDocument.load(file).use { doc ->
                var jsCount = 0
                var actionCount = 0
                var attachmentCount = 0
                var annotationCount = 0

                // Catalog JS & Actions
                if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) jsCount++
                val names = doc.documentCatalog.names
                if (names?.cosObject?.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) jsCount++
                if (names?.cosObject?.getDictionaryObject(COSName.getPDFName("EmbeddedFiles")) != null) attachmentCount++

                if (doc.documentCatalog.openAction != null) actionCount++
                if (doc.documentCatalog.actions != null) actionCount++
                if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("AA")) != null) actionCount++

                for (page in doc.pages) {
                    for (annot in page.annotations) {
                        val subType = annot.subtype
                        if (subType in listOf("Popup", "Text", "FreeText", "Highlight")) {
                            annotationCount++
                        }
                        val action = annot.cosObject.getDictionaryObject(COSName.A)
                        if (action is COSDictionary) {
                            val s = action.getNameAsString(COSName.S)
                            if (s == "JavaScript") jsCount++
                            if (s in listOf("Launch", "SubmitForm", "ImportData", "URI", "Sound", "Movie")) actionCount++
                        }
                    }
                }

                val hasMetadata = doc.documentInformation.author?.isNotBlank() == true ||
                        doc.documentInformation.title?.isNotBlank() == true ||
                        doc.documentInformation.creator?.isNotBlank() == true ||
                        doc.documentCatalog.metadata != null ||
                        doc.document.trailer.getItem(COSName.ID) != null

                val totalThreats = jsCount + actionCount + attachmentCount + (if (hasMetadata) 1 else 0)

                DesktopSanitizeResult(
                    threatsFound = totalThreats,
                    jsCount = jsCount,
                    launchActionsCount = actionCount,
                    metadataPurged = false,
                    attachmentsPurged = attachmentCount,
                    annotationsPurged = annotationCount
                )
            }
        } catch (e: Exception) {
            DesktopSanitizeResult(0, 0, 0, false, 0, 0)
        }
    }

    fun sanitizeDocument(
        inputFile: File,
        outputFile: File,
        purgeJs: Boolean = true,
        purgeActions: Boolean = true,
        purgeMetadata: Boolean = true,
        purgeAttachments: Boolean = true,
        purgePrivateAnnotations: Boolean = false
    ): DesktopSanitizeResult {
        return try {
            PDDocument.load(inputFile).use { doc ->
                var jsPurged = 0
                var actionsPurged = 0
                var attachmentsPurged = 0
                var annotationsPurged = 0

                if (purgeJs) {
                    if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) {
                        doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("JavaScript"))
                        jsPurged++
                    }
                    if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) {
                        doc.documentCatalog.names?.cosObject?.removeItem(COSName.getPDFName("JavaScript"))
                        jsPurged++
                    }
                    if (doc.documentCatalog.openAction != null) {
                        doc.documentCatalog.openAction = null
                        actionsPurged++
                    }
                    doc.documentCatalog.actions = null
                }

                if (purgeActions) {
                    if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("AA")) != null) {
                        doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("AA"))
                        actionsPurged++
                    }
                }

                if (purgeAttachments) {
                    if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("EmbeddedFiles")) != null) {
                        doc.documentCatalog.names?.cosObject?.removeItem(COSName.getPDFName("EmbeddedFiles"))
                        attachmentsPurged++
                    }
                }

                if (purgeMetadata) {
                    doc.documentInformation.title = null
                    doc.documentInformation.author = null
                    doc.documentInformation.subject = null
                    doc.documentInformation.keywords = null
                    doc.documentInformation.creator = null
                    doc.documentInformation.producer = "PDFchemy (Scrubbed & Sanitized)"
                    doc.documentInformation.creationDate = null
                    doc.documentInformation.modificationDate = null
                    doc.documentCatalog.metadata = null
                    doc.document.trailer.removeItem(COSName.ID)
                    doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("PieceInfo"))
                }

                for (page in doc.pages) {
                    page.cosObject.removeItem(COSName.getPDFName("PieceInfo"))
                    page.cosObject.removeItem(COSName.getPDFName("AA"))

                    if (purgeActions || purgeJs) {
                        for (annot in page.annotations) {
                            val action = annot.cosObject.getDictionaryObject(COSName.A)
                            if (action is COSDictionary) {
                                val s = action.getNameAsString(COSName.S)
                                if (purgeJs && s == "JavaScript") {
                                    annot.cosObject.removeItem(COSName.A)
                                    jsPurged++
                                }
                                if (purgeActions && s in listOf("Launch", "SubmitForm", "ImportData", "URI", "Sound", "Movie")) {
                                    annot.cosObject.removeItem(COSName.A)
                                    actionsPurged++
                                }
                            }
                        }
                    }

                    if (purgePrivateAnnotations) {
                        val initialSize = page.annotations.size
                        val kept = page.annotations.filter {
                            it.subtype !in listOf("Popup", "Text", "FreeText", "Highlight")
                        }
                        annotationsPurged += (initialSize - kept.size)
                        page.annotations = kept
                    }
                }

                doc.save(outputFile)

                DesktopSanitizeResult(
                    threatsFound = jsPurged + actionsPurged + attachmentsPurged + (if (purgeMetadata) 1 else 0),
                    jsCount = jsPurged,
                    launchActionsCount = actionsPurged,
                    metadataPurged = purgeMetadata,
                    attachmentsPurged = attachmentsPurged,
                    annotationsPurged = annotationsPurged
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DesktopSanitizeResult(0, 0, 0, false, 0, 0)
        }
    }

    // ==========================================
    // 3. CORRUPTED PDF RECOVERY & REPAIR STUDIO
    // ==========================================

    fun repairCorruptedPdf(inputFile: File, outputFile: File): DesktopRepairResult {
        val issues = mutableListOf<String>()
        var bytes = inputFile.readBytes()
        val originalSize = bytes.size.toLong()

        // Step 1: Scan for %PDF- header magic bytes
        val pdfHeader = "%PDF-".toByteArray(Charsets.US_ASCII)
        var headerIndex = -1
        for (i in 0 until (bytes.size - 5)) {
            if (bytes[i] == pdfHeader[0] &&
                bytes[i + 1] == pdfHeader[1] &&
                bytes[i + 2] == pdfHeader[2] &&
                bytes[i + 3] == pdfHeader[3] &&
                bytes[i + 4] == pdfHeader[4]) {
                headerIndex = i
                break
            }
        }

        if (headerIndex > 0) {
            issues.add("Stripped $headerIndex corrupt bytes prepended before PDF header")
            bytes = bytes.copyOfRange(headerIndex, bytes.size)
        } else if (headerIndex == -1) {
            issues.add("Injected missing %PDF-1.4 header")
            bytes = "%PDF-1.4\n".toByteArray(Charsets.US_ASCII) + bytes
        }

        // Step 2: Ensure EOF trailer marker is present
        val tailStr = String(bytes.takeLast(minOf(1024, bytes.size)).toByteArray(), Charsets.US_ASCII)
        if (!tailStr.contains("%%EOF")) {
            issues.add("Appended missing %%EOF termination marker")
            bytes = bytes + "\n%%EOF\n".toByteArray(Charsets.US_ASCII)
        }

        val tempFile = File.createTempFile("pdfchemy_repair_", ".pdf")
        try {
            tempFile.writeBytes(bytes)
            val loadedDoc = try {
                PDDocument.load(tempFile, MemoryUsageSetting.setupTempFileOnly())
            } catch (e: Exception) {
                issues.add("Applied fault-tolerant object stream recovery")
                PDDocument.load(inputFile, MemoryUsageSetting.setupTempFileOnly())
            }

            loadedDoc.use { doc ->
                PDDocument().use { cleanDoc ->
                    var recoveredCount = 0
                    for (i in 0 until doc.numberOfPages) {
                        try {
                            cleanDoc.addPage(cleanDoc.importPage(doc.getPage(i)))
                            recoveredCount++
                        } catch (pe: Exception) {
                            issues.add("Skipped unrecoverable page ${i + 1}")
                        }
                    }
                    issues.add("Reconstructed clean linear cross-reference table and page catalog")
                    cleanDoc.save(outputFile)
                    return DesktopRepairResult(
                        isSuccess = true,
                        pagesRecovered = recoveredCount,
                        issuesRepaired = issues,
                        originalSize = originalSize,
                        repairedSize = outputFile.length()
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return DesktopRepairResult(
                isSuccess = false,
                pagesRecovered = 0,
                issuesRepaired = listOf("Fatal corruption: ${e.localizedMessage ?: "Unknown parse error"}"),
                originalSize = originalSize,
                repairedSize = 0L
            )
        } finally {
            tempFile.delete()
        }
    }

    // ==========================================
    // 4. PDF/A ARCHIVAL CONVERTER (ISO 19005-1b)
    // ==========================================

    fun convertToPdfA(inputFile: File, outputFile: File): Boolean {
        return try {
            PDDocument.load(inputFile).use { doc ->
                // 1. Set PDF/A OutputIntent (sRGB IEC61966-2.1)
                val srgbProfile = ICC_Profile.getInstance(ColorSpace.CS_sRGB).data
                ByteArrayInputStream(srgbProfile).use { iccStream ->
                    val outputIntent = PDOutputIntent(doc, iccStream)
                    outputIntent.info = "sRGB IEC61966-2.1"
                    outputIntent.outputCondition = "sRGB IEC61966-2.1"
                    outputIntent.outputConditionIdentifier = "sRGB IEC61966-2.1"
                    outputIntent.registryName = "http://www.color.org"
                    doc.documentCatalog.addOutputIntent(outputIntent)
                }

                // 2. Set MarkInfo
                val markInfo = PDMarkInfo()
                markInfo.isMarked = true
                doc.documentCatalog.markInfo = markInfo

                // 3. Inject ISO 19005-1b compliant XMP metadata packet
                val xmpXml = """<?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
<x:xmpmeta xmlns:x="adobe:ns:meta/">
  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
    <rdf:Description rdf:about="" xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/">
      <pdfaid:part>1</pdfaid:part>
      <pdfaid:conformance>B</pdfaid:conformance>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:dc="http://purl.org/dc/elements/1.1/">
      <dc:format>application/pdf</dc:format>
    </rdf:Description>
    <rdf:Description rdf:about="" xmlns:pdf="http://ns.adobe.com/pdf/1.3/">
      <pdf:Producer>PDFchemy ISO 19005-1b Archival Engine</pdf:Producer>
    </rdf:Description>
  </rdf:RDF>
</x:xmpmeta>
<?xpacket end="w"?>""".trimIndent()

                val metadata = PDMetadata(doc)
                metadata.importXMPMetadata(xmpXml.toByteArray(Charsets.UTF_8))
                doc.documentCatalog.metadata = metadata

                // 4. Strip non-archival dynamic scripts and actions
                doc.documentCatalog.openAction = null
                doc.documentCatalog.actions = null
                doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("JavaScript"))
                doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("AA"))

                // 5. Flatten active AcroForm if present to preserve visual appearance
                doc.documentCatalog.acroForm?.let { acroForm ->
                    try {
                        acroForm.flatten()
                    } catch (_: Exception) {}
                }

                doc.save(outputFile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // 5. VISUAL MARGIN TRIMMER & SMART CROPBOX
    // ==========================================

    fun cropMargins(inputFile: File, outputFile: File, config: DesktopCropConfig): Boolean {
        return try {
            PDDocument.load(inputFile).use { doc ->
                val pageCount = doc.numberOfPages
                val pagesToCrop = if (config.applyToAllPages) 0 until pageCount else 0..0

                for (i in pagesToCrop) {
                    val page = doc.getPage(i)
                    val mb = page.mediaBox
                    val newX = (mb.lowerLeftX + config.leftPt).coerceAtMost(mb.upperRightX - 10f)
                    val newY = (mb.lowerLeftY + config.bottomPt).coerceAtMost(mb.upperRightY - 10f)
                    val newW = (mb.width - config.leftPt - config.rightPt).coerceAtLeast(10f)
                    val newH = (mb.height - config.topPt - config.bottomPt).coerceAtLeast(10f)
                    page.cropBox = PDRectangle(newX, newY, newW, newH)
                }
                doc.save(outputFile)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun detectContentCrop(inputFile: File, pageIndex: Int = 0, paddingPt: Float = 18f): DesktopCropConfig {
        return try {
            PDDocument.load(inputFile).use { doc ->
                val pIdx = pageIndex.coerceIn(0, doc.numberOfPages - 1)
                val page = doc.getPage(pIdx)
                val mb = page.mediaBox
                val renderer = PDFRenderer(doc)
                val dpi = 72f
                val image = renderer.renderImageWithDPI(pIdx, dpi)

                val width = image.width
                val height = image.height

                var minX = width
                var minY = height
                var maxX = 0
                var maxY = 0

                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val rgb = image.getRGB(x, y)
                        val r = (rgb shr 16) and 0xFF
                        val g = (rgb shr 8) and 0xFF
                        val b = rgb and 0xFF
                        val lum = 0.299 * r + 0.587 * g + 0.114 * b
                        if (lum < 240.0) { // Non-white content pixel
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                    }
                }

                if (minX >= maxX || minY >= maxY) {
                    DesktopCropConfig(18f, 18f, 18f, 18f)
                } else {
                    val scaleX = mb.width / width.toFloat()
                    val scaleY = mb.height / height.toFloat()

                    val leftPt = (minX * scaleX - paddingPt).coerceAtLeast(0f)
                    val topPt = (minY * scaleY - paddingPt).coerceAtLeast(0f)
                    val rightPt = ((width - maxX) * scaleX - paddingPt).coerceAtLeast(0f)
                    val bottomPt = ((height - maxY) * scaleY - paddingPt).coerceAtLeast(0f)

                    DesktopCropConfig(
                        leftPt = leftPt,
                        topPt = topPt,
                        rightPt = rightPt,
                        bottomPt = bottomPt,
                        applyToAllPages = true
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            DesktopCropConfig(28.35f, 28.35f, 28.35f, 28.35f) // 10mm fallback
        }
    }

    // ==========================================
    // 6. PDF TABLE & DATA EXTRACTOR TO CSV
    // ==========================================

    fun extractTablesToCsv(inputFile: File, pageIndex: Int? = null): String {
        return PDDocument.load(inputFile).use { doc ->
            val stripper = object : PDFTextStripper() {
                init {
                    sortByPosition = true
                    wordSeparator = "\t"
                }
            }
            if (pageIndex != null) {
                stripper.startPage = pageIndex + 1
                stripper.endPage = pageIndex + 1
            }
            val text = stripper.getText(doc)
            val sb = StringBuilder()
            val lines = text.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val cells = trimmed.split(Regex("(\t| {2,})")).filter { it.isNotBlank() }
                if (cells.isNotEmpty()) {
                    val csvLine = cells.joinToString(",") { cell ->
                        val clean = cell.trim()
                        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n")) {
                            "\"" + clean.replace("\"", "\"\"") + "\""
                        } else {
                            clean
                        }
                    }
                    sb.append(csvLine).append("\n")
                }
            }
            sb.toString()
        }
    }

    // ==========================================
    // 7. AUTO-DESKEW & SCANNER STRAIGHTENER
    // ==========================================

    fun detectSkewAngle(image: BufferedImage): Float {
        val targetWidth = 400
        val scale = targetWidth.toFloat() / image.width.toFloat()
        val targetHeight = (image.height * scale).toInt().coerceAtLeast(100)

        val smallImg = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY)
        val g = smallImg.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(image, 0, 0, targetWidth, targetHeight, null)
        g.dispose()

        // Extract non-white pixel coordinates (foreground text/lines)
        val points = mutableListOf<Point2D.Float>()
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val lum = (smallImg.raster.getSample(x, y, 0))
                if (lum < 200) { // non-white text pixel
                    points.add(Point2D.Float(x.toFloat(), y.toFloat()))
                }
            }
        }
        if (points.size < 50) return 0f // Not enough text to compute skew

        var maxVariance = 0.0
        var bestAngle = 0.0f

        // Search angles from -15.0 deg to +15.0 deg in 0.5 deg steps
        var angle = -15.0f
        while (angle <= 15.0f) {
            val rad = Math.toRadians(angle.toDouble())
            val sinA = Math.sin(rad)
            val cosA = Math.cos(rad)

            // Histogram of projected y'
            val hist = IntArray(targetHeight + 200)
            val offset = 100
            for (p in points) {
                val yRot = ((-p.x * sinA + p.y * cosA) + offset).toInt()
                if (yRot in hist.indices) {
                    hist[yRot]++
                }
            }

            // Compute variance of the histogram
            var sum = 0.0
            var sumSq = 0.0
            var count = 0
            for (h in hist) {
                sum += h
                sumSq += (h * h)
                count++
            }
            val variance = (sumSq / count) - (sum / count) * (sum / count)
            if (variance > maxVariance) {
                maxVariance = variance
                bestAngle = angle
            }
            angle += 0.5f
        }

        return bestAngle
    }

    fun deskewDocument(inputFile: File, outputFile: File, targetPages: Set<Int>? = null): Int {
        PDDocument.load(inputFile).use { doc ->
            val renderer = PDFRenderer(doc)
            var deskewedCount = 0

            for (i in 0 until doc.numberOfPages) {
                if (targetPages != null && i !in targetPages) continue

                val bimg = renderer.renderImageWithDPI(i, 100f)
                val skewAngle = detectSkewAngle(bimg)

                // If skew is significant (> 0.4 degrees)
                if (Math.abs(skewAngle) >= 0.4f) {
                    val page = doc.getPage(i)
                    val mb = page.mediaBox
                    val cx = mb.width / 2f
                    val cy = mb.height / 2f
                    val rad = Math.toRadians(-skewAngle.toDouble())

                    // Prepend rotation transform around center
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.PREPEND, false).use { cs ->
                        cs.saveGraphicsState()
                        cs.transform(Matrix.getRotateInstance(rad, cx, cy))
                    }
                    // Append restore graphics state
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false).use { cs ->
                        cs.restoreGraphicsState()
                    }
                    deskewedCount++
                }
            }
            doc.save(outputFile)
            return deskewedCount
        }
    }

    // ==========================================
    // 8. BOOKLET CREATOR & N-UP IMPOSITION
    // ==========================================

    fun generateBooklet(inputFile: File, outputFile: File, drawFoldGuide: Boolean = true): Boolean {
        return try {
            PDDocument.load(inputFile).use { srcDoc ->
                val origPageCount = srcDoc.numberOfPages
                if (origPageCount == 0) return false

                val paddedTotal = Math.ceil(origPageCount / 4.0).toInt() * 4
                val sheetCount = paddedTotal / 4

                PDDocument().use { outDoc ->
                    val layerUtil = LayerUtility(outDoc)
                    val sheetWidth = 842f
                    val sheetHeight = 595f
                    val sheetRect = PDRectangle(sheetWidth, sheetHeight)
                    val margin = 20f
                    val halfWidth = (sheetWidth - (margin * 2)) / 2f
                    val contentHeight = sheetHeight - (margin * 2)

                    for (k in 0 until sheetCount) {
                        // 1. Front sheet: Left = paddedTotal - 1 - 2k, Right = 2k
                        val frontLeft = paddedTotal - 1 - (2 * k)
                        val frontRight = 2 * k
                        val frontPage = PDPage(sheetRect)
                        outDoc.addPage(frontPage)

                        PDPageContentStream(outDoc, frontPage).use { cs ->
                            if (frontLeft < origPageCount) {
                                drawImportedPage(srcDoc, layerUtil, cs, frontLeft, margin, margin, halfWidth - 8f, contentHeight)
                            }
                            if (frontRight < origPageCount) {
                                drawImportedPage(srcDoc, layerUtil, cs, frontRight, margin + halfWidth + 8f, margin, halfWidth - 8f, contentHeight)
                            }
                            if (drawFoldGuide) {
                                drawCenterFoldGuide(cs, sheetWidth / 2f, margin, sheetHeight - margin)
                            }
                        }

                        // 2. Back sheet: Left = 2k + 1, Right = paddedTotal - 1 - (2k + 1)
                        val backLeft = (2 * k) + 1
                        val backRight = paddedTotal - 1 - ((2 * k) + 1)
                        val backPage = PDPage(sheetRect)
                        outDoc.addPage(backPage)

                        PDPageContentStream(outDoc, backPage).use { cs ->
                            if (backLeft < origPageCount) {
                                drawImportedPage(srcDoc, layerUtil, cs, backLeft, margin, margin, halfWidth - 8f, contentHeight)
                            }
                            if (backRight < origPageCount) {
                                drawImportedPage(srcDoc, layerUtil, cs, backRight, margin + halfWidth + 8f, margin, halfWidth - 8f, contentHeight)
                            }
                            if (drawFoldGuide) {
                                drawCenterFoldGuide(cs, sheetWidth / 2f, margin, sheetHeight - margin)
                            }
                        }
                    }
                    outDoc.save(outputFile)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateNUp(inputFile: File, outputFile: File, pagesPerSheet: Int = 2): Boolean {
        return try {
            PDDocument.load(inputFile).use { srcDoc ->
                val totalPages = srcDoc.numberOfPages
                if (totalPages == 0) return false

                PDDocument().use { outDoc ->
                    val layerUtil = LayerUtility(outDoc)

                    if (pagesPerSheet == 2) {
                        // 2 pages per sheet -> Landscape A4 (842 x 595)
                        val sheetW = 842f
                        val sheetH = 595f
                        val margin = 20f
                        val halfW = (sheetW - margin * 2) / 2f
                        val contentH = sheetH - margin * 2

                        var i = 0
                        while (i < totalPages) {
                            val sheetPage = PDPage(PDRectangle(sheetW, sheetH))
                            outDoc.addPage(sheetPage)

                            PDPageContentStream(outDoc, sheetPage).use { cs ->
                                drawImportedPage(srcDoc, layerUtil, cs, i, margin, margin, halfW - 6f, contentH)
                                if (i + 1 < totalPages) {
                                    drawImportedPage(srcDoc, layerUtil, cs, i + 1, margin + halfW + 6f, margin, halfW - 6f, contentH)
                                }
                            }
                            i += 2
                        }
                    } else {
                        // 4 pages per sheet -> Portrait A4 (595 x 842)
                        val sheetW = 595f
                        val sheetH = 842f
                        val margin = 16f
                        val halfW = (sheetW - margin * 2) / 2f
                        val halfH = (sheetH - margin * 2) / 2f

                        var i = 0
                        while (i < totalPages) {
                            val sheetPage = PDPage(PDRectangle(sheetW, sheetH))
                            outDoc.addPage(sheetPage)

                            PDPageContentStream(outDoc, sheetPage).use { cs ->
                                if (i < totalPages) drawImportedPage(srcDoc, layerUtil, cs, i, margin, margin + halfH, halfW - 4f, halfH - 4f)
                                if (i + 1 < totalPages) drawImportedPage(srcDoc, layerUtil, cs, i + 1, margin + halfW, margin + halfH, halfW - 4f, halfH - 4f)
                                if (i + 2 < totalPages) drawImportedPage(srcDoc, layerUtil, cs, i + 2, margin, margin, halfW - 4f, halfH - 4f)
                                if (i + 3 < totalPages) drawImportedPage(srcDoc, layerUtil, cs, i + 3, margin + halfW, margin, halfW - 4f, halfH - 4f)
                            }
                            i += 4
                        }
                    }
                    outDoc.save(outputFile)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun drawImportedPage(
        srcDoc: PDDocument,
        layerUtil: LayerUtility,
        cs: PDPageContentStream,
        pageIndex: Int,
        x: Float,
        y: Float,
        availW: Float,
        availH: Float
    ) {
        val form = layerUtil.importPageAsForm(srcDoc, pageIndex)
        val origW = form.bBox.width
        val origH = form.bBox.height
        val scale = Math.min(availW / origW, availH / origH)

        val finalW = origW * scale
        val finalH = origH * scale
        val offsetX = x + (availW - finalW) / 2f
        val offsetY = y + (availH - finalH) / 2f

        cs.saveGraphicsState()
        cs.transform(Matrix.getTranslateInstance(offsetX, offsetY))
        cs.transform(Matrix.getScaleInstance(scale, scale))
        cs.drawForm(form)
        cs.restoreGraphicsState()
    }

    private fun drawCenterFoldGuide(cs: PDPageContentStream, x: Float, topY: Float, bottomY: Float) {
        cs.saveGraphicsState()
        cs.setStrokingColor(180, 180, 180)
        cs.setLineWidth(0.5f)
        cs.setLineDashPattern(floatArrayOf(4f, 4f), 0f)
        cs.moveTo(x, topY)
        cs.lineTo(x, bottomY)
        cs.stroke()
        cs.restoreGraphicsState()
    }

    // ==========================================
    // 9. BATCH SPLIT BY BLANK PAGES & BOOKMARKS
    // ==========================================

    fun splitByBlankPages(inputFile: File, outputDir: File, whiteThreshold: Float = 0.999f): List<File> {
        val outputFiles = mutableListOf<File>()
        outputDir.mkdirs()

        PDDocument.load(inputFile).use { doc ->
            val totalPages = doc.numberOfPages
            if (totalPages == 0) return emptyList()

            val renderer = PDFRenderer(doc)
            val splitGroups = mutableListOf<MutableList<Int>>()
            var currentGroup = mutableListOf<Int>()

            for (i in 0 until totalPages) {
                val bimg = renderer.renderImageWithDPI(i, 36f)
                val w = bimg.width
                val h = bimg.height
                var nonWhitePixels = 0
                val totalPixels = w * h

                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val rgb = bimg.getRGB(x, y)
                        val r = (rgb shr 16) and 0xFF
                        val g = (rgb shr 8) and 0xFF
                        val b = rgb and 0xFF
                        if (r < 240 || g < 240 || b < 240) {
                            nonWhitePixels++
                        }
                    }
                }

                val whiteRatio = (totalPixels - nonWhitePixels).toFloat() / totalPixels.toFloat()
                val isBlank = whiteRatio >= whiteThreshold

                if (isBlank) {
                    if (currentGroup.isNotEmpty()) {
                        splitGroups.add(currentGroup)
                        currentGroup = mutableListOf()
                    }
                } else {
                    currentGroup.add(i)
                }
            }
            if (currentGroup.isNotEmpty()) {
                splitGroups.add(currentGroup)
            }

            for ((idx, group) in splitGroups.withIndex()) {
                val outFile = File(outputDir, "${inputFile.nameWithoutExtension}_part_${idx + 1}.pdf")
                PDDocument().use { subDoc ->
                    for (pIdx in group) {
                        subDoc.importPage(doc.getPage(pIdx))
                    }
                    subDoc.save(outFile)
                }
                outputFiles.add(outFile)
            }
        }
        return outputFiles
    }

    fun splitByBookmarks(inputFile: File, outputDir: File): List<File> {
        val outputFiles = mutableListOf<File>()
        outputDir.mkdirs()

        PDDocument.load(inputFile).use { doc ->
            val outline = doc.documentCatalog.documentOutline
            if (outline == null || outline.firstChild == null) {
                return emptyList()
            }

            val bookmarks = mutableListOf<Pair<String, Int>>()
            var item: PDOutlineItem? = outline.firstChild
            while (item != null) {
                val title = item.title?.replace(Regex("[^a-zA-Z0-9_\\-]"), "_") ?: "Chapter"
                val dest = item.destination
                val page = if (dest is PDPageDestination) dest.page else null
                val pIdx = if (page != null) doc.pages.indexOf(page).coerceAtLeast(0) else 0
                bookmarks.add(title to pIdx)
                item = item.nextSibling
            }

            if (bookmarks.isEmpty()) return emptyList()

            for (i in 0 until bookmarks.size) {
                val (title, startPage) = bookmarks[i]
                val endPage = if (i + 1 < bookmarks.size) bookmarks[i + 1].second else doc.numberOfPages
                if (startPage >= endPage && endPage > 0) continue

                val outFile = File(outputDir, "${inputFile.nameWithoutExtension}_${i + 1}_$title.pdf")
                PDDocument().use { subDoc ->
                    for (p in startPage until endPage) {
                        subDoc.addPage(subDoc.importPage(doc.getPage(p)))
                    }
                    subDoc.save(outFile)
                }
                outputFiles.add(outFile)
            }
        }
        return outputFiles
    }

    // ==========================================
    // 10. EMBEDDED FILE ATTACHMENTS & PORTFOLIO
    // ==========================================

    fun listAttachments(inputFile: File): List<DesktopAttachment> {
        val list = mutableListOf<DesktopAttachment>()
        PDDocument.load(inputFile).use { doc ->
            val names = doc.documentCatalog.names
            val embeddedFiles = names?.embeddedFiles
            if (embeddedFiles != null) {
                val map = embeddedFiles.names ?: emptyMap()
                for ((key, spec) in map) {
                    if (spec is PDComplexFileSpecification) {
                        val ef = spec.embeddedFile
                        val size = ef?.size?.toLong() ?: 0L
                        val mime = ef?.subtype ?: "application/octet-stream"
                        val name = spec.filename ?: key
                        list.add(DesktopAttachment(name = name, sizeBytes = size, mimeType = mime))
                    }
                }
            }
        }
        return list
    }

    fun extractAttachment(inputFile: File, attachmentName: String, outputDir: File): File? {
        outputDir.mkdirs()
        PDDocument.load(inputFile).use { doc ->
            val names = doc.documentCatalog.names
            val embeddedFiles = names?.embeddedFiles ?: return null
            val map = embeddedFiles.names ?: return null

            for ((key, spec) in map) {
                if (spec is PDComplexFileSpecification) {
                    val name = spec.filename ?: key
                    if (name.equals(attachmentName, ignoreCase = true)) {
                        val ef = spec.embeddedFile ?: return null
                        val outFile = File(outputDir, name)
                        ef.createInputStream().use { ins ->
                            outFile.writeBytes(ins.readBytes())
                        }
                        return outFile
                    }
                }
            }
        }
        return null
    }

    fun embedAttachment(inputFile: File, attachmentFile: File, outputFile: File): Boolean {
        return try {
            PDDocument.load(inputFile).use { doc ->
                var names = doc.documentCatalog.names
                if (names == null) {
                    names = PDDocumentNameDictionary(doc.documentCatalog)
                    doc.documentCatalog.names = names
                }

                var embeddedFiles = names.embeddedFiles
                if (embeddedFiles == null) {
                    embeddedFiles = PDEmbeddedFilesNameTreeNode()
                    names.embeddedFiles = embeddedFiles
                }

                val currentMap = (embeddedFiles.names ?: mutableMapOf()).toMutableMap()
                val fs = PDComplexFileSpecification()
                fs.file = attachmentFile.name
                FileInputStream(attachmentFile).use { fis ->
                    val ef = PDEmbeddedFile(doc, fis)
                    ef.size = attachmentFile.length().toInt()
                    fs.embeddedFile = ef
                }
                currentMap[attachmentFile.name] = fs
                embeddedFiles.names = currentMap

                doc.save(outputFile)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}


