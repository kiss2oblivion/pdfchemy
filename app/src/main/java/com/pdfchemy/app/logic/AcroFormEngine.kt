package com.pdfchemy.app.logic

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
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

object AcroFormEngine {

    suspend fun hasAcroForm(context: Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
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
}
