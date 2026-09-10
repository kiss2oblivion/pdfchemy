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
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
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

    @Test
    fun testVanguardZeroTrustBlockOnExecutablePdf() = runBlocking {
        // 1. Construct synthetic test PDF with executable OpenAction and JavaScript triggers
        val infectedFile = File(context.cacheDir, "vanguard_infected_test.pdf")
        val cleanFile = File(context.cacheDir, "vanguard_clean_test.pdf")
        val sanitizedFile = File(context.cacheDir, "vanguard_sanitized_test.pdf")

        val docInfected = PDDocument()
        val page = PDPage(PDRectangle.A4)
        docInfected.addPage(page)
        // Add auto-executing OpenAction JavaScript hook
        val jsDict = com.tom_roush.pdfbox.cos.COSDictionary().apply {
            setName(COSName.S, "JavaScript")
            setString(COSName.getPDFName("JS"), "app.alert('Vanguard Test');")
        }
        docInfected.documentCatalog.cosObject.setItem(COSName.getPDFName("OpenAction"), jsDict)
        docInfected.save(infectedFile)
        docInfected.close()

        // 2. Construct clean standard PDF
        val docClean = PDDocument()
        docClean.addPage(PDPage(PDRectangle.A4))
        docClean.save(cleanFile)
        docClean.close()

        val infectedUri = Uri.fromFile(infectedFile)
        val cleanUri = Uri.fromFile(cleanFile)
        val sanitizedUri = Uri.fromFile(sanitizedFile)

        // 3. Verify Vanguard Zero-Trust detection
        val infectedHasThreats = PdfSanitizerEngine.hasExecutableThreats(context, infectedUri)
        assertTrue("Vanguard must detect executable triggers in infected PDF", infectedHasThreats)

        val cleanHasThreats = PdfSanitizerEngine.hasExecutableThreats(context, cleanUri)
        assertFalse("Clean document must not be flagged as a threat", cleanHasThreats)

        // 4. Verify sanitization neutralizes the threat completely
        val result = PdfSanitizerEngine.sanitizeDocument(context, infectedUri, sanitizedUri)
        assertTrue(result.isSuccess)
        val sanitizedHasThreats = PdfSanitizerEngine.hasExecutableThreats(context, sanitizedUri)
        assertFalse("Sanitized document must have all executable triggers purged", sanitizedHasThreats)
    }

    @Test
    fun testVanguardZeroTrustFailClosedOnEncryptedPdf() = runBlocking {
        val encryptedFile = File(context.cacheDir, "vanguard_encrypted_test.pdf")
        val doc = PDDocument()
        doc.addPage(PDPage(PDRectangle.A4))

        val accessPermission = AccessPermission()
        val protectionPolicy = StandardProtectionPolicy("ownerSecret", "userSecret", accessPermission).apply {
            encryptionKeyLength = 128
            permissions = accessPermission
        }
        doc.protect(protectionPolicy)
        doc.save(encryptedFile)
        doc.close()

        val encryptedUri = Uri.fromFile(encryptedFile)

        // 1. Audit report must report isClean = false, isEncrypted = true, threatsFound >= 1
        val auditReport = PdfSanitizerEngine.auditDocumentThreats(context, encryptedUri)
        assertTrue("Encrypted document must have isEncrypted=true", auditReport.isEncrypted)
        assertFalse("Encrypted document must not be flagged clean (fail-closed zero-trust)", auditReport.isClean)
        assertTrue("Encrypted document must have threatsFound >= 1", auditReport.threatsFound >= 1)

        // 2. Vanguard hasExecutableThreats must return true (fail-closed)
        val hasThreats = PdfSanitizerEngine.hasExecutableThreats(context, encryptedUri)
        assertTrue("Vanguard must fail-closed on password-protected PDF", hasThreats)

        // 3. checkVanguardThreat must classify as EncryptedCannotVerify
        val threatResult = PdfSanitizerEngine.checkVanguardThreat(context, encryptedUri)
        assertTrue(
            "Vanguard threat result must be EncryptedCannotVerify",
            threatResult is VanguardThreatResult.EncryptedCannotVerify
        )
        if (threatResult is VanguardThreatResult.EncryptedCannotVerify) {
            assertEquals(encryptedUri, threatResult.uri)
        }
    }

    @Test
    fun testVanguardCleanPdfWithMetadataPasses() = runBlocking {
        val metaFile = File(context.cacheDir, "vanguard_metadata_test.pdf")
        val doc = PDDocument()
        doc.addPage(PDPage(PDRectangle.A4))
        doc.documentInformation = com.tom_roush.pdfbox.pdmodel.PDDocumentInformation().apply {
            author = "Jane Doe"
            title = "Annual Financial Report"
            creator = "Microsoft Word"
        }
        doc.save(metaFile)
        doc.close()

        val metaUri = Uri.fromFile(metaFile)

        // Metadata alone is not an executable threat or parse failure
        val hasThreats = PdfSanitizerEngine.hasExecutableThreats(context, metaUri)
        assertFalse("Clean PDF with document metadata must not be flagged as an executable threat", hasThreats)

        val threatResult = PdfSanitizerEngine.checkVanguardThreat(context, metaUri)
        assertTrue("Vanguard threat result for standard document with metadata must be Clean", threatResult is VanguardThreatResult.Clean)
    }
}

