package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class SanitizerAuditReport(
    val threatsFound: Int = 0,
    val jsCount: Int = 0,
    val launchActionsCount: Int = 0,
    val attachmentCount: Int = 0,
    val hasMetadata: Boolean = false,
    val isClean: Boolean = true
)

data class SanitizerResult(
    val isSuccess: Boolean,
    val threatsRemoved: Int,
    val jsRemoved: Int,
    val actionsRemoved: Int,
    val metadataRemoved: Boolean,
    val attachmentsRemoved: Int
)

object PdfSanitizerEngine {

    /**
     * Audits an input PDF for hidden execution threats, tracking metadata, and payloads.
     */
    suspend fun auditDocumentThreats(
        context: Context,
        pdfUri: Uri
    ): SanitizerAuditReport = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var doc: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext SanitizerAuditReport()
            doc = PDDocument.load(inputStream)

            var jsCount = 0
            var actionCount = 0
            var attachmentCount = 0

            // 1. Catalog JavaScript triggers
            if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) jsCount++
            if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) jsCount++

            // 2. OpenAction and launch triggers
            if (doc.documentCatalog.openAction != null) actionCount++
            val catalogAa = doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("AA"))
            if (catalogAa is COSDictionary && catalogAa.size() > 0) actionCount++

            // 3. Embedded files
            if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("EmbeddedFiles")) != null) attachmentCount++

            // 4. Page-level interactive actions
            for (page in doc.pages) {
                if (page.cosObject.getDictionaryObject(COSName.getPDFName("AA")) != null) actionCount++
                for (annot in page.annotations) {
                    val action = annot.cosObject.getDictionaryObject(COSName.A)
                    if (action is COSDictionary) {
                        val s = action.getNameAsString(COSName.S)
                        if (s == "JavaScript") jsCount++
                        if (s in listOf("Launch", "SubmitForm", "ImportData", "URI", "Sound", "Movie")) actionCount++
                    }
                    if (annot.cosObject.getDictionaryObject(COSName.getPDFName("AA")) != null) actionCount++
                }
            }

            // 5. Metadata present
            val info = doc.documentInformation
            val hasMeta = info != null && (!info.author.isNullOrBlank() || !info.title.isNullOrBlank() || !info.creator.isNullOrBlank())
            val totalThreats = jsCount + actionCount + attachmentCount + (if (hasMeta) 1 else 0)

            SanitizerAuditReport(
                threatsFound = totalThreats,
                jsCount = jsCount,
                launchActionsCount = actionCount,
                attachmentCount = attachmentCount,
                hasMetadata = hasMeta,
                isClean = totalThreats == 0
            )
        } catch (e: Exception) {
            AppLogger.e("PdfSanitizerEngine: audit failed", e)
            SanitizerAuditReport()
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Vanguard Zero-Trust Pre-Flight Inspection:
     * Fast check to determine if the PDF contains executable scripts, launch actions,
     * auto-run hooks (/OpenAction, /AA), or embedded files.
     * Used by Vanguard Shield to block infected or active documents from opening.
     */
    suspend fun hasExecutableThreats(
        context: Context,
        pdfUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        val report = auditDocumentThreats(context, pdfUri)
        report.jsCount > 0 || report.launchActionsCount > 0 || report.attachmentCount > 0
    }

    /**
     * Purges and sanitizes malicious scripts, launch actions, tracking metadata, and attachments.
     */
    suspend fun sanitizeDocument(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        purgeJs: Boolean = true,
        purgeActions: Boolean = true,
        purgeMetadata: Boolean = true,
        purgeAttachments: Boolean = true
    ): SanitizerResult = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var inputStream: InputStream? = null
        var doc: PDDocument? = null

        try {
            inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext SanitizerResult(false, 0, 0, 0, false, 0)
            doc = PDDocument.load(inputStream)

            var jsPurged = 0
            var actionsPurged = 0
            var attachmentsPurged = 0

            if (purgeJs) {
                if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) {
                    doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("JavaScript"))
                    jsPurged++
                }
                if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) {
                    doc.documentCatalog.names?.cosObject?.removeItem(COSName.getPDFName("JavaScript"))
                    jsPurged++
                }
                if (doc.documentCatalog.openAction != null) {
                    doc.documentCatalog.openAction = null
                    actionsPurged++
                }
                doc.documentCatalog.actions = null
            }

            if (purgeActions) {
                if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("AA")) != null) {
                    doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("AA"))
                    actionsPurged++
                }
            }

            if (purgeAttachments) {
                if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("EmbeddedFiles")) != null) {
                    doc.documentCatalog.names?.cosObject?.removeItem(COSName.getPDFName("EmbeddedFiles"))
                    attachmentsPurged++
                }
            }

            if (purgeMetadata) {
                doc.documentInformation.title = null
                doc.documentInformation.author = null
                doc.documentInformation.subject = null
                doc.documentInformation.keywords = null
                doc.documentInformation.creator = null
                doc.documentInformation.producer = "PDFchemy (Scrubbed & Sanitized)"
                doc.documentInformation.creationDate = null
                doc.documentInformation.modificationDate = null
                doc.documentCatalog.metadata = null
                doc.document.trailer.removeItem(COSName.ID)
                doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("PieceInfo"))
            }

            for (page in doc.pages) {
                page.cosObject.removeItem(COSName.getPDFName("PieceInfo"))
                page.cosObject.removeItem(COSName.getPDFName("AA"))

                if (purgeActions || purgeJs) {
                    for (annot in page.annotations) {
                        if (purgeActions && annot.cosObject.getDictionaryObject(COSName.getPDFName("AA")) != null) {
                            annot.cosObject.removeItem(COSName.getPDFName("AA"))
                            actionsPurged++
                        }
                        val action = annot.cosObject.getDictionaryObject(COSName.A)
                        if (action is COSDictionary) {
                            val s = action.getNameAsString(COSName.S)
                            if (purgeJs && s == "JavaScript") {
                                annot.cosObject.removeItem(COSName.A)
                                jsPurged++
                            }
                            if (purgeActions && s in listOf("Launch", "SubmitForm", "ImportData", "URI", "Sound", "Movie")) {
                                annot.cosObject.removeItem(COSName.A)
                                actionsPurged++
                            }
                        }
                    }
                }
            }

            context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                doc.save(outStream)
            }

            SanitizerResult(
                isSuccess = true,
                threatsRemoved = jsPurged + actionsPurged + attachmentsPurged + (if (purgeMetadata) 1 else 0),
                jsRemoved = jsPurged,
                actionsRemoved = actionsPurged,
                metadataRemoved = purgeMetadata,
                attachmentsRemoved = attachmentsPurged
            )
        } catch (e: Exception) {
            AppLogger.e("PdfSanitizerEngine: sanitize failed", e)
            SanitizerResult(false, 0, 0, 0, false, 0)
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }
    }
}
