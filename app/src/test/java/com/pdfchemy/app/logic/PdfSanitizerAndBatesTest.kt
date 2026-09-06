package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PdfSanitizerAndBatesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)
    }

    @Test
    fun testBatesStampingAndroid() = runBlocking {
        val inFile = File(context.cacheDir, "bates_in.pdf")
        val outFile = File(context.cacheDir, "bates_out.pdf")

        val doc = PDDocument()
        doc.addPage(PDPage(PDRectangle.A4))
        doc.addPage(PDPage(PDRectangle.A4))
        doc.save(inFile)
        doc.close()

        val inUri = Uri.fromFile(inFile)
        val outUri = Uri.fromFile(outFile)

        val options = BatesOptions(
            prefix = "EXHIBIT-",
            suffix = "-CONF",
            startNumber = 10,
            digits = 4,
            position = NumberPosition.BOTTOM_RIGHT
        )

        val success = PdfStampAndNumberEngine.applyBatesStamping(context, inUri, outUri, options)
        assertTrue(success)
        assertTrue(outFile.exists() && outFile.length() > 0)

        PDDocument.load(outFile).use { loaded ->
            assertEquals(2, loaded.numberOfPages)
        }
    }

    @Test
    fun testSanitizeDocumentAndroid() = runBlocking {
        val inFile = File(context.cacheDir, "sanitize_in.pdf")
        val outFile = File(context.cacheDir, "sanitize_out.pdf")

        val doc = PDDocument()
        val page = PDPage(PDRectangle.A4)
        doc.addPage(page)
        doc.documentInformation.author = "Threat Actor"
        doc.documentInformation.title = "Confidential Leak"
        doc.documentCatalog.cosObject.setString(COSName.getPDFName("JavaScript"), "app.alert(1);")
        doc.save(inFile)
        doc.close()

        val inUri = Uri.fromFile(inFile)
        val outUri = Uri.fromFile(outFile)

        val audit = PdfSanitizerEngine.auditDocumentThreats(context, inUri)
        assertTrue("Threats should be detected", audit.threatsFound > 0)
        assertTrue(audit.jsCount > 0)
        assertFalse(audit.isClean)

        val result = PdfSanitizerEngine.sanitizeDocument(context, inUri, outUri)
        assertTrue(result.isSuccess)
        assertTrue(result.threatsRemoved > 0)

        PDDocument.load(outFile).use { cleanDoc ->
            assertNull(cleanDoc.documentInformation.author)
            assertNull(cleanDoc.documentInformation.title)
            assertNull(cleanDoc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("JavaScript")))
        }
    }

    @Test
    fun testConvertToPdfAAndroid() = runBlocking {
        val inFile = File(context.cacheDir, "pdfa_in.pdf")
        val outFile = File(context.cacheDir, "pdfa_out.pdf")

        val doc = PDDocument()
        doc.addPage(PDPage(PDRectangle.A4))
        doc.save(inFile)
        doc.close()

        val inUri = Uri.fromFile(inFile)
        val outUri = Uri.fromFile(outFile)

        val success = PdfArchiveValidatorEngine.convertToPdfA(context, inUri, outUri)
        assertTrue(success)
        assertTrue(outFile.exists() && outFile.length() > 0)

        PDDocument.load(outFile).use { pdfaDoc ->
            assertNotNull(pdfaDoc.documentCatalog.metadata)
            assertNotNull(pdfaDoc.documentCatalog.markInfo)
            assertTrue(pdfaDoc.documentCatalog.markInfo.isMarked)
        }
    }
}
