package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.pdfchemy.app.utils.FileUtils
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.LayerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.min

data class BookletSheetPlan(
    val sheetNumber: Int,
    val isFront: Boolean,
    val leftPageOriginalIndex: Int?,  // null means blank padding page
    val rightPageOriginalIndex: Int?  // null means blank padding page
)

object PdfBookletEngine {

    /**
     * Computes the saddle-stitch booklet imposition plan for a document with originalPageCount pages.
     */
    fun computeBookletPlan(originalPageCount: Int): List<BookletSheetPlan> {
        if (originalPageCount <= 0) return emptyList()

        val paddedTotal = ceil(originalPageCount / 4.0).toInt() * 4
        val sheetCount = paddedTotal / 4
        val plan = mutableListOf<BookletSheetPlan>()

        for (k in 0 until sheetCount) {
            // Sheet k Front (Outside fold)
            val frontLeft = paddedTotal - 1 - (2 * k)
            val frontRight = 2 * k
            plan.add(
                BookletSheetPlan(
                    sheetNumber = k + 1,
                    isFront = true,
                    leftPageOriginalIndex = if (frontLeft < originalPageCount) frontLeft else null,
                    rightPageOriginalIndex = if (frontRight < originalPageCount) frontRight else null
                )
            )

            // Sheet k Back (Inside fold)
            val backLeft = (2 * k) + 1
            val backRight = paddedTotal - 1 - ((2 * k) + 1)
            plan.add(
                BookletSheetPlan(
                    sheetNumber = k + 1,
                    isFront = false,
                    leftPageOriginalIndex = if (backLeft < originalPageCount) backLeft else null,
                    rightPageOriginalIndex = if (backRight < originalPageCount) backRight else null
                )
            )
        }

        return plan
    }

    /**
     * Generates a 2-up saddle-stitch imposed PDF ready for double-sided booklet printing
     * using 100% lossless vector page import (LayerUtility).
     */
    suspend fun generateBookletPdf(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        paperSize: TargetPaperSize = TargetPaperSize.A4,
        drawFoldGuide: Boolean = true,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var srcDoc: PDDocument? = null
        var outDoc: PDDocument? = null
        var inputStream: InputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open source PDF stream")

            srcDoc = PDDocument.load(inputStream)
            val origPageCount = srcDoc.numberOfPages
            if (origPageCount == 0) {
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            val plan = computeBookletPlan(origPageCount)
            outDoc = PDDocument()
            val layerUtil = LayerUtility(outDoc)

            // Landscape sheet dimensions (Width x Height)
            val sheetWidth = paperSize.heightPts
            val sheetHeight = paperSize.widthPts
            val sheetRect = PDRectangle(sheetWidth, sheetHeight)

            val margin = 20f
            val halfWidth = (sheetWidth - (margin * 2)) / 2f
            val contentHeight = sheetHeight - (margin * 2)

            for ((sideIdx, sidePlan) in plan.withIndex()) {
                val sheetPage = PDPage(sheetRect)
                outDoc.addPage(sheetPage)

                PDPageContentStream(outDoc, sheetPage).use { cs ->
                    // 1. Draw Left Page
                    if (sidePlan.leftPageOriginalIndex != null) {
                        drawVectorPageOnSheet(
                            srcDoc = srcDoc,
                            layerUtil = layerUtil,
                            cs = cs,
                            pageIndex = sidePlan.leftPageOriginalIndex,
                            targetX = margin,
                            targetY = margin,
                            availWidth = halfWidth - 8f,
                            availHeight = contentHeight
                        )
                    }

                    // 2. Draw Right Page
                    if (sidePlan.rightPageOriginalIndex != null) {
                        drawVectorPageOnSheet(
                            srcDoc = srcDoc,
                            layerUtil = layerUtil,
                            cs = cs,
                            pageIndex = sidePlan.rightPageOriginalIndex,
                            targetX = margin + halfWidth + 8f,
                            targetY = margin,
                            availWidth = halfWidth - 8f,
                            availHeight = contentHeight
                        )
                    }

                    // 3. Optional Center Fold Guideline
                    if (drawFoldGuide) {
                        cs.setStrokingColor(210, 210, 210)
                        cs.setLineWidth(0.5f)
                        cs.setLineDashPattern(floatArrayOf(4f, 4f), 0f)
                        cs.moveTo(sheetWidth / 2f, margin)
                        cs.lineTo(sheetWidth / 2f, sheetHeight - margin)
                        cs.stroke()
                    }
                }

                onProgress(sideIdx + 1, plan.size)
            }

            val tempFile = File(context.cacheDir, "booklet_${System.currentTimeMillis()}.pdf")
            outDoc.save(tempFile)
            outDoc.close()
            outDoc = null
            srcDoc.close()
            srcDoc = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination booklet stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "booklet.pdf",
                "Booklet Imposition (Saddle-Stitch)"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfBookletEngine: Error creating booklet", e)
            Result.failure(e)
        } finally {
            try { outDoc?.close() } catch (_: Exception) {}
            try { srcDoc?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun drawVectorPageOnSheet(
        srcDoc: PDDocument,
        layerUtil: LayerUtility,
        cs: PDPageContentStream,
        pageIndex: Int,
        targetX: Float,
        targetY: Float,
        availWidth: Float,
        availHeight: Float
    ) {
        val srcPage = srcDoc.getPage(pageIndex)
        val srcBox = srcPage.cropBox ?: srcPage.mediaBox
        val srcW = srcBox.width
        val srcH = srcBox.height

        val formXObject = layerUtil.importPageAsForm(srcDoc, pageIndex)
        val scale = min(availWidth / srcW, availHeight / srcH)
        val drawW = srcW * scale
        val drawH = srcH * scale
        val drawX = targetX + ((availWidth - drawW) / 2f)
        val drawY = targetY + ((availHeight - drawH) / 2f)

        cs.saveGraphicsState()
        val matrix = Matrix.getTranslateInstance(drawX, drawY)
        matrix.scale(scale, scale)
        cs.transform(matrix)
        cs.drawForm(formXObject)
        cs.restoreGraphicsState()
    }
}
