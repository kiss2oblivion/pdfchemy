// =================================================================================================
// [FEATURE: PDF Compressor & Image Re-encoding Engine] (FEATURES_REGISTRY Android §1)
// Core document size reduction, DCT/JPEG & Flate optimization, grayscale, and target MB sizing.
// =================================================================================================

package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.tom_roush.pdfbox.cos.COSBase
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.pdfchemy.app.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import java.io.InputStream
import java.io.OutputStream

object PdfCompressor {

    /**
     * Compresses a PDF by reducing image quality and downsampling large images.
     */
    suspend fun compressPdf(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        quality: Float = 0.5f,
        useGrayscale: Boolean = false,
        useLossless: Boolean = false,
        stripMetadata: Boolean = false,
        targetMb: Float? = null
    ): Result<CompressionReport> = withContext(Dispatchers.IO) {
        if (targetMb == null) {
            return@withContext compressSinglePass(context, sourceUri, destUri, quality, useGrayscale, useLossless, stripMetadata)
        }

        val targetBytes = (targetMb * 1024 * 1024).toLong()
        val contentResolver = context.contentResolver
        val sourceSize = try {
            contentResolver.openFileDescriptor(sourceUri, "r")?.use { it.statSize } ?: -1L
        } catch (_: Exception) {
            -1L
        }

        // Optimization: If file is already smaller than target, do not degrade it
        if (sourceSize in 1..targetBytes) {
            try {
                contentResolver.openInputStream(sourceUri)?.use { input ->
                    contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                return@withContext Result.success(
                    CompressionReport(
                        originalSize = sourceSize,
                        imagesProcessed = 0,
                        hasSignatures = false,
                        targetMissed = false
                    )
                )
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
        }

        // Calibrate initial quality from required compression ratio
        val ratio = if (sourceSize > 0) targetBytes.toDouble() / sourceSize.toDouble() else 0.5
        val initialQuality = when {
            ratio < 0.20 -> 0.15f
            ratio < 0.40 -> 0.35f
            ratio < 0.65 -> 0.55f
            else -> 0.75f
        }

        val cacheDir = context.cacheDir
        val tempFile1 = java.io.File(cacheDir, "temp_compress_${System.currentTimeMillis()}_1.pdf")
        val tempUri1 = Uri.fromFile(tempFile1)

        val pass1Result = compressSinglePass(context, sourceUri, tempUri1, initialQuality, useGrayscale, useLossless, stripMetadata)
        if (pass1Result.isFailure) {
            tempFile1.delete()
            return@withContext pass1Result
        }

        val report1 = pass1Result.getOrThrow()
        val size1 = tempFile1.length()

        var bestTempFile = tempFile1
        var bestReport = report1

        // If pass 1 exceeds the target size, perform one targeted tightening pass
        if (size1 > targetBytes && initialQuality > 0.10f) {
            val scaleFactor = targetBytes.toFloat() / size1.toFloat()
            val tighterQuality = (initialQuality * scaleFactor * 0.90f).coerceIn(0.05f, initialQuality - 0.05f)

            val tempFile2 = java.io.File(cacheDir, "temp_compress_${System.currentTimeMillis()}_2.pdf")
            val tempUri2 = Uri.fromFile(tempFile2)

            val pass2Result = compressSinglePass(context, sourceUri, tempUri2, tighterQuality, useGrayscale, useLossless, stripMetadata)
            if (pass2Result.isSuccess) {
                val size2 = tempFile2.length()
                if (size2 < size1) {
                    tempFile1.delete()
                    bestTempFile = tempFile2
                    bestReport = pass2Result.getOrThrow()
                } else {
                    tempFile2.delete()
                }
            } else {
                tempFile2.delete()
            }
        }

        try {
            val success = bestTempFile.inputStream().use { input ->
                contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                    true
                } ?: false
            }

            if (!success) throw Exception("Failed to write to destination")

            val finalSize = bestTempFile.length()

            Result.success(bestReport.copy(targetMissed = finalSize > targetBytes))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            bestTempFile.delete()
            if (tempFile1.exists()) tempFile1.delete()
        }
    }

    private suspend fun compressSinglePass(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        quality: Float,
        useGrayscale: Boolean,
        useLossless: Boolean,
        stripMetadata: Boolean
    ): Result<CompressionReport> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val pfd = try { contentResolver.openFileDescriptor(sourceUri, "r") } catch (e: Exception) { null }
            val fileSize = pfd?.use { it.statSize } ?: -1L
            
            // DELEGATE TO JAIL: The host process must NEVER call PDDocument.load()
            val targetDpi = 150f
            
            com.pdfchemy.app.jail.PdfJailClient.compressPdf(
                context, sourceUri, destUri, targetDpi, quality, false
            )
            
            // The isolated process succeeded. Construct a minimal report.
            val report = CompressionReport(
                originalSize = fileSize,
                imagesProcessed = 1, // Jail doesn't return count currently
                hasSignatures = false, // Handled implicitly
                targetMissed = false
            )

            Result.success(report)
        } catch (e: Exception) {
            AppLogger.e("PdfCompressor: Jail execution failed", e)
            Result.failure(e)
        }
    }

    data class CompressionReport(
        val originalSize: Long,
        val imagesProcessed: Int,
        val hasSignatures: Boolean,
        val targetMissed: Boolean = false
    )

    /**
     * Analyzes a PDF to count pages, images, check signatures, and determine the case scenario.
     */
    suspend fun analyzePdf(
        context: Context,
        uri: Uri
    ): Result<PdfAnalysis> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        try {
            val contentResolver = context.contentResolver
            inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Failed to open file for analysis."))
            
            val doc = try {
                PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly())
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("Not a valid PDF file."))
            }
            document = doc

            val pageCount = doc.numberOfPages
            var imageCount = 0
            val hasSignatures = doc.signatureDictionaries.isNotEmpty()

            for (page in doc.pages) {
                val resources = page.resources ?: continue
                val processedNames = mutableSetOf<String>()

                for (name in resources.xObjectNames) {
                    val isImage = try { resources.isImageXObject(name) } catch (e: Exception) { false }

                    if (isImage && processedNames.add(name.name)) {
                        imageCount++
                    }
                }
            }

            val scenario = when {
                hasSignatures -> PdfScenario.SIGNED_OFFICIAL
                imageCount == 0 -> PdfScenario.TEXT_VECTOR
                imageCount >= pageCount -> PdfScenario.SCANNED_IMAGE_HEAVY
                else -> PdfScenario.MIXED
            }

            val recommendedQuality = when (scenario) {
                PdfScenario.SIGNED_OFFICIAL -> 0.75f // Better
                PdfScenario.TEXT_VECTOR -> 0.75f // Better
                PdfScenario.SCANNED_IMAGE_HEAVY -> 0.25f // High compression
                PdfScenario.MIXED -> 0.50f // Balanced
            }

            val reason = when (scenario) {
                PdfScenario.SIGNED_OFFICIAL -> "This document is digitally signed or official. High-quality compression is recommended to prevent invalidating signatures or losing document integrity."
                PdfScenario.TEXT_VECTOR -> "No images were detected. Standard downsampling won't reduce size, so we recommend maintaining full quality."
                PdfScenario.SCANNED_IMAGE_HEAVY -> "This file is image-heavy or scanned. Downsampling and compressing images will yield significant size savings with minimal text impact."
                PdfScenario.MIXED -> "This document contains a mix of text and images. Balanced compression is recommended to save space while keeping images readable."
            }

            Result.success(PdfAnalysis(
                pageCount = pageCount,
                imageCount = imageCount,
                hasSignatures = hasSignatures,
                scenario = scenario,
                recommendedQuality = recommendedQuality,
                recommendationReason = reason
            ))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            document?.close()
            inputStream?.close()
        }
    }

    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayscaleBitmap
    }
}

enum class PdfScenario(val displayName: String) {
    SCANNED_IMAGE_HEAVY("Scanned / Image-Heavy"),
    TEXT_VECTOR("Text & Vector"),
    SIGNED_OFFICIAL("Signed / Official"),
    MIXED("Mixed Content")
}

data class PdfAnalysis(
    val pageCount: Int,
    val imageCount: Int,
    val hasSignatures: Boolean,
    val scenario: PdfScenario,
    val recommendedQuality: Float,
    val recommendationReason: String
)

