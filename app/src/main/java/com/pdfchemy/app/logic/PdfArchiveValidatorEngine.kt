package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

enum class CheckSeverity {
    ERROR,
    WARNING,
    INFO
}

data class ComplianceCheckItem(
    val rule: String,
    val isPassed: Boolean,
    val details: String,
    val severity: CheckSeverity = CheckSeverity.ERROR
)

data class PdfAReport(
    val pdfaVersionDetected: String,
    val complianceScore: Int, // 0 to 100
    val checks: List<ComplianceCheckItem>,
    val isCompliant: Boolean
)

object PdfArchiveValidatorEngine {

    /**
     * Audits a PDF against ISO 19005 (PDF/A) preflight rules.
     */
    suspend fun inspectPdfACompliance(
        context: Context,
        pdfUri: Uri
    ): Result<PdfAReport> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val checks = mutableListOf<ComplianceCheckItem>()

            // 1. Encryption Check (PDF/A prohibits encryption)
            val isEncrypted = document.isEncrypted
            checks.add(
                ComplianceCheckItem(
                    rule = "No Encryption / Passwords",
                    isPassed = !isEncrypted,
                    details = if (!isEncrypted) "Document is unencrypted (Standard compliant)" else "Encrypted PDFs are strictly forbidden in PDF/A archives",
                    severity = CheckSeverity.ERROR
                )
            )

            // 2. OutputIntents (ICC Color Profile)
            val outputIntents = document.documentCatalog.outputIntents
            val hasOutputIntent = !outputIntents.isNullOrEmpty()
            checks.add(
                ComplianceCheckItem(
                    rule = "Device-Independent Color Profile (OutputIntents)",
                    isPassed = hasOutputIntent,
                    details = if (hasOutputIntent) "Color OutputIntent profile found (${outputIntents.size} present)" else "Missing standard ICC OutputIntent profile for device-independent rendering",
                    severity = if (hasOutputIntent) CheckSeverity.INFO else CheckSeverity.WARNING
                )
            )

            // 3. XMP Metadata Schema
            val xmpMetadata = document.documentCatalog.metadata
            val hasXmp = xmpMetadata != null
            var pdfaVersion = "None (Standard PDF)"

            if (hasXmp) {
                try {
                    val xmlString = xmpMetadata!!.createInputStream().bufferedReader().use { it.readText() }
                    if (xmlString.contains("pdfaid:part=\"1\"") || xmlString.contains("<pdfaid:part>1</pdfaid:part>")) {
                        pdfaVersion = "PDF/A-1"
                    } else if (xmlString.contains("pdfaid:part=\"2\"") || xmlString.contains("<pdfaid:part>2</pdfaid:part>")) {
                        pdfaVersion = "PDF/A-2"
                    } else if (xmlString.contains("pdfaid:part=\"3\"") || xmlString.contains("<pdfaid:part>3</pdfaid:part>")) {
                        pdfaVersion = "PDF/A-3"
                    }
                } catch (_: Exception) {}
            }

            checks.add(
                ComplianceCheckItem(
                    rule = "Standardized XMP Metadata Packet",
                    isPassed = hasXmp,
                    details = if (hasXmp) "XMP Metadata stream is embedded" else "Missing embedded XMP metadata packet",
                    severity = CheckSeverity.ERROR
                )
            )

            // 4. Embedded Fonts Audit
            var totalFonts = 0
            var embeddedFonts = 0
            val checkedFonts = mutableSetOf<String>()

            for (page in document.pages) {
                val res = page.resources ?: continue
                for (fontName in res.fontNames) {
                    val font = res.getFont(fontName) ?: continue
                    val name = font.name ?: fontName.name
                    if (checkedFonts.add(name)) {
                        totalFonts++
                        if (isFontEmbedded(font)) {
                            embeddedFonts++
                        }
                    }
                }
            }

            val fontsFullyEmbedded = totalFonts == 0 || embeddedFonts == totalFonts
            checks.add(
                ComplianceCheckItem(
                    rule = "100% Embedded Font Subsets",
                    isPassed = fontsFullyEmbedded,
                    details = if (totalFonts == 0) "No text fonts required" else "$embeddedFonts / $totalFonts unique fonts fully embedded",
                    severity = CheckSeverity.ERROR
                )
            )

            // 5. Forbidden Executable Actions & Scripts
            val names = document.documentCatalog.names
            val hasJs = names?.javaScript != null || document.documentCatalog.actions != null
            checks.add(
                ComplianceCheckItem(
                    rule = "Absence of JavaScript & External Actions",
                    isPassed = !hasJs,
                    details = if (!hasJs) "No dangerous scripts or external triggers detected" else "Contains active scripts/actions incompatible with PDF/A preservation",
                    severity = CheckSeverity.ERROR
                )
            )

            // Calculate overall score
            val passCount = checks.count { it.isPassed }
            val score = ((passCount.toDouble() / checks.size.toDouble()) * 100).toInt()
            val isCompliant = checks.none { !it.isPassed && it.severity == CheckSeverity.ERROR }

            Result.success(
                PdfAReport(
                    pdfaVersionDetected = if (isCompliant && pdfaVersion == "None (Standard PDF)") "PDF/A-1b Ready" else pdfaVersion,
                    complianceScore = score,
                    checks = checks,
                    isCompliant = isCompliant
                )
            )
        } catch (e: Exception) {
            AppLogger.e("PdfArchiveValidatorEngine: Error validating PDF/A", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun isFontEmbedded(font: PDFont): Boolean {
        return try {
            font.isEmbedded || (font.fontDescriptor?.fontFile != null || font.fontDescriptor?.fontFile2 != null || font.fontDescriptor?.fontFile3 != null)
        } catch (_: Exception) {
            false
        }
    }
}
