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
class PdfBookletEngineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)
    }

    @Test
    fun testComputeBookletPlan_4Pages() {
        val plan = PdfBookletEngine.computeBookletPlan(4)
        // 4 pages -> 1 sheet -> 2 sides (Front and Back)
        assertEquals(2, plan.size)

        // Sheet 1 Front: Left = Page 4 (idx 3), Right = Page 1 (idx 0)
        assertEquals(3, plan[0].leftPageOriginalIndex)
        assertEquals(0, plan[0].rightPageOriginalIndex)

        // Sheet 1 Back: Left = Page 2 (idx 1), Right = Page 3 (idx 2)
        assertEquals(1, plan[1].leftPageOriginalIndex)
        assertEquals(2, plan[1].rightPageOriginalIndex)
    }

    @Test
    fun testComputeBookletPlan_7Pages_PadsTo8() {
        val plan = PdfBookletEngine.computeBookletPlan(7)
        // 7 pages pads to 8 -> 2 sheets -> 4 sides
        assertEquals(4, plan.size)

        // Sheet 1 Front: Left = Page 8 (null / blank), Right = Page 1 (idx 0)
        assertNull(plan[0].leftPageOriginalIndex)
        assertEquals(0, plan[0].rightPageOriginalIndex)
    }

    @Test
    fun testGenerateBookletPdf() = runBlocking {
        val samplePdf = File(context.cacheDir, "sample_4pages.pdf")
        val doc = PDDocument()
        for (i in 0 until 4) {
            doc.addPage(PDPage())
        }
        doc.save(samplePdf)
        doc.close()

        val destFile = File(context.cacheDir, "booklet_out.pdf")
        val destUri = Uri.fromFile(destFile)

        val result = PdfBookletEngine.generateBookletPdf(
            context = context,
            sourcePdfUri = Uri.fromFile(samplePdf),
            destPdfUri = destUri,
            paperSize = TargetPaperSize.A4,
            drawFoldGuide = true
        )

        assertTrue(result.isSuccess)
        assertTrue(destFile.exists() && destFile.length() > 0)

        val outDoc = PDDocument.load(destFile)
        // 4 pages booklet -> 2 landscape sheets
        assertEquals(2, outDoc.numberOfPages)
        outDoc.close()
    }
}
