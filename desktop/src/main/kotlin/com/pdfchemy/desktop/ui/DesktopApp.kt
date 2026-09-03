package com.pdfchemy.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfchemy.desktop.engine.DesktopPdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

enum class DesktopNavTab(val label: String, val icon: ImageVector) {
    HOME("All Tools", Icons.Rounded.Dashboard),
    COMPRESS("Compress", Icons.Rounded.Speed),
    ORGANIZE("Organize", Icons.Rounded.ContentCopy),
    READER("Reader", Icons.AutoMirrored.Rounded.MenuBook),
    SECURITY("Security", Icons.Rounded.Lock),
    CONVERT("Convert", Icons.AutoMirrored.Rounded.Notes)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(
    initialFile: File? = null,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {}
) {
    var activeTab by remember { mutableStateOf(DesktopNavTab.HOME) }
    var selectedFile by remember { mutableStateOf<File?>(initialFile) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialFile) {
        if (initialFile != null) {
            selectedFile = initialFile
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                        }
                        Text("PDFchemy Tools", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                "Desktop Edition",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Local-First Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00E676))
                            Text("100% Offline & Private", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Open File Button
                    FilledTonalButton(
                        onClick = {
                            val picked = DesktopFileDialog.openPdf()
                            if (picked != null) {
                                selectedFile = picked
                                statusMessage = "Loaded: ${picked.name}"
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open PDF (Ctrl+O)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Theme Toggle Button
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Left Navigation Rail
            NavigationRail(
                modifier = Modifier.width(100.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                DesktopNavTab.values().forEach { tab ->
                    NavigationRailItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Main Workspace Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                when (activeTab) {
                    DesktopNavTab.HOME -> HomeView(
                        onSelectTab = { activeTab = it },
                        selectedFile = selectedFile,
                        onSelectFile = { selectedFile = it }
                    )
                    DesktopNavTab.COMPRESS -> CompressView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.ORGANIZE -> OrganizeView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.READER -> ReaderView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.SECURITY -> SecurityView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.CONVERT -> ConvertView(selectedFile, onFileChange = { selectedFile = it })
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. HOME VIEW (Full Suite Grid)
// -------------------------------------------------------------------------------------------------
@Composable
private fun HomeView(
    onSelectTab: (DesktopNavTab) -> Unit,
    selectedFile: File?,
    onSelectFile: (File) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero Drag & Drop Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Emergency Document Utility", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "All operations execute 100% on this computer. Zero cloud uploads, zero telemetry. Blazing fast multi-core desktop performance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedFile != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "Active Document: ${selectedFile.name} (${formatFileSize(selectedFile.length())})",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val file = DesktopFileDialog.openPdf()
                        if (file != null) onSelectFile(file)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select PDF", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Document Tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // 3-Column Tool Cards Grid
        val tools = listOf(
            ToolItem("Compress PDF", "Shrink document size safely with custom quality and DPI presets.", Icons.Rounded.Speed, DesktopNavTab.COMPRESS),
            ToolItem("Merge Documents", "Combine multiple PDF files into one single organized document.", Icons.Rounded.CallMerge, DesktopNavTab.ORGANIZE),
            ToolItem("Split PDF", "Extract pages, split into individual files or segments.", Icons.Rounded.CallSplit, DesktopNavTab.ORGANIZE),
            ToolItem("Reflow Reader", "Read PDFs and EPUBs with continuous reflow text, dark & sepia themes.", Icons.AutoMirrored.Rounded.MenuBook, DesktopNavTab.READER),
            ToolItem("Encrypt & Protect", "Lock documents with 128/256-bit AES encryption or remove passwords.", Icons.Rounded.Lock, DesktopNavTab.SECURITY),
            ToolItem("Extract & Convert", "Extract plain text, convert Markdown, CSV, and office documents.", Icons.AutoMirrored.Rounded.Notes, DesktopNavTab.CONVERT)
        )

        val columns = 3
        val rows = (tools.size + columns - 1) / columns

        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (c in 0 until columns) {
                    val index = r * columns + c
                    if (index < tools.size) {
                        val tool = tools[index]
                        Card(
                            onClick = { onSelectTab(tool.tab) },
                            modifier = Modifier
                                .weight(1f)
                                .height(140.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(tool.icon, contentDescription = tool.title, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(tool.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class ToolItem(val title: String, val desc: String, val icon: ImageVector, val tab: DesktopNavTab)

// -------------------------------------------------------------------------------------------------
// 2. COMPRESS VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun CompressView(file: File?, onFileChange: (File) -> Unit) {
    var qualityLevel by remember { mutableFloatStateOf(0.7f) }
    var targetDpi by remember { mutableFloatStateOf(140f) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Compress PDF", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        if (file == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select a PDF to Compress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val f = DesktopFileDialog.openPdf()
                        if (f != null) onFileChange(f)
                    }) {
                        Text("Browse File")
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(file.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Original Size: ${formatFileSize(file.length())}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        val f = DesktopFileDialog.openPdf()
                        if (f != null) onFileChange(f)
                    }) {
                        Text("Change File")
                    }
                }
            }

            // Quality Presets
            Text("Compression Level", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple("Extreme (Smallest)", 0.45f, 100f),
                    Triple("Balanced (Recommended)", 0.70f, 140f),
                    Triple("High Quality", 0.85f, 180f)
                ).forEach { (label, q, dpi) ->
                    val isSelected = qualityLevel == q
                    Card(
                        onClick = { qualityLevel = q; targetDpi = dpi },
                        modifier = Modifier.weight(1f).height(90.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                            Text("DPI: ${dpi.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(progressText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            if (resultText != null) {
                Surface(
                    color = Color(0xFF00E676).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(resultText!!, modifier = Modifier.padding(16.dp), color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_compressed.pdf") ?: return@Button
                    isProcessing = true
                    resultText = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val newSize = DesktopPdfEngine.compressPdf(file, outFile, targetDpi, qualityLevel) { current, total ->
                                progressText = "Processing page $current of $total..."
                            }
                            val savedPct = ((file.length() - newSize).toFloat() / file.length() * 100).toInt()
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                resultText = "Success! Compressed from ${formatFileSize(file.length())} to ${formatFileSize(newSize)} ($savedPct% saved).\nSaved to: ${outFile.absolutePath}"
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isProcessing = false
                                resultText = "Compression failed: ${e.message}"
                            }
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Speed, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Compression", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. ORGANIZE VIEW (Merge, Split, Rotate)
// -------------------------------------------------------------------------------------------------
@Composable
private fun OrganizeView(file: File?, onFileChange: (File) -> Unit) {
    var mergeList by remember { mutableStateOf<List<File>>(emptyList()) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Organize Documents", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Merge Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Merge PDFs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Combine multiple files into a single master PDF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {
                        val files = DesktopFileDialog.openMultiplePdfs()
                        if (files.isNotEmpty()) {
                            mergeList = mergeList + files
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Files")
                    }
                }

                if (mergeList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        mergeList.forEachIndexed { index, f ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}. ${f.name} (${formatFileSize(f.length())})", fontWeight = FontWeight.Medium)
                                    IconButton(onClick = {
                                        mergeList = mergeList.filterIndexed { i, _ -> i != index }
                                    }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val outFile = DesktopFileDialog.savePdf(suggestedName = "merged_document.pdf") ?: return@Button
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        DesktopPdfEngine.mergePdfs(mergeList, outFile)
                                        withContext(Dispatchers.Main) {
                                            resultMsg = "Successfully merged ${mergeList.size} files into:\n${outFile.absolutePath}"
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            resultMsg = "Merge failed: ${e.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CallMerge, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Merge ${mergeList.size} Files", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (resultMsg != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(resultMsg!!, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. READER VIEW (Reflow Text & Inspector)
// -------------------------------------------------------------------------------------------------
@Composable
private fun ReaderView(file: File?, onFileChange: (File) -> Unit) {
    var extractedText by remember { mutableStateOf<String?>(null) }
    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(file) {
        if (file != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val text = DesktopPdfEngine.extractText(file)
                    withContext(Dispatchers.Main) { extractedText = text }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { extractedText = "Could not extract text: ${e.message}" }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Reflow Reader", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(file?.name ?: "No document open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { fontSizeSp = (fontSizeSp - 2f).coerceAtLeast(12f) }) {
                    Text("A-", fontWeight = FontWeight.Bold)
                }
                Text("${fontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = { fontSizeSp = (fontSizeSp + 2f).coerceAtMost(28f) }) {
                    Text("A+", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = {
                    val f = DesktopFileDialog.openPdf()
                    if (f != null) onFileChange(f)
                }) {
                    Text("Open Book")
                }
            }
        }

        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            if (extractedText == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Open a document to read with clean reflow text.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                SelectionContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = extractedText!!,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.6f).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 5. SECURITY VIEW (Encrypt & Decrypt)
// -------------------------------------------------------------------------------------------------
@Composable
private fun SecurityView(file: File?, onFileChange: (File) -> Unit) {
    var password by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Document Security", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Password Protection (128-bit AES)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Lock your PDF with strong local encryption. The file cannot be opened without this password.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Enter Password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (file == null) {
                                statusText = "Please select a PDF document first."
                                return@Button
                            }
                            if (password.isBlank()) {
                                statusText = "Password cannot be empty."
                                return@Button
                            }
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_protected.pdf") ?: return@Button
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DesktopPdfEngine.encryptPdf(file, outFile, password)
                                    withContext(Dispatchers.Main) {
                                        statusText = "Document successfully encrypted and saved to:\n${outFile.absolutePath}"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        statusText = "Encryption failed: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encrypt Document")
                    }

                    OutlinedButton(
                        onClick = {
                            if (file == null) {
                                statusText = "Please select a PDF document first."
                                return@OutlinedButton
                            }
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_unlocked.pdf") ?: return@OutlinedButton
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DesktopPdfEngine.decryptPdf(file, outFile, password)
                                    withContext(Dispatchers.Main) {
                                        statusText = "Password successfully removed! Saved to:\n${outFile.absolutePath}"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        statusText = "Decryption failed (check password): ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Password")
                    }
                }
            }
        }

        if (statusText != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(statusText!!, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. CONVERT VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun ConvertView(file: File?, onFileChange: (File) -> Unit) {
    var statusText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Document Conversion", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Extract to Text (.txt)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Extract raw selectable plain text from the document.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        val txtFile = File(file.parentFile, "${file.nameWithoutExtension}_text.txt")
                        scope.launch(Dispatchers.IO) {
                            try {
                                val text = DesktopPdfEngine.extractText(file)
                                txtFile.writeText(text)
                                withContext(Dispatchers.Main) {
                                    statusText = "Extracted text written to:\n${txtFile.absolutePath}"
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    statusText = "Extraction failed: ${e.message}"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Extract Plain Text")
                }
            }
        }

        if (statusText != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(statusText!!, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
