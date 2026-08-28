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

data class PdfFontInfo(
    val postscriptName: String,
    val familyName: String,
    val formatType: String,
    val isEmbedded: Boolean,
    val isSubset: Boolean,
    val encoding: String,
    val pageCountUsed: Int
)

object PdfFontInspectorEngine {

    /**
     * Inspects and catalogs all embedded and external fonts used across all pages of a PDF.
     */
    suspend fun inspectFonts(
        context: Context,
        pdfUri: Uri
    ): Result<List<PdfFontInfo>> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var document: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: throw IllegalStateException("Cannot open input PDF")

            document = PDDocument.load(inputStream)
            val fontMap = mutableMapOf<String, MutableFontEntry>()

            for ((pageIdx, page) in document.pages.withIndex()) {
                val res = page.resources ?: continue
                for (cosName in res.fontNames) {
                    val font = res.getFont(cosName) ?: continue
                    val rawName = font.name ?: cosName.name

                    val entry = fontMap.getOrPut(rawName) {
                        MutableFontEntry(
                            rawName = rawName,
                            font = font
                        )
                    }
                    entry.pages.add(pageIdx + 1)
                }
            }

            val resultList = fontMap.values.map { entry ->
                val raw = entry.rawName
                val isSubset = raw.length > 7 && raw[6] == '+'
                val cleanName = if (isSubset) raw.substring(7) else raw

                val fontSubtype = try {
                    entry.font.subType ?: entry.font.javaClass.simpleName
                } catch (_: Exception) {
                    entry.font.javaClass.simpleName
                }

                val formatType = when {
                    fontSubtype.contains("TrueType", ignoreCase = true) -> "TrueType"
                    fontSubtype.contains("Type1", ignoreCase = true) -> "Type 1"
                    fontSubtype.contains("Type0", ignoreCase = true) -> "Type 0 (Composite/CID)"
                    fontSubtype.contains("OpenType", ignoreCase = true) -> "OpenType"
                    else -> fontSubtype
                }

                val isEmbedded = try {
                    entry.font.isEmbedded || (entry.font.fontDescriptor?.fontFile != null ||
                            entry.font.fontDescriptor?.fontFile2 != null ||
                            entry.font.fontDescriptor?.fontFile3 != null)
                } catch (_: Exception) {
                    false
                }

                val encoding = try {
                    entry.font.cosObject.getNameAsString("Encoding") ?: "Standard"
                } catch (_: Exception) {
                    "Standard"
                }

                PdfFontInfo(
                    postscriptName = raw,
                    familyName = cleanName,
                    formatType = formatType,
                    isEmbedded = isEmbedded,
                    isSubset = isSubset,
                    encoding = encoding,
                    pageCountUsed = entry.pages.size
                )
            }.sortedBy { it.familyName }

            Result.success(resultList)
        } catch (e: Exception) {
            AppLogger.e("PdfFontInspectorEngine: Error inspecting fonts", e)
            Result.failure(e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    private class MutableFontEntry(
        val rawName: String,
        val font: PDFont,
        val pages: MutableSet<Int> = mutableSetOf()
    )
}
