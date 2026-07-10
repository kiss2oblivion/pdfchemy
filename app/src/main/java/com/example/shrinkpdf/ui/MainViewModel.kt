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
import com.example.shrinkpdf.logic.PdfMetadata
import com.example.shrinkpdf.logic.PdfMetadataManager
import com.example.shrinkpdf.logic.PdfTextExtractor
import com.example.shrinkpdf.logic.TextToPdfConverter
import com.example.shrinkpdf.billing.BillingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import com.example.shrinkpdf.utils.AppLogger
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.cos.COSName
import androidx.compose.ui.res.stringResource
import com.example.shrinkpdf.R

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("shrinkpdf_settings", Context.MODE_PRIVATE)

    private val historyRepository = com.example.shrinkpdf.logic.HistoryRepository(application)
    private val _historyList = MutableStateFlow(historyRepository.getHistory())
    val historyList: StateFlow<List<com.example.shrinkpdf.logic.HistoryItem>> = _historyList.asStateFlow()

    fun refreshHistory() {
        _historyList.value = historyRepository.getHistory()
    }
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

    private val _isHistoryEnabled = MutableStateFlow(prefs.getBoolean("history_enabled", true))
    val isHistoryEnabled: StateFlow<Boolean> = _isHistoryEnabled.asStateFlow()

    fun setHistoryEnabled(enabled: Boolean) {
        _isHistoryEnabled.value = enabled
        prefs.edit().putBoolean("history_enabled", enabled).apply()
        if (!enabled) {
            clearHistory()
        }
    }

    fun clearHistory() {
        historyRepository.clearHistory()
        refreshHistory()
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

    private val metadataManager = PdfMetadataManager()

    private val _currentMetadata = MutableStateFlow<PdfMetadata?>(null)
    val currentMetadata: StateFlow<PdfMetadata?> = _currentMetadata.asStateFlow()

    sealed class UiState {
        object Idle : UiState()
        object Processing : UiState()
        data class BatchProcessing(val current: Int, val total: Int, val currentFileName: String) : UiState()
        data class Success(val title: String, val message: String, val outputUris: List<Uri> = emptyList()) : UiState()
        data class Warning(val title: String, val message: String, val outputUris: List<Uri> = emptyList()) : UiState()
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

    private val _targetMb = MutableStateFlow<Float?>(null)
    val targetMb: StateFlow<Float?> = _targetMb.asStateFlow()

    fun setTargetMb(mb: Float?) {
        _targetMb.value = mb
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
                stripMetadata = _stripMetadata.value,
                targetMb = _targetMb.value
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
                        if (_targetMb.value != null) {
                            append("- Target Size: ${_targetMb.value} MB\n")
                            append("- Auto-Optimized: Yes\n")
                        } else {
                            append("- Quality Preset: ${(compressionQuality.value * 100).toInt()}%\n")
                        }
                        append("- Grayscale: ${if (useGrayscale.value) "Enabled" else "Disabled"}\n")
                        append("- Lossless ZIP: ${if (useLossless.value) "Enabled" else "Disabled"}\n")
                        append("- Metadata Removed: ${if (stripMetadata.value) "Yes" else "No"}")
                    } else {
                        append("Compression finished successfully.\n\n")
                    }
                }.toString()

                if (report.targetMissed) {
                    _uiState.value = UiState.Warning(
                        "Target Size Unreachable",
                        "The best possible compression was applied, but the file could not be compressed under ${_targetMb.value} MB without destroying the content.\n\n" + reductionDetails,
                        listOf(destUri)
                    )
                } else {
                    _uiState.value = UiState.Success("Compression Result", reductionDetails, listOf(destUri))
                }
                historyRepository.addHistoryItem(destUri, "Compressed PDF", "Compress")
                refreshHistory()
                
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
                historyRepository.addHistoryItem(destUri, "Text to PDF", "Convert")
                refreshHistory()
                _uiState.value = UiState.Success("PDF Created", "Your document has been saved successfully.", listOf(destUri))
            }.onFailure { error ->
                _uiState.value = UiState.Error(error.message ?: "Failed to create PDF.")
            }
        }
    }

    fun convertImagesToPdf(context: Context, imageUris: List<Uri>, destUri: Uri) {
        if (_uiState.value is UiState.Processing) return

        viewModelScope.launch {
            _uiState.value = UiState.Processing
            try {
                withContext(Dispatchers.IO) {
                    val document = com.tom_roush.pdfbox.pdmodel.PDDocument()
                    val total = imageUris.size
                    
                    for ((index, uri) in imageUris.withIndex()) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            if (bitmap != null) {
                                val page = com.tom_roush.pdfbox.pdmodel.PDPage(com.tom_roush.pdfbox.pdmodel.common.PDRectangle.A4)
                                document.addPage(page)
                                
                                val pdImage = com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(document, bitmap)
                                val contentStream = com.tom_roush.pdfbox.pdmodel.PDPageContentStream(document, page)
                                
                                val pageWidth = page.mediaBox.width
                                val pageHeight = page.mediaBox.height
                                val margin = 20f
                                val maxWidth = pageWidth - margin * 2
                                val maxHeight = pageHeight - margin * 2
                                
                                val imgWidth = bitmap.width.toFloat()
                                val imgHeight = bitmap.height.toFloat()
                                
                                val scale = minOf(maxWidth / imgWidth, maxHeight / imgHeight)
                                val drawWidth = imgWidth * scale
                                val drawHeight = imgHeight * scale
                                
                                val startX = (pageWidth - drawWidth) / 2
                                val startY = (pageHeight - drawHeight) / 2
                                
                                contentStream.drawImage(pdImage, startX, startY, drawWidth, drawHeight)
                                contentStream.close()
                                bitmap.recycle()
                            }
                        }
                    }
                    
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        document.save(out)
                    }
                    document.close()
                }
                _uiState.value = UiState.Success("PDF Created", "Your images have been converted successfully.")
            } catch (e: Exception) {
                AppLogger.e("Exception in MainViewModel", e)
                _uiState.value = UiState.Error(e.message ?: "Failed to create PDF from images.")
            }
        }
    }

    private val _selectedFiles = MutableStateFlow<List<SelectedFile>>(emptyList())
    val selectedFiles: StateFlow<List<SelectedFile>> = _selectedFiles.asStateFlow()

    fun clearSelectedFiles() {
        _selectedFiles.value = emptyList()
    }

    fun removeSelectedFile(uri: Uri) {
        _selectedFiles.value = _selectedFiles.value.filter { it.uri != uri }
    }

    fun onFilesSelected(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val existingUris = _selectedFiles.value.map { it.uri }.toSet()
            val newUris = uris.filter { it !in existingUris }
            if (newUris.isEmpty()) return@launch

            val newList = newUris.map { uri ->
                val size = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                } catch (e: Exception) {
                    -1L
                }
                val name = com.example.shrinkpdf.utils.FileUtils.getFileName(context, uri) ?: "unknown_file.pdf"
                SelectedFile(uri, name, size, isAnalyzing = true)
            }
            _selectedFiles.value = _selectedFiles.value + newList

            // Analyze them sequentially
            newList.forEach { selectedFile ->
                val analysisResult = PdfCompressor.analyzePdf(context, selectedFile.uri)
                analysisResult.onSuccess { analysis ->
                    _selectedFiles.value = _selectedFiles.value.map { item ->
                        if (item.uri == selectedFile.uri) item.copy(analysis = analysis, isAnalyzing = false) else item
                    }
                    if (_pdfAnalysis.value == null && _selectedFiles.value.firstOrNull()?.uri == selectedFile.uri) {
                        _pdfAnalysis.value = analysis
                        _compressionQuality.value = analysis.recommendedQuality
                        applyScenarioDefaults(analysis.scenario)
                    }
                }.onFailure {
                    _selectedFiles.value = _selectedFiles.value.map { item ->
                        if (item.uri == selectedFile.uri) item.copy(isAnalyzing = false) else item
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
            val outputUris = mutableListOf<Uri>()

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
                    stripMetadata = _stripMetadata.value,
                    targetMb = _targetMb.value
                )

                compressResult.onSuccess { report ->
                    outputUris.add(outputDoc.uri)
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
                historyRepository.addHistoryItem(destTreeUri, "Batch Compression Folder", "Compress Batch")
                refreshHistory()
                val totalSavedBytes = sizeSavedMap.values.sum()
                val totalSavedStr = formatSize(totalSavedBytes)
                _uiState.value = UiState.Success(
                    "Batch Compression Finished",
                    "Successfully compressed all $total files!\nTotal space saved: $totalSavedStr",
                    outputUris
                )
            } else if (successCount > 0) {
                historyRepository.addHistoryItem(destTreeUri, "Batch Compression Folder", "Compress Batch")
                refreshHistory()
                val totalSavedBytes = sizeSavedMap.values.sum()
                val totalSavedStr = formatSize(totalSavedBytes)
                _uiState.value = UiState.Success(
                    "Batch Compression Finished",
                    "Successfully compressed $successCount of $total files.\nTotal space saved: $totalSavedStr",
                    outputUris
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

    fun mergePdfs(context: Context, sourceUris: List<Uri>, destUri: Uri) {
        if (_uiState.value is UiState.Processing) return

        viewModelScope.launch {
            _uiState.value = UiState.Processing
            try {
                PdfManipulator.mergePdfs(context, sourceUris, destUri)
                historyRepository.addHistoryItem(destUri, "Merged PDF", "Merge")
                refreshHistory()
                _uiState.value = UiState.Success("Merge Complete", "Successfully merged ${sourceUris.size} documents.", listOf(destUri))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to merge PDFs.")
            }
        }
    }

    fun resetMetadata() {
        _currentMetadata.value = null
        _pdfAnalysis.value = null
    }

    fun loadMetadata(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            val metadataResult = metadataManager.getMetadata(context, uri)
            val analysisResult = PdfCompressor.analyzePdf(context, uri)

            if (metadataResult.isSuccess && analysisResult.isSuccess) {
                _currentMetadata.value = metadataResult.getOrNull()
                _pdfAnalysis.value = analysisResult.getOrNull()
            } else {
                _uiState.value = UiState.Error("Failed to load metadata or analysis.")
            }
            _isAnalyzing.value = false
        }
    }

    fun updateMetadata(context: Context, sourceUri: Uri, destUri: Uri, newMetadata: PdfMetadata) {
        viewModelScope.launch {
            _uiState.value = UiState.Processing
            val result = metadataManager.updateMetadata(context, sourceUri, destUri, newMetadata)
            if (result.isSuccess) {
                historyRepository.addHistoryItem(destUri, "Updated Metadata PDF", "Metadata")
                refreshHistory()
                _uiState.value = UiState.Success("Metadata Updated", "The document's metadata has been successfully updated.", listOf(destUri))
            } else {
                _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Failed to update metadata.")
            }
        }
    }

    fun clearMetadata(context: Context, sourceUri: Uri, destUri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Processing
            val result = metadataManager.clearMetadata(context, sourceUri, destUri)
            if (result.isSuccess) {
                historyRepository.addHistoryItem(destUri, "Cleared Metadata PDF", "Metadata")
                refreshHistory()
                _uiState.value = UiState.Success("Metadata Removed", "All metadata has been stripped from the document.", listOf(destUri))
            } else {
                _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Failed to remove metadata.")
            }
        }
    }

    fun clearMetadataOverwrite(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = UiState.Processing
            val result = metadataManager.clearMetadataOverwrite(context, sourceUri)
            if (result.isSuccess) {
                historyRepository.addHistoryItem(sourceUri, "Cleared Metadata PDF", "Metadata")
                refreshHistory()
                _uiState.value = UiState.Success("Metadata Removed", "All metadata has been stripped and the original file has been overwritten.", listOf(sourceUri))
            } else {
                _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Failed to overwrite metadata.")
            }
        }
    }

    fun splitPdf(context: Context, sourceUri: Uri, destTreeUri: Uri, pageRange: String? = null) {
        if (_uiState.value is UiState.Processing) return

        viewModelScope.launch {
            _uiState.value = UiState.Processing
            try {
                val directory = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, destTreeUri)
                if (directory == null || !directory.exists()) {
                    _uiState.value = UiState.Error("Selected folder is invalid or does not exist.")
                    return@launch
                }
                
                val baseName = com.example.shrinkpdf.utils.FileUtils.getFileName(context, sourceUri)?.substringBeforeLast(".") ?: "split_doc"
                
                PdfManipulator.splitPdf(context, sourceUri, directory, baseName, pageRange)
                historyRepository.addHistoryItem(destTreeUri, "Split PDF Folder", "Split")
                refreshHistory()
                _uiState.value = UiState.Success("Split Complete", "Successfully split the document into the selected folder.", listOf(destTreeUri))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to split PDF.")
            }
        }
    }
    
    fun deletePages(context: Context, sourceUri: Uri, destUri: Uri, pageRange: String) {
        if (_uiState.value is UiState.Processing) return

        viewModelScope.launch {
            _uiState.value = UiState.Processing
            try {
                PdfManipulator.deletePages(context, sourceUri, destUri, pageRange)
                historyRepository.addHistoryItem(destUri, com.example.shrinkpdf.utils.FileUtils.getFileName(context, destUri) ?: "Unknown", "Organize")
                refreshHistory()
                _uiState.value = UiState.Success("Pages Deleted", "Successfully removed the selected pages.", listOf(destUri))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete pages.")
            }
        }
    }

    fun extractImagesFromPdf(pdfUri: Uri, outputDirectory: androidx.documentfile.provider.DocumentFile, context: Context, onComplete: (Int, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UiState.Processing
            var extractedCount = 0
            var errorCount = 0
            try {
                context.contentResolver.openInputStream(pdfUri)?.use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    for (pageIndex in 0 until document.numberOfPages) {
                        val page = document.getPage(pageIndex)
                        val resources = page.resources
                        if (resources != null) {
                            val xObjectNames = resources.xObjectNames
                            for (xObjectName in xObjectNames) {
                                val xObject = resources.getXObject(xObjectName)
                                if (xObject is PDImageXObject) {
                                    try {
                                        val bitmap = xObject.image
                                        if (bitmap != null) {
                                            val newFile = outputDirectory.createFile("image/jpeg", "extracted_image_${System.currentTimeMillis()}.jpg")
                                            newFile?.uri?.let { newUri ->
                                                context.contentResolver.openOutputStream(newUri)?.use { out ->
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                                    extractedCount++
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        AppLogger.e("Exception in MainViewModel", e)
                                        errorCount++
                                    }
                                }
                            }
                        }
                    }
                    document.close()
                }
                
                withContext(Dispatchers.Main) {
                    historyRepository.addHistoryItem(outputDirectory.uri, "Extracted Images Folder", "Extract Images")
                    refreshHistory()
                    onComplete(extractedCount, errorCount)
                    _uiState.value = UiState.Idle
                }
            } catch (e: Exception) {
                AppLogger.e("Exception in MainViewModel", e)
                withContext(Dispatchers.Main) {
                    onComplete(extractedCount, errorCount)
                    _uiState.value = UiState.Idle
                }
            }
        }
    }

    fun rotatePdf(context: Context, sourceUri: Uri, destUri: Uri, degrees: Int, pageRange: String = "") {
        _uiState.value = UiState.Processing
        viewModelScope.launch {
            try {
                PdfManipulator.rotatePdf(context, sourceUri, destUri, degrees, pageRange)
                withContext(Dispatchers.Main) {
                    historyRepository.addHistoryItem(destUri, "Rotated PDF", "Rotate PDF")
                    refreshHistory()
                    _uiState.value = UiState.Success("Success", "PDF rotated successfully", listOf(destUri))
                }
            } catch (e: Exception) {
                AppLogger.e("Exception in MainViewModel", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Error(e.message ?: "Rotation failed")
                }
            }
        }
    }

    fun extractTextFromPdf(context: Context, sourceUri: Uri, destUri: Uri) {
        _uiState.value = UiState.Processing
        viewModelScope.launch {
            try {
                val success = PdfTextExtractor.extractText(context, sourceUri, destUri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        historyRepository.addHistoryItem(destUri, "Extracted Text", "PDF to Text")
                        refreshHistory()
                        _uiState.value = UiState.Success("Success", "Text extracted successfully", listOf(destUri))
                    } else {
                        _uiState.value = UiState.Error("Could not extract text")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("Exception in MainViewModel", e)
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.Error(e.message ?: "Extraction failed")
                }
            }
        }
    }
}
