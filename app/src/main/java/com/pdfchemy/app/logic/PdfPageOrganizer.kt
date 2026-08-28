package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.pdfchemy.app.utils.AppLogger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PageAction(
    val originalPageIndex: Int? = null,
    val rotationDegrees: Int = 0,
    val isBlank: Boolean = false
)

object PdfPageOrganizer {

    suspend fun reorganizePages(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        actions: List<PageAction>
    ): Boolean = withContext(Dispatchers.IO) {
        var sourceDoc: PDDocument? = null
        var newDoc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                sourceDoc = PDDocument.load(inStream)
                if (sourceDoc == null) return@withContext false

                newDoc = PDDocument()
                val totalOriginalPages = sourceDoc!!.numberOfPages

                for (action in actions) {
                    if (action.isBlank) {
                        val blankPage = PDPage(PDRectangle.LETTER)
                        newDoc!!.addPage(blankPage)
                    } else if (action.originalPageIndex != null && action.originalPageIndex in 0 until totalOriginalPages) {
                        val originalPage = sourceDoc!!.getPage(action.originalPageIndex)
                        val importedPage = newDoc!!.importPage(originalPage)
                        val newRotation = (importedPage.rotation + action.rotationDegrees) % 360
                        importedPage.rotation = if (newRotation < 0) newRotation + 360 else newRotation
                    }
                }

                if (newDoc!!.numberOfPages == 0) {
                    return@withContext false
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    newDoc!!.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            AppLogger.e("Failed to reorganize PDF pages: ${e.message}", e)
            false
        } finally {
            sourceDoc?.close()
            newDoc?.close()
        }
    }
}
