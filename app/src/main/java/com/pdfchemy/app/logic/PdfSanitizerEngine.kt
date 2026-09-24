package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

data class SanitizerAuditReport(
    val threatsFound: Int = 0,
    val jsCount: Int = 0,
    val launchActionsCount: Int = 0,
    val attachmentCount: Int = 0,
    val uriCount: Int = 0,
    val hasMetadata: Boolean = false,
    val isClean: Boolean = true,
    val isEncrypted: Boolean = false,
    val parseFailed: Boolean = false
)

sealed class VanguardThreatResult {
    object Clean : VanguardThreatResult()
    data class ExecutableThreat(val report: SanitizerAuditReport) : VanguardThreatResult()
    data class EncryptedCannotVerify(val uri: Uri) : VanguardThreatResult()
    data class ParseFailed(val uri: Uri) : VanguardThreatResult()
}

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
        val inputStream = context.contentResolver.openInputStream(pdfUri)
            ?: return@withContext SanitizerAuditReport(threatsFound = 1, isClean = false, parseFailed = true)
        auditDocumentThreats(context, inputStream)
    }

    suspend fun auditDocumentThreats(
        context: Context,
        inputStream: InputStream
    ): SanitizerAuditReport = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var doc: PDDocument? = null

        try {
            try {
                // Limit memory to 10MB and scratch disk usage to 250MB to prevent Zip/Object bombs
                val memSettings = com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMixed(10 * 1024 * 1024, 250 * 1024 * 1024)
                doc = PDDocument.load(inputStream, "", memSettings)
            } catch (e: InvalidPasswordException) {
                AppLogger.w("PdfSanitizerEngine: Document is encrypted / password protected")
                return@withContext SanitizerAuditReport(
                    threatsFound = 1,
                    isClean = false,
                    isEncrypted = true
                )
            }

            if (doc.isEncrypted) {
                AppLogger.w("PdfSanitizerEngine: Document has encryption dictionary (cannot pre-flight verify)")
                return@withContext SanitizerAuditReport(
                    threatsFound = 1,
                    isClean = false,
                    isEncrypted = true
                )
            }

            var jsCount = 0
            var actionCount = 0
            var attachmentCount = 0
            var uriCount = 0

            // 1. Catalog JavaScript triggers
            if (doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) jsCount++
            if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("JavaScript")) != null) jsCount++

            // 2. OpenAction and launch triggers
            val processAction: (COSDictionary) -> Unit = { actionDict ->
                val s = actionDict.getNameAsString(COSName.S)
                if (s == "JavaScript") jsCount++
                else if (s in listOf("Launch", "SubmitForm", "ImportData", "Sound", "Movie", "GoToE", "GoToR")) actionCount++
                else if (s == "URI") uriCount++
            }

            walkActions(doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("OpenAction")), mutableSetOf(), processAction)
            scanAaDictionary(doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("AA")), processAction)

            val acroForm = doc.documentCatalog.acroForm
            if (acroForm != null) {
                scanAaDictionary(acroForm.cosObject.getDictionaryObject(COSName.getPDFName("AA")), processAction)
                for (field in acroForm.fieldTree) {
                    scanAaDictionary(field.cosObject.getDictionaryObject(COSName.getPDFName("AA")), processAction)
                }
            }

            // 3. Embedded files
            if (doc.documentCatalog.names?.cosObject?.getDictionaryObject(COSName.getPDFName("EmbeddedFiles")) != null) attachmentCount++

            // 4. Page-level interactive actions
            for (page in doc.pages) {
                scanAaDictionary(page.cosObject.getDictionaryObject(COSName.getPDFName("AA")), processAction)
                for (annot in page.annotations) {
                    walkActions(annot.cosObject.getDictionaryObject(COSName.A), mutableSetOf(), processAction)
                    scanAaDictionary(annot.cosObject.getDictionaryObject(COSName.getPDFName("AA")), processAction)
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
                uriCount = uriCount,
                hasMetadata = hasMeta,
                isClean = totalThreats == 0
            )
        } catch (e: Exception) {
            val isEnc = e is InvalidPasswordException || e.cause is InvalidPasswordException
            AppLogger.e("PdfSanitizerEngine: audit failed (isEncrypted=$isEnc)", e)
            SanitizerAuditReport(
                threatsFound = 1,
                isClean = false,
                isEncrypted = isEnc,
                parseFailed = !isEnc
            )
        } finally {
            try { doc?.close() } catch (_: Exception) {}
            try { inputStream.close() } catch (_: Exception) {}
        }
    }

    /**
     * Vanguard Zero-Trust Pre-Flight Inspection:
     * Fast check to determine if the PDF contains executable scripts, launch actions,
     * auto-run hooks (/OpenAction, /AA), embedded files, or unverified encryption.
     * Used by Vanguard Shield to block infected or unverified documents from opening.
     */
    suspend fun hasExecutableThreats(
        context: Context,
        pdfUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        val report = auditDocumentThreats(context, pdfUri)
        report.jsCount > 0 || report.launchActionsCount > 0 || report.attachmentCount > 0 || report.isEncrypted || report.parseFailed
    }

    /**
     * Categorizes document threat status for fine-grained Vanguard dialog handling.
     */
    suspend fun checkVanguardThreat(
        context: Context,
        pdfUri: Uri
    ): VanguardThreatResult = withContext(Dispatchers.IO) {
        val report = auditDocumentThreats(context, pdfUri)
        if (report.isEncrypted) {
            return@withContext VanguardThreatResult.EncryptedCannotVerify(pdfUri)
        }
        if (report.jsCount > 0 || report.launchActionsCount > 0 || report.attachmentCount > 0) {
            return@withContext VanguardThreatResult.ExecutableThreat(report)
        }
        if (report.parseFailed) {
            return@withContext VanguardThreatResult.ParseFailed(pdfUri)
        }
        VanguardThreatResult.Clean
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
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: return@withContext SanitizerResult(false, 0, 0, 0, false, 0)
        val outStream = context.contentResolver.openOutputStream(destUri)
            ?: return@withContext SanitizerResult(false, 0, 0, 0, false, 0)
        sanitizeDocument(context, inputStream, outStream, purgeJs, purgeActions, purgeMetadata, purgeAttachments)
    }

    suspend fun sanitizeDocument(
        context: Context,
        inputStream: InputStream,
        outStream: java.io.OutputStream,
        purgeJs: Boolean = true,
        purgeActions: Boolean = true,
        purgeMetadata: Boolean = true,
        purgeAttachments: Boolean = true
    ): SanitizerResult = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var doc: PDDocument? = null

        try {
            val memSettings = com.tom_roush.pdfbox.io.MemoryUsageSetting.setupMixed(10 * 1024 * 1024, 250 * 1024 * 1024)
            doc = PDDocument.load(inputStream, memSettings)

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
            }

            if (purgeJs || purgeActions) {
                val openAction = doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("OpenAction"))
                if (hasMaliciousAction(openAction, purgeJs, purgeActions)) {
                    doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("OpenAction"))
                    actionsPurged++
                }
                
                if (hasMaliciousAa(doc.documentCatalog.cosObject.getDictionaryObject(COSName.getPDFName("AA")), purgeJs, purgeActions)) {
                    doc.documentCatalog.cosObject.removeItem(COSName.getPDFName("AA"))
                    actionsPurged++
                }
                
                val acroForm = doc.documentCatalog.acroForm
                if (acroForm != null) {
                    if (hasMaliciousAa(acroForm.cosObject.getDictionaryObject(COSName.getPDFName("AA")), purgeJs, purgeActions)) {
                        acroForm.cosObject.removeItem(COSName.getPDFName("AA"))
                        actionsPurged++
                    }
                    for (field in acroForm.fieldTree) {
                        if (hasMaliciousAa(field.cosObject.getDictionaryObject(COSName.getPDFName("AA")), purgeJs, purgeActions)) {
                            field.cosObject.removeItem(COSName.getPDFName("AA"))
                            actionsPurged++
                        }
                    }
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

                if (purgeActions || purgeJs) {
                    if (hasMaliciousAa(page.cosObject.getDictionaryObject(COSName.getPDFName("AA")), purgeJs, purgeActions)) {
                        page.cosObject.removeItem(COSName.getPDFName("AA"))
                        actionsPurged++
                    }
                    
                    for (annot in page.annotations) {
                        if (hasMaliciousAa(annot.cosObject.getDictionaryObject(COSName.getPDFName("AA")), purgeJs, purgeActions)) {
                            annot.cosObject.removeItem(COSName.getPDFName("AA"))
                            actionsPurged++
                        }
                        
                        val action = annot.cosObject.getDictionaryObject(COSName.A)
                        if (hasMaliciousAction(action, purgeJs, purgeActions)) {
                            annot.cosObject.removeItem(COSName.A)
                            actionsPurged++
                        }
                    }
                }
            }

            doc.save(outStream)

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
            try { inputStream.close() } catch (_: Exception) {}
            try { outStream.close() } catch (_: Exception) {}
        }
    }

    private fun walkActions(
        actionObj: com.tom_roush.pdfbox.cos.COSBase?,
        visited: MutableSet<com.tom_roush.pdfbox.cos.COSBase> = mutableSetOf(),
        onAction: (COSDictionary) -> Unit
    ) {
        if (actionObj == null) return
        if (!visited.add(actionObj)) return

        when (actionObj) {
            is COSDictionary -> {
                onAction(actionObj)
                walkActions(actionObj.getDictionaryObject(COSName.getPDFName("Next")), visited, onAction)
            }
            is com.tom_roush.pdfbox.cos.COSArray -> {
                for (i in 0 until actionObj.size()) {
                    walkActions(actionObj.getObject(i), visited, onAction)
                }
            }
        }
    }

    private fun scanAaDictionary(
        aaObj: com.tom_roush.pdfbox.cos.COSBase?,
        onAction: (COSDictionary) -> Unit
    ) {
        if (aaObj is COSDictionary) {
            for (key in aaObj.keySet()) {
                walkActions(aaObj.getDictionaryObject(key), mutableSetOf(), onAction)
            }
        }
    }

    private fun hasMaliciousAction(
        actionObj: com.tom_roush.pdfbox.cos.COSBase?,
        checkJs: Boolean,
        checkActions: Boolean,
        visited: MutableSet<com.tom_roush.pdfbox.cos.COSBase> = mutableSetOf()
    ): Boolean {
        if (actionObj == null) return false
        if (!visited.add(actionObj)) return false

        when (actionObj) {
            is COSDictionary -> {
                val s = actionObj.getNameAsString(COSName.S)
                if (checkJs && s == "JavaScript") return true
                if (checkActions && s in listOf("Launch", "SubmitForm", "ImportData", "GoToE", "GoToR", "URI", "Sound", "Movie")) return true
                return hasMaliciousAction(actionObj.getDictionaryObject(COSName.getPDFName("Next")), checkJs, checkActions, visited)
            }
            is com.tom_roush.pdfbox.cos.COSArray -> {
                for (i in 0 until actionObj.size()) {
                    if (hasMaliciousAction(actionObj.getObject(i), checkJs, checkActions, visited)) return true
                }
            }
        }
        return false
    }

    private fun hasMaliciousAa(
        aaObj: com.tom_roush.pdfbox.cos.COSBase?,
        checkJs: Boolean,
        checkActions: Boolean
    ): Boolean {
        if (aaObj !is COSDictionary) return false
        for (key in aaObj.keySet()) {
            if (hasMaliciousAction(aaObj.getDictionaryObject(key), checkJs, checkActions, mutableSetOf())) return true
        }
        return false
    }
}
