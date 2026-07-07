package com.example.shrinkpdf.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.shrinkpdf.logic.PdfCompressor
import com.example.shrinkpdf.logic.PdfAnalysis
import com.example.shrinkpdf.logic.PdfScenario
import com.example.shrinkpdf.logic.PdfManipulator
import com.example.shrinkpdf.logic.TextToPdfConverter
import com.example.shrinkpdf.billing.BillingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

        private val prefs = application.getSharedPreferences("shrinkpdf_settings", Context.MODE_PRIVATE)

    private val _isHapticEnabled = MutableStateFlow(prefs.getBoolean("haptic", true))
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _isSfxEnabled = MutableStateFlow(prefs.getBoolean("sfx", true))
    val isSfxEnabled: StateFlow<Boolean> = _isSfxEnabled.asStateFlow()

    fun setHapticEnabled(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        prefs.edit().putBoolean("haptic", enabled).apply()
    }

    fun setSfxEnabled(enabled: Boolean) {
        _isSfxEnabled.value = enabled
        prefs.edit().putBoolean("sfx", enabled).apply()
    }

    private val _warning = MutableStateFlow<String?>(null)
    val warning: StateFlow<String?> = _warning.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _premiumPrice = MutableStateFlow("\$1.99")
    val premiumPrice: StateFlow<String> = _premiumPrice.asStateFlow()

    private var billingManager: BillingManager? = null

    fun initBilling(context: Context) {
        if (billingManager == null) {
            billingManager = BillingManager(context.applicationContext, viewModelScope)
            viewModelScope.launch {
                billingManager?.isPremium?.collect { _isPremium.value = it }
            }
            viewModelScope.launch {
                billingManager?.premiumPrice?.collect { _premiumPrice.value = it }
            }
        }
    }
    private val _compressionQuality = MutableStateFlow(0.50f)
    val compressionQuality: StateFlow<Float> = _compressionQuality.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedFileSize = MutableStateFlow<Long>(-1L)
    val selectedFileSize: StateFlow<Long> = _selectedFileSize.asStateFlow()

    private val _pdfAnalysis = MutableStateFlow<PdfAnalysis?>(null)
    val pdfAnalysis: StateFlow<PdfAnalysis?> = _pdfAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _useGrayscale = MutableStateFlow(false)
    val useGrayscale: StateFlow<Boolean> = _useGrayscale.asStateFlow()

    private val _useLossless = MutableStateFlow(false)
    val useLossless: StateFlow<Boolean> = _useLossless.asStateFlow()

    private val _stripMetadata = MutableStateFlow(false)
    val stripMetadata: StateFlow<Boolean> = _stripMetadata.asStateFlow()

    sealed class UiState {
        object Idle : UiState()
        object Processing : UiState()
        data class BatchProcessing(val current: Int, val total: Int, val currentFileName: String) : UiState()
        data class Success(val title: String, val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    data class SelectedFile(
        val uri: Uri,
        val name: String,
        val size: Long,
        val analysis: PdfAnalysis? = null,
        val isAnalyzing: Boolean = false
    )

    fun setQuality(quality: Float) {
        _compressionQuality.value = quality
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun purchasePremium(activity: android.app.Activity) {
        billingManager?.launchPurchaseFlow(activity)
    }

    fun setUseGrayscale(value: Boolean) {
        _useGrayscale.value = value
    }

    fun setUseLossless(value: Boolean) {
        _useLossless.value = value
    }

    fun setStripMetadata(value: Boolean) {
        _stripMetadata.value = value
    }

    fun onFileSelected(context: Context, uri: Uri) {
        _pdfAnalysis.value = null
        _selectedFileSize.value = -1L
        _useGrayscale.value = false
        _useLossless.value = false
        _stripMetadata.value = false
        viewModelScope.launch {
            val size = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            } catch (e: Exception) {
                -1L
            }
            _selectedFileSize.value = size

            _isAnalyzing.value = true
            val analysisResult = PdfCompressor.analyzePdf(context, uri)
            _isAnalyzing.value = false
            analysisResult.onSuccess { analysis ->
                _pdfAnalysis.value = analysis
                _compressionQuality.value = analysis.recommendedQuality
                
                // Smart auto-toggles recommendation based on scenario
                applyScenarioDefaults(analysis.scenario)
            }.onFailure {
                _compressionQuality.value = 0.50f
                _useGrayscale.value = false
                _useLossless.value = false
                _stripMetadata.value = false
            }
        }
    }

    fun loadTextFromFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                    _inputText.value = stringBuilder.toString()
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to read text file: ${e.message}")
            }
        }
    }

    fun compressPdf(context: Context, sourceUri: Uri, destUri: Uri) {
        if (_uiState.value is UiState.Processing) return

        viewModelScope.launch {
            _uiState.value = UiState.Processing
            
            val result = PdfCompressor.compressPdf(
                context = context,
                sourceUri = sourceUri,
                destUri = destUri,
                quality = _compressionQuality.value,
                useGrayscale = _useGrayscale.value,
                useLossless = _useLossless.value,
                stripMetadata = _stripMetadata.value
            )
            
            result.onSuccess { report ->
                if (report.hasSignatures) {
                    _warning.value = "Warning: Compressing may invalidate the digital signature."
                }

                val compressedSize = try {
                    context.contentResolver.openFileDescriptor(destUri, "r")?.use { it.statSize } ?: -1L
                } catch (e: Exception) {
                    -1L
                }

                val originalSize = report.originalSize
                
                val reductionDetails = StringBuilder().apply {
                    if (originalSize > 0 && compressedSize > 0) {
                        val reductionPercent = ((originalSize - compressedSize).toFloat() / originalSize * 100).toInt()
                        val saved = formatSize(originalSize - compressedSize)
                        if (compressedSize > originalSize) {
                            append("The output is slightly larger than the original. This PDF may already be highly optimized.\n\n")
                        } else {
                            append("Reduced by $reductionPercent% ($saved saved!)\n\n")
                        }
                        // Verdict based on reduction percent
                        val verdict = when {
                            compressedSize > originalSize -> "No compression benefit"
                            reductionPercent >= 50 -> "Excellent compression"
                            reductionPercent >= 20 -> "Good compression"
                            reductionPercent >= 5 -> "Minor compression"
                            else -> "Minimal compression gain"
                        }
                        append("Verdict: $verdict\n\n")
                        append("Original Size: ${formatSize(originalSize)}\n")
                        append("Compressed Size: ${formatSize(compressedSize)}\n\n")
                        append("Settings Used:\n")
                        append("- Quality Preset: ${(compressionQuality.value * 100).toInt()}%\n")
                        append("- Grayscale: ${if (useGrayscale.value) "Enabled" else "Disabled"}\n")
                        append("- Lossless ZIP: ${if (useLossless.value) "Enabled" else "Disabled"}\n")
                        append("- Metadata Removed: ${if (stripMetadata.value) "Yes" else "No"}")
                    } else {
                        append("Compression finished successfully.\n\n")
                    }
                }.toString()

                _uiState.value = UiState.Success("Compression Result", reductionDetails)
                
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "An unknown error occurred.")
            }
        }
    }

    fun convertTextToPdf(context: Context, destUri: Uri) {
        if (_uiState.value is UiState.Processing) return

        viewModelScope.launch {
            _uiState.value = UiState.Processing
            
            val result = TextToPdfConverter.convert(context, _inputText.value, destUri)
            
            result.onSuccess {
                _uiState.value = UiState.Success("PDF Created", "Your document has been saved successfully.")
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Failed to create PDF.")
            }
        }
    }

    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun onFilesSelected(context: Context, uris: List<Uri>) {
        _selectedFiles.value = emptyList()
        viewModelScope.launch {
            val list = uris.map { uri ->
                val size = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                } catch (e: Exception) {
                    -1L
                }
                val name = com.example.shrinkpdf.utils.FileUtils.getFileName(context, uri) ?: "unknown_file.pdf"
                SelectedFile(uri, name, size, isAnalyzing = true)
            }
            _selectedFiles.value = list

            // Analyze them sequentially
            list.forEachIndexed { index, selectedFile ->
                val analysisResult = PdfCompressor.analyzePdf(context, selectedFile.uri)
                analysisResult.onSuccess { analysis ->
                    _selectedFiles.value = _selectedFiles.value.mapIndexed { idx, item ->
                        if (idx == index) item.copy(analysis = analysis, isAnalyzing = false) else item
                    }
                    if (index == 0) {
                        _pdfAnalysis.value = analysis
                        _compressionQuality.value = analysis.recommendedQuality
                        applyScenarioDefaults(analysis.scenario)
                    }
                }.onFailure {
                    _selectedFiles.value = _selectedFiles.value.mapIndexed { idx, item ->
                        if (idx == index) item.copy(isAnalyzing = false) else item
                    }
                }
            }
        }
    }

    fun compressBatch(context: Context, destTreeUri: Uri) {
        if (_uiState.value is UiState.Processing || _uiState.value is UiState.BatchProcessing) return

        viewModelScope.launch(Dispatchers.IO) {
            val files = _selectedFiles.value
            if (files.isEmpty()) {
                _uiState.value = UiState.Error("No files selected for batch compression.")
                return@launch
            }

            val directory = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, destTreeUri)
            if (directory == null || !directory.exists()) {
                _uiState.value = UiState.Error("Selected folder is invalid or does not exist.")
                return@launch
            }

            val total = files.size
            var successCount = 0
            val sizeSavedMap = mutableMapOf<Int, Long>()

            files.forEachIndexed { index, selectedFile ->
                _uiState.value = UiState.BatchProcessing(index + 1, total, selectedFile.name)

                val outputName = "compressed_${selectedFile.name}"
                val outputDoc = directory.createFile("application/pdf", outputName)
                if (outputDoc == null) {
                    return@forEachIndexed
                }

                val compressResult = PdfCompressor.compressPdf(
                    context = context,
                    sourceUri = selectedFile.uri,
                    destUri = outputDoc.uri,
                    quality = _compressionQuality.value,
                    useGrayscale = _useGrayscale.value,
                    useLossless = _useLossless.value,
                    stripMetadata = _stripMetadata.value
                )

                compressResult.onSuccess { report ->
                    val compressedSize = try {
                        context.contentResolver.openFileDescriptor(outputDoc.uri, "r")?.use { it.statSize } ?: -1L
                    } catch (e: Exception) { -1L }
                    if (report.originalSize > 0 && compressedSize > 0 && compressedSize < report.originalSize) {
                        sizeSavedMap[index] = report.originalSize - compressedSize
                    }
                    successCount++
                }
            }

            if (successCount == total) {
                val totalSavedBytes = sizeSavedMap.values.sum()
                val totalSavedStr = formatSize(totalSavedBytes)
                _uiState.value = UiState.Success(
                    "Batch Compression Finished",
                    "Successfully compressed all $total files!\nTotal space saved: $totalSavedStr"
                )
            } else if (successCount > 0) {
                val totalSavedBytes = sizeSavedMap.values.sum()
                val totalSavedStr = formatSize(totalSavedBytes)
                _uiState.value = UiState.Success(
                    "Batch Compression Finished",
                    "Successfully compressed $successCount of $total files.\nTotal space saved: $totalSavedStr"
                )
            } else {
                _uiState.value = UiState.Error("Failed to compress any files in the batch.")
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
        _warning.value = null
        _pdfAnalysis.value = null
        _isAnalyzing.value = false
        _useGrayscale.value = false
        _useLossless.value = false
        _stripMetadata.value = false
        _selectedFiles.value = emptyList()
    }

    private fun applyScenarioDefaults(scenario: com.example.shrinkpdf.logic.PdfScenario) {
        when (scenario) {
            com.example.shrinkpdf.logic.PdfScenario.SIGNED_OFFICIAL -> {
                _useGrayscale.value = false
                _useLossless.value = true
                _stripMetadata.value = false
            }
            com.example.shrinkpdf.logic.PdfScenario.SCANNED_IMAGE_HEAVY -> {
                _useGrayscale.value = true
                _useLossless.value = false
                _stripMetadata.value = true
            }
            com.example.shrinkpdf.logic.PdfScenario.TEXT_VECTOR -> {
                _useGrayscale.value = false
                _useLossless.value = true
                _stripMetadata.value = true
            }
            com.example.shrinkpdf.logic.PdfScenario.MIXED -> {
                _useGrayscale.value = false
                _useLossless.value = false
                _stripMetadata.value = true
            }
        }
    }

    fun dismissWarning() {
        _warning.value = null
    }

    fun estimateCompressedSize(originalSize: Long, quality: Float, useLossless: Boolean, useGrayscale: Boolean, scenario: com.example.shrinkpdf.logic.PdfScenario?): Long {
        if (originalSize <= 0) return 0L
        if (useLossless) return (originalSize * 1.05).toLong()
        
        // Use continuous ratio estimation instead of fixed breakpoints
        var ratio = 0.10f + (quality * 0.85f) // Maps 0.25 -> ~0.31, 0.50 -> ~0.52, 0.75 -> ~0.74
        if (scenario == com.example.shrinkpdf.logic.PdfScenario.TEXT_VECTOR) {
            ratio = 0.80f + (quality * 0.20f) // Maps 0.25 -> 0.85, 0.50 -> 0.90, 0.75 -> 0.95
        }
        
        val grayscaleDiscount = if (useGrayscale) 0.85f else 1.0f
        return (originalSize * ratio * grayscaleDiscount).toLong()
    }

    fun formatSize(bytes: Long): String {
        if (bytes < 0) return "-${formatSize(-bytes)}"
        if (bytes == 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceIn(0, units.size - 1)
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}


