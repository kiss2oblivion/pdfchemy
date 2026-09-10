package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class PlacedSignature(
    val pageIndex: Int,
    val xRatio: Float,
    val yRatio: Float,
    val widthRatio: Float,
    val heightRatio: Float,
    val bitmapBytes: ByteArray,
    val dateStamp: String? = null
)

object SignatureEngine {

    private const val SIGNATURES_DIR = "user_signatures"

    fun getSignaturesDir(context: Context): File {
        val dir = File(context.filesDir, SIGNATURES_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveSignature(context: Context, name: String, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = getSignaturesDir(context)
            val file = File(dir, "${name}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            AppLogger.e("Failed to save signature: ${e.message}", e)
            false
        }
    }

    suspend fun loadSignatures(context: Context): List<Pair<String, Bitmap>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<String, Bitmap>>()
        try {
            val dir = getSignaturesDir(context)
            val files = dir.listFiles { f -> f.extension.equals("png", ignoreCase = true) } ?: emptyArray()
            for (f in files) {
                val bmp = BitmapFactory.decodeFile(f.absolutePath)
                if (bmp != null) {
                    list.add(f.nameWithoutExtension to bmp)
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to load signatures: ${e.message}", e)
        }
        list
    }

    suspend fun deleteSignature(context: Context, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val dir = getSignaturesDir(context)
            val file = File(dir, "${name}.png")
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            AppLogger.e("Failed to delete signature: ${e.message}", e)
            false
        }
    }

    suspend fun applySignatures(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        signatures: List<PlacedSignature>
    ): Boolean = withContext(Dispatchers.IO) {
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                doc = PDDocument.load(inStream)
                if (doc == null) return@withContext false

                val totalPages = doc!!.numberOfPages
                val signaturesByPage = signatures.groupBy { it.pageIndex }

                for ((pageIdx, sigs) in signaturesByPage) {
                    if (pageIdx in 0 until totalPages) {
                        val page = doc!!.getPage(pageIdx)
                        val mediaBox = page.cropBox ?: page.mediaBox
                        val pageWidth = mediaBox.width
                        val pageHeight = mediaBox.height

                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            for (sig in sigs) {
                                val sigBmp = BitmapFactory.decodeByteArray(sig.bitmapBytes, 0, sig.bitmapBytes.size)
                                if (sigBmp != null) {
                                    val pdImage = LosslessFactory.createFromImage(doc, sigBmp)
                                    val stampX = sig.xRatio * pageWidth
                                    val stampW = (sig.widthRatio * pageWidth).coerceAtLeast(40f)
                                    val stampH = (sig.heightRatio * pageHeight).coerceAtLeast(20f)
                                    // In PDF, Y=0 is bottom
                                    val stampY = pageHeight - (sig.yRatio * pageHeight) - stampH

                                    cs.drawImage(pdImage, stampX, stampY, stampW, stampH)

                                    if (!sig.dateStamp.isNullOrBlank()) {
                                        cs.beginText()
                                        cs.setFont(PDType1Font.HELVETICA_BOLD, 9f)
                                        cs.setNonStrokingColor(0, 0, 0)
                                        cs.newLineAtOffset(stampX, stampY - 12f)
                                        cs.showText("Signed: ${sig.dateStamp}")
                                        cs.endText()
                                    }
                                }
                            }
                        }
                    }
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    doc!!.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e("Failed to apply signatures to PDF: ${e.message}", e)
            false
        } finally {
            doc?.close()
        }
    }

    fun createMarkBitmap(symbol: String, colorHex: String, size: Int = 120): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor(colorHex)
            textSize = size * 0.75f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val yOffset = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
        canvas.drawText(symbol, canvas.width / 2f, yOffset, paint)
        return bmp
    }

    fun createBusinessStampBitmap(
        title: String,
        subtext: String? = null,
        colorHex: String,
        width: Int = 360,
        height: Int = 120
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val colorInt = Color.parseColor(colorHex)

        val borderPaint = Paint().apply {
            isAntiAlias = true
            color = colorInt
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        val rect = RectF(8f, 8f, width - 8f, height - 8f)
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

        val innerPaint = Paint().apply {
            isAntiAlias = true
            color = colorInt
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        val innerRect = RectF(14f, 14f, width - 14f, height - 14f)
        canvas.drawRoundRect(innerRect, 12f, 12f, innerPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = colorInt
            textSize = if (subtext.isNullOrBlank()) 26f else 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }

        if (subtext.isNullOrBlank()) {
            val yOffset = (height / 2f) - ((titlePaint.descent() + titlePaint.ascent()) / 2f)
            canvas.drawText(title, width / 2f, yOffset, titlePaint)
        } else {
            canvas.drawText(title, width / 2f, 50f, titlePaint)

            val subPaint = Paint().apply {
                isAntiAlias = true
                color = colorInt
                textSize = 14f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            canvas.drawText(subtext, width / 2f, 85f, subPaint)
        }

        return bmp
    }

    /**
     * Applies a cryptographic PKI digital signature using a self-signed certificate.
     */
    suspend fun applyDigitalSignature(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        signerName: String,
        reason: String,
        location: String
    ): Boolean = withContext(Dispatchers.IO) {
        var sourceFile: File? = null
        var destFile: File? = null
        try {
            sourceFile = File(context.cacheDir, "temp_sign_in_${System.currentTimeMillis()}.pdf")
            destFile = File(context.cacheDir, "temp_sign_out_${System.currentTimeMillis()}.pdf")
            
            context.contentResolver.openInputStream(sourceUri)?.use { ins ->
                sourceFile.outputStream().use { fos ->
                    ins.copyTo(fos)
                }
            } ?: return@withContext false

            val subjectStr = "CN=$signerName, O=PDFchemy, C=US"
            val keyPairInfo = AndroidPdfCryptoSigner.generateSelfSignedCertificate(subjectStr)

            AndroidPdfCryptoSigner.signPdf(
                sourceFile = sourceFile,
                destFile = destFile,
                keyPairInfo = keyPairInfo,
                reason = reason,
                location = location
            )

            context.contentResolver.openOutputStream(destUri)?.use { outs ->
                destFile.inputStream().use { fis ->
                    fis.copyTo(outs)
                }
            } ?: return@withContext false
            
            true
        } catch (e: Exception) {
            AppLogger.e("Failed to apply digital signature: ${e.message}", e)
            false
        } finally {
            sourceFile?.delete()
            destFile?.delete()
        }
    }
}
