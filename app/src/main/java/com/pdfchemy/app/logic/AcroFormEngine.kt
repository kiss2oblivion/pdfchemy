package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget
import com.tom_roush.pdfbox.pdmodel.interactive.form.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

enum class FormFieldType {
    TEXT,
    CHECKBOX,
    RADIO,
    CHOICE,
    SIGNATURE,
    OTHER
}

data class FormFieldInfo(
    val name: String,
    val fullyQualifiedName: String,
    val type: FormFieldType,
    val value: String,
    val possibleOptions: List<String> = emptyList(),
    val isReadOnly: Boolean = false,
    val isRequired: Boolean = false
)

data class InteractiveFieldSpec(
    val pageIndex: Int,
    val name: String,
    val type: FormFieldType = FormFieldType.TEXT,
    val xRatio: Float = 0.1f,
    val yRatio: Float = 0.1f,
    val widthRatio: Float = 0.4f,
    val heightRatio: Float = 0.05f,
    val defaultValue: String = "",
    val options: List<String> = emptyList()
)

object AcroFormEngine {

    suspend fun hasAcroForm(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                doc = PDDocument.load(stream)
                val acroForm = doc?.documentCatalog?.acroForm
                val hasFields = acroForm != null && acroForm.fields.isNotEmpty()
                hasFields
            } ?: false
        } catch (e: Exception) {
            false
        } finally {
            doc?.close()
        }
    }

    suspend fun extractFields(context: Context, sourceUri: Uri): List<FormFieldInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FormFieldInfo>()
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                doc = PDDocument.load(stream)
                val acroForm = doc?.documentCatalog?.acroForm ?: return@withContext emptyList()
                
                for (field in acroForm.fieldTree) {
                    val fieldInfo = parseField(field)
                    if (fieldInfo != null) {
                        result.add(fieldInfo)
                    }
                }
            }
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to extract AcroForm fields: ${e.message}", e)
        } finally {
            doc?.close()
        }
        result
    }

    private fun parseField(field: PDField): FormFieldInfo? {
        val name = field.partialName ?: field.fullyQualifiedName ?: return null
        val fqName = field.fullyQualifiedName ?: name
        val isReadOnly = field.isReadOnly
        val isRequired = field.isRequired

        return when (field) {
            is PDTextField -> {
                FormFieldInfo(
                    name = name,
                    fullyQualifiedName = fqName,
                    type = FormFieldType.TEXT,
                    value = field.value ?: "",
                    isReadOnly = isReadOnly,
                    isRequired = isRequired
                )
            }
            is PDCheckBox -> {
                FormFieldInfo(
                    name = name,
                    fullyQualifiedName = fqName,
                    type = FormFieldType.CHECKBOX,
                    value = if (field.isChecked) "Yes" else "Off",
                    possibleOptions = listOf("Yes", "Off"),
                    isReadOnly = isReadOnly,
                    isRequired = isRequired
                )
            }
            is PDRadioButton -> {
                val options = field.onValues.toList()
                FormFieldInfo(
                    name = name,
                    fullyQualifiedName = fqName,
                    type = FormFieldType.RADIO,
                    value = field.value ?: "",
                    possibleOptions = options,
                    isReadOnly = isReadOnly,
                    isRequired = isRequired
                )
            }
            is PDChoice -> {
                val options = field.options ?: emptyList()
                FormFieldInfo(
                    name = name,
                    fullyQualifiedName = fqName,
                    type = FormFieldType.CHOICE,
                    value = field.value?.firstOrNull() ?: "",
                    possibleOptions = options,
                    isReadOnly = isReadOnly,
                    isRequired = isRequired
                )
            }
            is PDSignatureField -> {
                FormFieldInfo(
                    name = name,
                    fullyQualifiedName = fqName,
                    type = FormFieldType.SIGNATURE,
                    value = if (field.value != null) "Signed" else "Unsigned",
                    isReadOnly = true,
                    isRequired = isRequired
                )
            }
            else -> {
                FormFieldInfo(
                    name = name,
                    fullyQualifiedName = fqName,
                    type = FormFieldType.OTHER,
                    value = field.valueAsString ?: "",
                    isReadOnly = isReadOnly,
                    isRequired = isRequired
                )
            }
        }
    }

    suspend fun fillAndSaveForm(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        fieldValues: Map<String, String>,
        flattenForm: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                doc = PDDocument.load(inStream)
                val acroForm = doc?.documentCatalog?.acroForm ?: return@withContext false

                for ((fqName, value) in fieldValues) {
                    val field = acroForm.getField(fqName)
                    if (field != null) {
                        try {
                            when (field) {
                                is PDTextField -> field.setValue(value)
                                is PDCheckBox -> {
                                    if (value.equals("Yes", ignoreCase = true) || value.equals("true", ignoreCase = true) || value.equals("on", ignoreCase = true)) {
                                        field.check()
                                    } else {
                                        field.unCheck()
                                    }
                                }
                                is PDRadioButton -> field.setValue(value)
                                is PDChoice -> field.setValue(value)
                                else -> field.setValue(value)
                            }
                        } catch (e: Exception) {
                            com.pdfchemy.app.utils.AppLogger.w("Could not set value for field $fqName: ${e.message}")
                        }
                    }
                }

                if (flattenForm) {
                    try {
                        if (acroForm.defaultResources == null) {
                            val dr = com.tom_roush.pdfbox.pdmodel.PDResources()
                            dr.put(com.tom_roush.pdfbox.cos.COSName.getPDFName("Helv"), com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA)
                            acroForm.defaultResources = dr
                        }
                        acroForm.flatten()
                    } catch (e: Exception) {
                        com.pdfchemy.app.utils.AppLogger.w("Failed to flatten AcroForm: ${e.message}")
                    }
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    doc?.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to fill AcroForm: ${e.message}", e)
            false
        } finally {
            doc?.close()
        }
    }

    suspend fun createAcroFormWithFields(
        context: Context,
        sourceUri: Uri,
        destUri: Uri,
        fields: List<InteractiveFieldSpec>
    ): Boolean = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)
        var doc: PDDocument? = null
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inStream ->
                doc = PDDocument.load(inStream)
                val document = doc ?: return@withContext false
                val catalog = document.documentCatalog
                var acroForm = catalog.acroForm
                if (acroForm == null) {
                    acroForm = PDAcroForm(document)
                    catalog.acroForm = acroForm
                }

                var dr = acroForm.defaultResources
                if (dr == null) {
                    dr = PDResources()
                    acroForm.defaultResources = dr
                }
                val helvName = COSName.getPDFName("Helv")
                dr.put(helvName, PDType1Font.HELVETICA)
                acroForm.defaultAppearance = "/Helv 12 Tf 0 g"

                val pageCount = document.numberOfPages
                for (spec in fields) {
                    if (spec.pageIndex < 0 || spec.pageIndex >= pageCount) continue
                    val page = document.getPage(spec.pageIndex)
                    val cropBox = page.cropBox ?: page.mediaBox
                    val pw = cropBox.width
                    val ph = cropBox.height

                    val x = cropBox.lowerLeftX + (spec.xRatio.coerceIn(0f, 1f) * pw)
                    val w = (spec.widthRatio.coerceIn(0.01f, 1f) * pw)
                    val h = (spec.heightRatio.coerceIn(0.01f, 1f) * ph)
                    val y = cropBox.lowerLeftY + (ph - (spec.yRatio.coerceIn(0f, 1f) + spec.heightRatio.coerceIn(0.01f, 1f)) * ph)

                    val rect = PDRectangle(x, y, w, h)

                    when (spec.type) {
                        FormFieldType.CHECKBOX -> {
                            val cb = PDCheckBox(acroForm)
                            cb.partialName = spec.name
                            val widget = cb.widgets.firstOrNull() ?: PDAnnotationWidget().also {
                                cb.widgets = listOf(it)
                            }
                            widget.rectangle = rect
                            widget.page = page
                            widget.isPrinted = true
                            page.annotations.add(widget)
                            if (spec.defaultValue.equals("Yes", true) || spec.defaultValue.equals("true", true) || spec.defaultValue.equals("1", true)) {
                                cb.check()
                            } else {
                                cb.unCheck()
                            }
                            acroForm.fields.add(cb)
                        }
                        FormFieldType.CHOICE -> {
                            val combo = PDComboBox(acroForm)
                            combo.partialName = spec.name
                            combo.defaultAppearance = "/Helv 12 Tf 0 g"
                            if (spec.options.isNotEmpty()) {
                                combo.options = spec.options
                            }
                            val widget = combo.widgets.firstOrNull() ?: PDAnnotationWidget().also {
                                combo.widgets = listOf(it)
                            }
                            widget.rectangle = rect
                            widget.page = page
                            widget.isPrinted = true
                            page.annotations.add(widget)
                            if (spec.defaultValue.isNotBlank()) {
                                combo.setValue(spec.defaultValue)
                            } else if (spec.options.isNotEmpty()) {
                                combo.setValue(spec.options.first())
                            }
                            acroForm.fields.add(combo)
                        }
                        else -> { // TEXT and default
                            val tf = PDTextField(acroForm)
                            tf.partialName = spec.name
                            tf.defaultAppearance = "/Helv 12 Tf 0 g"
                            val widget = tf.widgets.firstOrNull() ?: PDAnnotationWidget().also {
                                tf.widgets = listOf(it)
                            }
                            widget.rectangle = rect
                            widget.page = page
                            widget.isPrinted = true
                            page.annotations.add(widget)
                            if (spec.defaultValue.isNotBlank()) {
                                tf.setValue(spec.defaultValue)
                            }
                            acroForm.fields.add(tf)
                        }
                    }
                }

                context.contentResolver.openOutputStream(destUri)?.use { outStream ->
                    document.save(outStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            com.pdfchemy.app.utils.AppLogger.e("Failed to create AcroForm fields: ${e.message}", e)
            false
        } finally {
            doc?.close()
        }
    }
}
