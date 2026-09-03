package com.pdfchemy.desktop.engine

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class DesktopPdfEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createTestPdf(pages: Int = 3, text: String = "Hello PDFchemy Desktop"): File {
        val file = tempFolder.newFile("test_sample_${System.currentTimeMillis()}.pdf")
        val doc = PDDocument()
        for (i in 1..pages) {
            val page = PDPage()
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                cs.newLineAtOffset(50f, 700f)
                cs.showText("$text - Page $i")
                cs.endText()
            }
        }
        doc.save(file)
        doc.close()
        return file
    }

    @Test
    fun testGetPageCount() {
        val pdf = createTestPdf(pages = 4)
        val count = DesktopPdfEngine.getPageCount(pdf)
        assertEquals(4, count)
    }

    @Test
    fun testExtractText() {
        val pdf = createTestPdf(pages = 1, text = "Confidential Report 2026")
        val extracted = DesktopPdfEngine.extractText(pdf)
        assertTrue(extracted.contains("Confidential Report 2026"))
    }

    @Test
    fun testMergePdfs() {
        val pdf1 = createTestPdf(pages = 2, text = "Doc A")
        val pdf2 = createTestPdf(pages = 3, text = "Doc B")
        val merged = tempFolder.newFile("merged.pdf")

        DesktopPdfEngine.mergePdfs(listOf(pdf1, pdf2), merged)
        assertEquals(5, DesktopPdfEngine.getPageCount(merged))
    }

    @Test
    fun testRotatePages() {
        val pdf = createTestPdf(pages = 2)
        val rotated = tempFolder.newFile("rotated.pdf")
        DesktopPdfEngine.rotatePages(pdf, rotated, degrees = 90)

        PDDocument.load(rotated).use { doc ->
            assertEquals(90, doc.getPage(0).rotation)
            assertEquals(90, doc.getPage(1).rotation)
        }
    }

    @Test
    fun testSaveReorderedPdf() {
        val pdf = createTestPdf(pages = 3, text = "Reorder Test")
        val reordered = tempFolder.newFile("reordered.pdf")

        // Reorder: Page 3 first, then Page 1 (rotated 90)
        val specs = listOf(
            PageItemSpec(originalPageIndex = 2, rotation = 0),
            PageItemSpec(originalPageIndex = 0, rotation = 90)
        )
        DesktopPdfEngine.saveReorderedPdf(pdf, reordered, specs)

        assertEquals(2, DesktopPdfEngine.getPageCount(reordered))
        PDDocument.load(reordered).use { doc ->
            assertEquals(0, doc.getPage(0).rotation)
            assertEquals(90, doc.getPage(1).rotation)
        }
    }

    @Test
    fun testImagesToPdfAndExtract() {
        // Create 2 test images
        val img1 = tempFolder.newFile("test1.png")
        val img2 = tempFolder.newFile("test2.png")

        val b1 = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply { color = Color.RED; fillRect(0, 0, 200, 200); dispose() }
        }
        val b2 = BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply { color = Color.BLUE; fillRect(0, 0, 200, 200); dispose() }
        }
        ImageIO.write(b1, "png", img1)
        ImageIO.write(b2, "png", img2)

        val compiledPdf = tempFolder.newFile("compiled_from_images.pdf")
        DesktopPdfEngine.imagesToPdf(listOf(img1, img2), compiledPdf)

        assertEquals(2, DesktopPdfEngine.getPageCount(compiledPdf))

        // Extract back
        val outDir = tempFolder.newFolder("extracted_images")
        val extracted = DesktopPdfEngine.extractPagesToImages(compiledPdf, outDir, format = "png")
        assertEquals(2, extracted.size)
        assertTrue(extracted[0].exists())
    }

    @Test
    fun testEncryptAndDecryptPdf() {
        val pdf = createTestPdf(pages = 1, text = "Secret Password Test")
        val encrypted = tempFolder.newFile("encrypted.pdf")
        val decrypted = tempFolder.newFile("decrypted.pdf")

        DesktopPdfEngine.encryptPdf(pdf, encrypted, "myPassword123")
        assertTrue(encrypted.exists() && encrypted.length() > 0)

        // Decrypt
        DesktopPdfEngine.decryptPdf(encrypted, decrypted, "myPassword123")
        val text = DesktopPdfEngine.extractText(decrypted)
        assertTrue(text.contains("Secret Password Test"))
    }
}
