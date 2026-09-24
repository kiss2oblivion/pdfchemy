package com.pdfchemy.app.logic

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RedactionBox(
    val pageIndex: Int,
    val normalizedRect: RectF, // Coordinates normalized to [0..1] relative to page width and height
    val overlayLabel: String? = "REDACTED"
)

object PdfRedactor {

    /**
     * Applies permanent visual redaction to the specified pages of a PDF.
     * Draws opaque redaction fill rectangles and optional security warning labels
     * to sanitize sensitive data permanently.
     */
    suspend fun applyRedactions(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        redactions: List<RedactionBox>
    ): Boolean = withContext(Dispatchers.IO) {
        val config = RedactionConfig(
            isBlackout = true,
            forensicSanitize = true,
            defaultOverlayText = "REDACTED"
        )
        val result = com.pdfchemy.app.sandbox.SandboxCoordinator.applyRedactions(
            context = context,
            sourcePdfUri = sourceUri,
            destPdfUri = destUri,
            boxes = redactions,
            config = config
        )
        result.isSuccess
    }
}
