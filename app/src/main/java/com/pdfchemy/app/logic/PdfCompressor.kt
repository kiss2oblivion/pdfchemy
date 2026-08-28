package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import com.pdfchemy.app.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        var minQuality = 0.05f
        var maxQuality = 1.0f
        
        val cacheDir = context.cacheDir
        var bestTempFile: java.io.File? = null
        var bestReport: CompressionReport? = null

        for (i in 0..4) {
            val currentQuality = (minQuality + maxQuality) / 2
            val tempFile = java.io.File(cacheDir, "temp_compress_${System.currentTimeMillis()}_$i.pdf")
            val tempUri = Uri.fromFile(tempFile)
            
            val result = compressSinglePass(context, sourceUri, tempUri, currentQuality, useGrayscale, useLossless, stripMetadata)
            
            if (result.isFailure) {
                bestTempFile?.delete()
                tempFile.delete()
                return@withContext result
            }
            
            val report = result.getOrThrow()
            val size = tempFile.length()
            
            if (size <= targetBytes) {
                minQuality = currentQuality // try higher quality
            } else {
                maxQuality = currentQuality // need lower quality
            }

            if (bestTempFile == null) {
                bestTempFile = tempFile
                bestReport = report
            } else {
                val bTempFile = bestTempFile ?: return@withContext Result.failure(Exception("Compression failed to produce a valid file."))
                val bestSize = bTempFile.length()
                if (size <= targetBytes && bestSize <= targetBytes && size > bestSize) {
                    bestTempFile?.delete()
                    bestTempFile = tempFile
                    bestReport = report
                } else if (size <= targetBytes && bestSize > targetBytes) {
                    bestTempFile?.delete()
                    bestTempFile = tempFile
                    bestReport = report
                } else if (size > targetBytes && bestSize > targetBytes && size < bestSize) {
                    bestTempFile?.delete()
                    bestTempFile = tempFile
                    bestReport = report
                } else {
                    tempFile.delete()
                }
            }
        }

        try {
            val bTempFile = bestTempFile ?: return@withContext Result.failure(Exception("Failed to create temporary compressed file"))
            val bReport = bestReport ?: return@withContext Result.failure(Exception("Compression report missing"))
            
            val success = bTempFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(destUri)?.use { output ->
                    input.copyTo(output)
                    true
                } ?: false
            }

            if (!success) throw Exception("Failed to write to destination")

            val finalSize = bTempFile.length()
            bTempFile.delete()
            
            return@withContext Result.success(bReport.copy(targetMissed = finalSize > targetBytes))
        } catch (e: Exception) {
            bestTempFile?.delete()
            return@withContext Result.failure(e)
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
        var document: PDDocument? = null
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val contentResolver = context.contentResolver
            
            val pfd = try { contentResolver.openFileDescriptor(sourceUri, "r") } catch (e: Exception) { null }
            val fileSize = pfd?.use { it.statSize } ?: -1L
            
            if (fileSize == 0L) {
                return@withContext Result.failure(Exception("File is empty (0 bytes)"))
            }

            inputStream = contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("The selected file is no longer available."))
            
            val doc = try {
                PDDocument.load(inputStream)
            } catch (e: Exception) {
                return@withContext Result.failure(Exception("This file cannot be opened or is not a valid PDF."))
            }
            document = doc

            if (doc.isEncrypted) {
                return@withContext Result.failure(Exception("This PDF is password-protected and cannot be processed."))
            }

            val hasSignatures = doc.signatureDictionaries.isNotEmpty()

            if (stripMetadata) {
                doc.documentInformation = com.tom_roush.pdfbox.pdmodel.PDDocumentInformation()
                doc.documentCatalog.metadata = null
            }
            
            var imagesProcessed = 0
            var imagesSkipped = 0
            val maxDimension = if (quality < 0.2f) 800f else if (quality < 0.4f) 1200f else if (quality < 0.6f) 1800f else 3000f

            for (page in doc.pages) {
                val resources = page.resources ?: continue
                val processedNames = mutableSetOf<String>()

                for (name in resources.xObjectNames) {
                    val xObject = try { resources.getXObject(name) } catch (e: Throwable) { null }

                    if (xObject is PDImageXObject && !processedNames.contains(name.name)) {
                        var originalBitmap: Bitmap? = null
                        var scaledBitmap: Bitmap? = null
                        var grayscaleBitmap: Bitmap? = null
                        try {
                            originalBitmap = try {
                                xObject.image
                            } catch (oom: OutOfMemoryError) {
                                AppLogger.e("PdfCompressor: OOM while decoding image '${name.name}', keeping original", oom)
                                null
                            } catch (e: Throwable) {
                                AppLogger.w("PdfCompressor: Failed to decode embedded image '${name.name}' (${e.message}), keeping original", e)
                                null
                            }

                            if (originalBitmap == null || originalBitmap.width <= 0 || originalBitmap.height <= 0) {
                                imagesSkipped++
                                processedNames.add(name.name)
                                continue
                            }

                            var bitmap = originalBitmap
                            
                            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                                val scale = Math.min(maxDimension / bitmap.width, maxDimension / bitmap.height)
                                val matrix = Matrix()
                                matrix.postScale(scale, scale)
                                scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                bitmap = scaledBitmap
                            }

                            if (useGrayscale) {
                                grayscaleBitmap = convertToGrayscale(bitmap)
                                bitmap = grayscaleBitmap
                            }

                            val compressedImage = if (useLossless) {
                                LosslessFactory.createFromImage(doc, bitmap)
                            } else {
                                JPEGFactory.createFromImage(doc, bitmap, quality)
                            }

                            resources.put(name, compressedImage)
                            processedNames.add(name.name)
                            imagesProcessed++
                        } catch (oom: OutOfMemoryError) {
                            AppLogger.e("PdfCompressor: OutOfMemoryError while compressing image '${name.name}', preserving original", oom)
                            imagesSkipped++
                            processedNames.add(name.name)
                        } catch (e: Throwable) {
                            AppLogger.w("PdfCompressor: Non-fatal error while re-encoding image '${name.name}': ${e.message}, preserving original", e)
                            imagesSkipped++
                            processedNames.add(name.name)
                        } finally {
                            try { grayscaleBitmap?.recycle() } catch (_: Throwable) {}
                            try { if (scaledBitmap != null && scaledBitmap !== originalBitmap) scaledBitmap.recycle() } catch (_: Throwable) {}
                            try { originalBitmap?.recycle() } catch (_: Throwable) {}
                        }
                    }
                }
            }

            outputStream = contentResolver.openOutputStream(destUri)
                ?: return@withContext Result.failure(Exception("Cannot write to the chosen destination. Please verify storage permissions and available space."))
            
            doc.save(outputStream)
            
            Result.success(CompressionReport(
                originalSize = fileSize,
                imagesProcessed = imagesProcessed,
                hasSignatures = hasSignatures
            ))

        } catch (oom: OutOfMemoryError) {
            AppLogger.e("PdfCompressor: Out of memory during PDF compression", oom)
            Result.failure(Exception("The PDF contains large high-resolution graphics that exceeded device memory. Try choosing a higher compression preset or enabling Grayscale mode.", oom))
        } catch (e: Throwable) {
            AppLogger.e("PdfCompressor: Error during PDF compression", e)
            val friendlyMsg = when {
                e.message?.contains("password", ignoreCase = true) == true || e.message?.contains("encrypt", ignoreCase = true) == true ->
                    "This PDF is password-protected or encrypted. Please unlock it in PDF Security before compressing."
                e.message?.contains("bitmap", ignoreCase = true) == true || e.message?.contains("memory", ignoreCase = true) == true || e.message?.contains("dimension", ignoreCase = true) == true ->
                    "Unable to re-encode one or more high-resolution graphics. Try using Grayscale mode or a lighter compression preset."
                e.message?.contains("cannot open", ignoreCase = true) == true || e.message?.contains("no longer available", ignoreCase = true) == true ->
                    "The selected file could not be read from storage. Please select the file again."
                else ->
                    e.localizedMessage?.takeIf { it.isNotBlank() && it.length > 5 } ?: "An unexpected error occurred while processing the PDF structure. You can try running 'Repair PDF' first."
            }
            Result.failure(Exception(friendlyMsg, e))
        } finally {
            try { document?.close() } catch (_: Throwable) {}
            try { inputStream?.close() } catch (_: Throwable) {}
            try { outputStream?.close() } catch (_: Throwable) {}
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
                PDDocument.load(inputStream)
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
                    val xObject = try { resources.getXObject(name) } catch (e: Exception) { null }

                    if (xObject is PDImageXObject && !processedNames.contains(name.name)) {
                        imageCount++
                        processedNames.add(name.name)
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

