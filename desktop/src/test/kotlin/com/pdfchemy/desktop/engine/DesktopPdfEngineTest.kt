package com.pdfchemy.desktop.engine

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
