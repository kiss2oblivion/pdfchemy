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

enum class NUpLayout(val cols: Int, val rows: Int, val pagesPerSheet: Int, val isLandscapeDefault: Boolean) {
    TWO_UP(cols = 1, rows = 2, pagesPerSheet = 2, isLandscapeDefault = false),
    FOUR_UP(cols = 2, rows = 2, pagesPerSheet = 4, isLandscapeDefault = true),
    SIX_UP(cols = 2, rows = 3, pagesPerSheet = 6, isLandscapeDefault = false),
    NINE_UP(cols = 3, rows = 3, pagesPerSheet = 9, isLandscapeDefault = true),
    SIXTEEN_UP(cols = 4, rows = 4, pagesPerSheet = 16, isLandscapeDefault = true)
}

enum class NUpOrder {
    HORIZONTAL, // Across then down
    VERTICAL    // Down then across
}

data class NUpConfig(
    val layout: NUpLayout = NUpLayout.FOUR_UP,
    val order: NUpOrder = NUpOrder.HORIZONTAL,
    val paperSize: TargetPaperSize = TargetPaperSize.A4,
    val drawBorders: Boolean = true,
    val marginPt: Float = 24f,
    val spacingPt: Float = 12f
)

object PdfNUpEngine {

    /**
     * Generates a multi-page N-Up handout grid PDF with lossless vector page embedding.
     */
    suspend fun generateNUpPdf(
        context: Context,
        sourcePdfUri: Uri,
        destPdfUri: Uri,
        config: NUpConfig,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var srcDoc: PDDocument? = null
        var outDoc: PDDocument? = null
        var inputStream: InputStream? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourcePdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            srcDoc = PDDocument.load(inputStream)
            val origPageCount = srcDoc.numberOfPages
            if (origPageCount == 0) {
                return@withContext Result.failure(IllegalStateException("PDF contains no pages"))
            }

            outDoc = PDDocument()
            val layerUtil = LayerUtility(outDoc)

            val cols = config.layout.cols
            val rows = config.layout.rows
            val perSheet = config.layout.pagesPerSheet
            val totalSheets = ceil(origPageCount.toDouble() / perSheet.toDouble()).toInt()

            // Sheet dimensions
            val sheetW = if (config.layout.isLandscapeDefault) config.paperSize.heightPts else config.paperSize.widthPts
            val sheetH = if (config.layout.isLandscapeDefault) config.paperSize.widthPts else config.paperSize.heightPts
            val sheetRect = PDRectangle(sheetW, sheetH)

            val availW = sheetW - (config.marginPt * 2) - (config.spacingPt * (cols - 1))
            val availH = sheetH - (config.marginPt * 2) - (config.spacingPt * (rows - 1))
            val cellW = availW / cols
            val cellH = availH / rows

            for (sheetIdx in 0 until totalSheets) {
                val sheetPage = PDPage(sheetRect)
                outDoc.addPage(sheetPage)

                PDPageContentStream(outDoc, sheetPage).use { cs ->
                    for (slotIdx in 0 until perSheet) {
                        val (col, row) = when (config.order) {
                            NUpOrder.HORIZONTAL -> {
                                val c = slotIdx % cols
                                val r = slotIdx / cols
                                c to (rows - 1 - r) // in PDF Y is bottom-to-top
                            }
                            NUpOrder.VERTICAL -> {
                                val r = slotIdx % rows
                                val c = slotIdx / rows
                                c to (rows - 1 - r)
                            }
                        }

                        val srcPageIdx = (sheetIdx * perSheet) + slotIdx
                        if (srcPageIdx < origPageCount) {
                            val cellX = config.marginPt + (col * (cellW + config.spacingPt))
                            val cellY = config.marginPt + (row * (cellH + config.spacingPt))

                            drawPageInCell(
                                srcDoc = srcDoc,
                                layerUtil = layerUtil,
                                cs = cs,
                                pageIndex = srcPageIdx,
                                cellX = cellX,
                                cellY = cellY,
                                cellW = cellW,
                                cellH = cellH,
                                drawBorder = config.drawBorders
                            )
                        }
                    }
                }

                onProgress(sheetIdx + 1, totalSheets)
            }

            val tempFile = File(context.cacheDir, "nup_${System.currentTimeMillis()}.pdf")
            outDoc.save(tempFile)
            outDoc.close()
            outDoc = null
            srcDoc.close()
            srcDoc = null

            context.contentResolver.openOutputStream(destPdfUri)?.use { out ->
                tempFile.inputStream().use { inp ->
                    inp.copyTo(out)
                }
            } ?: throw IllegalStateException("Cannot open destination PDF stream")

            tempFile.delete()

            val historyRepo = HistoryRepository(context)
            historyRepo.addHistoryItem(
                destPdfUri,
                FileUtils.getFileName(context, destPdfUri) ?: "nup_handout.pdf",
                "${config.layout.pagesPerSheet}-Up Grid Handout"
            )

            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("PdfNUpEngine: Error generating N-Up grid", e)
            Result.failure(e)
        } finally {
            try { outDoc?.close() } catch (_: Exception) {}
            try { srcDoc?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun drawPageInCell(
        srcDoc: PDDocument,
        layerUtil: LayerUtility,
        cs: PDPageContentStream,
        pageIndex: Int,
        cellX: Float,
        cellY: Float,
        cellW: Float,
        cellH: Float,
        drawBorder: Boolean
    ) {
        val srcPage = srcDoc.getPage(pageIndex)
        val srcBox = srcPage.cropBox ?: srcPage.mediaBox
        val srcW = srcBox.width
        val srcH = srcBox.height

        val formXObject = layerUtil.importPageAsForm(srcDoc, pageIndex)
        val scale = min(cellW / srcW, cellH / srcH)
        val drawW = srcW * scale
        val drawH = srcH * scale
        val drawX = cellX + ((cellW - drawW) / 2f)
        val drawY = cellY + ((cellH - drawH) / 2f)

        // Draw page form
        cs.saveGraphicsState()
        val matrix = Matrix.getTranslateInstance(drawX, drawY)
        matrix.scale(scale, scale)
        cs.transform(matrix)
        cs.drawForm(formXObject)
        cs.restoreGraphicsState()

        // Optional subtle slide frame border
        if (drawBorder) {
            cs.saveGraphicsState()
            cs.setStrokingColor(200, 205, 215)
            cs.setLineWidth(0.6f)
            cs.addRect(drawX, drawY, drawW, drawH)
            cs.stroke()
            cs.restoreGraphicsState()
        }
    }
}
