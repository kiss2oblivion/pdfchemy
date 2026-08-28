package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PdfAttachmentEngineTest {

    private lateinit var context: Context
    private lateinit var samplePdfFile: File
    private lateinit var samplePdfUri: Uri
    private lateinit var sampleAttachFile: File
    private lateinit var sampleAttachUri: Uri

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)

        samplePdfFile = File(context.cacheDir, "sample_carrier.pdf")
        val doc = PDDocument()
        doc.addPage(PDPage())
        doc.save(samplePdfFile)
        doc.close()
        samplePdfUri = Uri.fromFile(samplePdfFile)

        sampleAttachFile = File(context.cacheDir, "invoice_data.xml")
        sampleAttachFile.writeText("<invoice id='12345'><total>49.99</total></invoice>")
        sampleAttachUri = Uri.fromFile(sampleAttachFile)
    }

    @Test
    fun testEmbedListAndExtractAttachment() = runBlocking {
        val attachedPdfFile = File(context.cacheDir, "with_attachment.pdf")
        val attachedPdfUri = Uri.fromFile(attachedPdfFile)

        // 1. Embed
        val embedRes = PdfAttachmentEngine.embedAttachment(
            context = context,
            sourcePdfUri = samplePdfUri,
            destPdfUri = attachedPdfUri,
            fileToEmbedUri = sampleAttachUri,
            customFileName = "factur-x.xml"
        )
        assertTrue(embedRes.isSuccess)

        // 2. List
        val listRes = PdfAttachmentEngine.listAttachments(context, attachedPdfUri)
        assertTrue(listRes.isSuccess)
        val list = listRes.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("factur-x.xml", list[0].name)

        // 3. Extract
        val extractedFile = File(context.cacheDir, "extracted_factur.xml")
        val extractedUri = Uri.fromFile(extractedFile)
        val extractRes = PdfAttachmentEngine.extractAttachment(
            context = context,
            pdfUri = attachedPdfUri,
            attachmentName = "factur-x.xml",
            destUri = extractedUri
        )
        assertTrue(extractRes.isSuccess)
        assertTrue(extractedFile.exists() && extractedFile.length() > 0)
        assertTrue(extractedFile.readText().contains("49.99"))
    }
}
