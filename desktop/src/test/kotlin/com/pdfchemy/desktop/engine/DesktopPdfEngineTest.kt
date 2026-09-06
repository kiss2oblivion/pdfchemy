package com.pdfchemy.desktop.engine

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField
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

    @Test
    fun testStampDocument_BusinessStamp() {
        val pdf = createTestPdf(pages = 2, text = "Official Contract")
        val stamped = tempFolder.newFile("stamped_contract.pdf")

        val stampImage = DesktopPdfEngine.createBusinessStamp(
            title = "CONFORM CU ORIGINALUL",
            subtext = "2026-09-06",
            colorHex = "#1E3A8A"
        )
        assertNotNull(stampImage)
        assertEquals(480, stampImage.width)
        assertEquals(180, stampImage.height)

        val success = DesktopPdfEngine.stampDocument(
            inputFile = pdf,
            outputFile = stamped,
            pageIndex = 0,
            stampImage = stampImage,
            xRatio = 0.6f,
            yRatio = 0.8f,
            widthRatio = 0.35f
        )
        assertTrue(success)
        assertTrue(stamped.exists() && stamped.length() > pdf.length())
        assertEquals(2, DesktopPdfEngine.getPageCount(stamped))
    }

    @Test
    fun testStampDocument_RenderedSignature() {
        val pdf = createTestPdf(pages = 1, text = "Sign Here")
        val signed = tempFolder.newFile("signed_doc.pdf")

        val strokes = listOf(
            listOf(java.awt.geom.Point2D.Float(10f, 10f), java.awt.geom.Point2D.Float(50f, 60f), java.awt.geom.Point2D.Float(100f, 20f)),
            listOf(java.awt.geom.Point2D.Float(120f, 80f), java.awt.geom.Point2D.Float(180f, 90f))
        )
        val sigImage = DesktopPdfEngine.renderStrokesToImage(
            strokes = strokes,
            canvasWidth = 300,
            canvasHeight = 120,
            colorHex = "#1E3A8A"
        )
        assertNotNull(sigImage)

        val success = DesktopPdfEngine.stampDocument(
            inputFile = pdf,
            outputFile = signed,
            pageIndex = 0,
            stampImage = sigImage,
            xRatio = 0.7f,
            yRatio = 0.85f,
            widthRatio = 0.30f
        )
        assertTrue(success)
        assertTrue(signed.exists() && signed.length() > pdf.length())
        assertEquals(1, DesktopPdfEngine.getPageCount(signed))
    }

    @Test
    fun testAddTextAnnotations_FormFill() {
        val pdf = createTestPdf(pages = 2, text = "Official Employment Contract")
        val annotated = tempFolder.newFile("annotated_doc.pdf")

        val items = listOf(
            TextAnnotationItem(text = "Jane Doe", xRatio = 0.2f, yRatio = 0.3f, fontSize = 14f, colorHex = "#18181B"),
            TextAnnotationItem(text = "✓", xRatio = 0.5f, yRatio = 0.4f, fontSize = 18f, colorHex = "#1E3A8A"),
            TextAnnotationItem(text = "✕", xRatio = 0.6f, yRatio = 0.4f, fontSize = 18f, colorHex = "#DC2626"),
            TextAnnotationItem(text = "2026-09-06", xRatio = 0.2f, yRatio = 0.7f, fontSize = 12f, colorHex = "#18181B")
        )

        val success = DesktopPdfEngine.addTextAnnotations(
            inputFile = pdf,
            outputFile = annotated,
            pageIndex = 0,
            items = items
        )
        assertTrue(success)
        assertTrue(annotated.exists() && annotated.length() > 0)
        assertEquals(2, DesktopPdfEngine.getPageCount(annotated))

        val text = DesktopPdfEngine.extractText(annotated)
        assertTrue("Extracted text should contain Jane Doe", text.contains("Jane Doe"))
        assertTrue("Extracted text should contain 2026-09-06", text.contains("2026-09-06"))
    }

    @Test
    fun testAddWatermark() {
        val pdf = createTestPdf(pages = 3, text = "Confidential Financial Data")
        val watermarked = tempFolder.newFile("watermarked_doc.pdf")

        val success = DesktopPdfEngine.addWatermark(
            inputFile = pdf,
            outputFile = watermarked,
            watermarkText = "CONFIDENTIAL",
            opacity = 0.25f,
            rotationDegrees = 45f,
            colorHex = "#DC2626"
        )
        assertTrue(success)
        assertTrue(watermarked.exists() && watermarked.length() > pdf.length())
        assertEquals(3, DesktopPdfEngine.getPageCount(watermarked))

        PDDocument.load(watermarked).use { doc ->
            val page = doc.getPage(0)
            val streamText = page.contentStreams.asSequence().joinToString("") { it.createInputStream().bufferedReader().readText() }
            assertTrue("Page content stream should contain CONFIDENTIAL", streamText.contains("CONFIDENTIAL"))
        }
    }

    @Test
    fun testAddPageNumbers() {
        val pdf = createTestPdf(pages = 3, text = "Project Proposal")
        val numbered = tempFolder.newFile("numbered_doc.pdf")

        val success = DesktopPdfEngine.addPageNumbers(
            inputFile = pdf,
            outputFile = numbered,
            formatPattern = "Page %1\$d of %2\$d",
            position = HeaderFooterPos.BOTTOM_CENTER,
            fontSize = 10f
        )
        assertTrue(success)
        assertTrue(numbered.exists() && numbered.length() > 0)
        assertEquals(3, DesktopPdfEngine.getPageCount(numbered))

        val text = DesktopPdfEngine.extractText(numbered)
        assertTrue("Extracted text should contain page number 1 of 3", text.contains("Page 1 of 3"))
        assertTrue("Extracted text should contain page number 2 of 3", text.contains("Page 2 of 3"))
        assertTrue("Extracted text should contain page number 3 of 3", text.contains("Page 3 of 3"))
    }

    @Test
    fun testAcroFormExtractionAndFlattening() {
        val file = tempFolder.newFile("test_acroform.pdf")
        val doc = PDDocument()
        val page = PDPage()
        doc.addPage(page)

        val acroForm = PDAcroForm(doc)
        doc.documentCatalog.acroForm = acroForm

        val res = org.apache.pdfbox.pdmodel.PDResources()
        res.put(org.apache.pdfbox.cos.COSName.getPDFName("Helv"), PDType1Font.HELVETICA)
        acroForm.defaultResources = res
        acroForm.defaultAppearance = "/Helv 12 Tf 0 g"

        val textField = PDTextField(acroForm)
        textField.partialName = "ApplicantName"
        textField.defaultAppearance = "/Helv 12 Tf 0 g"
        textField.value = "Original Name"
        acroForm.fields.add(textField)

        val checkBox = PDCheckBox(acroForm)
        checkBox.partialName = "TermsAccepted"
        checkBox.unCheck()
        acroForm.fields.add(checkBox)

        doc.save(file)
        doc.close()

        // 1. Detection
        assertTrue(DesktopPdfEngine.hasAcroForm(file))

        // 2. Extraction
        val fields = DesktopPdfEngine.extractAcroFields(file)
        assertEquals(2, fields.size)
        val nameField = fields.find { it.name == "ApplicantName" }
        assertNotNull(nameField)
        assertEquals(AcroFieldType.TEXT, nameField!!.type)
        assertEquals("Original Name", nameField.value)

        val checkField = fields.find { it.name == "TermsAccepted" }
        assertNotNull(checkField)
        assertEquals(AcroFieldType.CHECKBOX, checkField!!.type)

        // 3. Fill and Flatten
        val flattened = tempFolder.newFile("flattened_form.pdf")
        val filled = DesktopPdfEngine.fillAndFlattenAcroForm(
            inputFile = file,
            outputFile = flattened,
            fieldValues = mapOf("ApplicantName" to "John Doe", "TermsAccepted" to "Yes"),
            flatten = true
        )
        assertTrue(filled)
        assertTrue(flattened.exists() && flattened.length() > 0)

        // Once flattened, interactive form is removed
        assertFalse(DesktopPdfEngine.hasAcroForm(flattened))
    }

    @Test
    fun testCompareDocumentsIdentical() {
        val pdf1 = createTestPdf(pages = 2, text = "Legal Contract Revision 1")
        val pdf2 = createTestPdf(pages = 2, text = "Legal Contract Revision 1")

        val summary = DesktopPdfEngine.compareDocuments(pdf1, pdf2)
        assertTrue(summary.isEntirelyIdentical)
        assertEquals(2, summary.pagesA)
        assertEquals(2, summary.pagesB)
        assertEquals(0, summary.totalAddedLines)
        assertEquals(0, summary.totalRemovedLines)
        assertEquals(2, summary.pageDiffs.size)
        assertTrue(summary.pageDiffs[0].isIdentical)
        assertTrue(summary.pageDiffs[1].isIdentical)
    }

    @Test
    fun testCompareDocumentsDiffering() {
        val pdf1 = tempFolder.newFile("diff_a.pdf")
        val doc1 = PDDocument()
        val page1 = PDPage()
        doc1.addPage(page1)
        PDPageContentStream(doc1, page1).use { cs ->
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, 12f)
            cs.newLineAtOffset(50f, 700f)
            cs.showText("Clause 1: Scope of Work")
            cs.newLineAtOffset(0f, -20f)
            cs.showText("Clause 2: Payment in 30 Days")
            cs.endText()
        }
        doc1.save(pdf1)
        doc1.close()

        val pdf2 = tempFolder.newFile("diff_b.pdf")
        val doc2 = PDDocument()
        val page2 = PDPage()
        doc2.addPage(page2)
        PDPageContentStream(doc2, page2).use { cs ->
            cs.beginText()
            cs.setFont(PDType1Font.HELVETICA, 12f)
            cs.newLineAtOffset(50f, 700f)
            cs.showText("Clause 1: Scope of Work")
            cs.newLineAtOffset(0f, -20f)
            cs.showText("Clause 2: Payment in 14 Days (Accelerated)")
            cs.newLineAtOffset(0f, -20f)
            cs.showText("Clause 3: Confidentiality Warranty")
            cs.endText()
        }
        doc2.save(pdf2)
        doc2.close()

        val summary = DesktopPdfEngine.compareDocuments(pdf1, pdf2)
        assertFalse(summary.isEntirelyIdentical)
        assertEquals(1, summary.pageDiffs.size)
        assertFalse(summary.pageDiffs[0].isIdentical)
        assertTrue(summary.totalAddedLines > 0)
    }

    @Test
    fun testBatesStamping() {
        val pdf = createTestPdf(pages = 3, text = "Evidence Exhibit")
        val outPdf = tempFolder.newFile("bates_stamped.pdf")

        val config = DesktopBatesConfig(
            prefix = "CASE-2026-",
            suffix = "-EVID",
            startNumber = 101,
            digits = 5,
            position = DesktopBatesPosition.BOTTOM_RIGHT,
            fontSize = 10f
        )

        val success = DesktopPdfEngine.applyBatesStamping(pdf, outPdf, config)
        assertTrue(success)

        val extractedText = DesktopPdfEngine.extractText(outPdf)
        assertTrue(extractedText.contains("CASE-2026-00101-EVID"))
        assertTrue(extractedText.contains("CASE-2026-00102-EVID"))
        assertTrue(extractedText.contains("CASE-2026-00103-EVID"))
    }

    @Test
    fun testSanitizeDocument() {
        val pdf = tempFolder.newFile("threat_test.pdf")
        val doc = PDDocument()
        val page = PDPage()
        doc.addPage(page)
        doc.documentInformation.author = "Confidential Leaker"
        doc.documentInformation.title = "Internal Memo"
        doc.documentCatalog.cosObject.setString(org.apache.pdfbox.cos.COSName.getPDFName("JavaScript"), "app.alert('pwned');")
        doc.save(pdf)
        doc.close()

        val audit = DesktopPdfEngine.auditDocumentThreats(pdf)
        assertTrue(audit.threatsFound > 0)

        val cleanPdf = tempFolder.newFile("sanitized.pdf")
        val result = DesktopPdfEngine.sanitizeDocument(pdf, cleanPdf)
        assertTrue(result.threatsFound > 0)

        PDDocument.load(cleanPdf).use { cleanDoc ->
            assertNull(cleanDoc.documentInformation.author)
            assertNull(cleanDoc.documentInformation.title)
            assertNull(cleanDoc.documentCatalog.cosObject.getDictionaryObject(org.apache.pdfbox.cos.COSName.getPDFName("JavaScript")))
        }
    }

    @Test
    fun testRepairCorruptedPdf() {
        val validPdf = createTestPdf(pages = 2, text = "Salvageable Document Content")
        val validBytes = validPdf.readBytes()

        // Intentionally damage the file by prepending junk and corrupting trailer
        val corruptBytes = "GARBAGE_HEADER_CORRUPTION_DATA\n".toByteArray(Charsets.US_ASCII) + validBytes.dropLast(20).toByteArray()
        val damagedFile = tempFolder.newFile("damaged.pdf")
        damagedFile.writeBytes(corruptBytes)

        val repairedFile = tempFolder.newFile("repaired.pdf")
        val repairResult = DesktopPdfEngine.repairCorruptedPdf(damagedFile, repairedFile)

        assertTrue(repairResult.isSuccess)
        assertTrue(repairResult.pagesRecovered >= 1)
        assertTrue(repairResult.issuesRepaired.isNotEmpty())
        assertTrue(repairedFile.exists() && repairedFile.length() > 0)
    }

    @Test
    fun testConvertToPdfA() {
        val pdf = createTestPdf(pages = 2, text = "Archival Document")
        val outPdf = tempFolder.newFile("archival_pdfa.pdf")

        val success = DesktopPdfEngine.convertToPdfA(pdf, outPdf)
        assertTrue(success)

        PDDocument.load(outPdf).use { doc ->
            assertTrue(doc.documentCatalog.outputIntents.isNotEmpty())
            assertNotNull(doc.documentCatalog.metadata)
            assertNotNull(doc.documentCatalog.markInfo)
            assertTrue(doc.documentCatalog.markInfo.isMarked)
        }
    }

    @Test
    fun testCropMargins() {
        val pdf = createTestPdf(pages = 2, text = "Crop Test")
        val outPdf = tempFolder.newFile("cropped.pdf")

        val config = DesktopCropConfig(
            leftPt = 36f,
            topPt = 36f,
            rightPt = 36f,
            bottomPt = 36f,
            applyToAllPages = true
        )

        val success = DesktopPdfEngine.cropMargins(pdf, outPdf, config)
        assertTrue(success)

        PDDocument.load(outPdf).use { doc ->
            val page = doc.getPage(0)
            val mb = page.mediaBox
            val cb = page.cropBox
            assertEquals(mb.width - 72f, cb.width, 1.0f)
            assertEquals(mb.height - 72f, cb.height, 1.0f)
        }
    }
}

