package com.pdfchemy.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.CallMerge
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfchemy.desktop.engine.DesktopPdfEngine
import com.pdfchemy.desktop.engine.PageItemSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

enum class DesktopNavTab(val label: String, val icon: ImageVector) {
    HOME("All Tools", Icons.Rounded.Dashboard),
    COMPRESS("Compress", Icons.Rounded.Speed),
    ORGANIZE("Page Studio", Icons.Rounded.GridView),
    CONVERT("Convert", Icons.AutoMirrored.Rounded.Notes),
    READER("Reader", Icons.AutoMirrored.Rounded.MenuBook),
    SECURITY("Security", Icons.Rounded.Lock),
    BATCH("Batch Queue", Icons.Rounded.Layers)
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

    var showDonationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialFile) {
        if (initialFile != null) {
            selectedFile = initialFile
        }
    }

    if (showDonationDialog) {
        DonationDialog(onDismiss = { showDonationDialog = false })
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
                    // Local-First Privacy Badge
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

                    // Donate / Support Button
                    FilledTonalButton(
                        onClick = { showDonationDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF4081)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF4081))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Donate", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

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
                modifier = Modifier.width(110.dp),
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

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Sponsor Heart
                FilledTonalIconButton(
                    onClick = { showDonationDialog = true },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                        contentColor = Color(0xFFFF4081)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = "Support Development")
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
                        onSelectFile = { selectedFile = it },
                        onOpenTipJar = { showDonationDialog = true }
                    )
                    DesktopNavTab.COMPRESS -> CompressView(selectedFile, onFileChange = { selectedFile = it }, onOpenTipJar = { showDonationDialog = true })
                    DesktopNavTab.ORGANIZE -> PageStudioView(selectedFile, onFileChange = { selectedFile = it }, onOpenTipJar = { showDonationDialog = true })
                    DesktopNavTab.CONVERT -> ConvertView(selectedFile, onFileChange = { selectedFile = it }, onOpenTipJar = { showDonationDialog = true })
                    DesktopNavTab.READER -> ReaderView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.SECURITY -> SecurityView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.BATCH -> BatchQueueView(onOpenTipJar = { showDonationDialog = true })
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. HOME VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun HomeView(
    onSelectTab: (DesktopNavTab) -> Unit,
    selectedFile: File?,
    onSelectFile: (File) -> Unit,
    onOpenTipJar: () -> Unit = {}
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
                        "All operations execute 100% on this computer. Zero cloud uploads, zero telemetry. Multi-core desktop speed without limits.",
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

        Text("Document Superpowers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // 3-Column Tool Cards Grid
        val tools = listOf(
            ToolItem("Visual Page Studio", "Rearrange, rotate, delete, and duplicate pages with instant visual thumbnail cards.", Icons.Rounded.GridView, DesktopNavTab.ORGANIZE),
            ToolItem("Smart Compressor", "Shrink to exact target sizes (e.g. under 2MB for email/portals) or use quality presets.", Icons.Rounded.Speed, DesktopNavTab.COMPRESS),
            ToolItem("Images ⇄ PDF Studio", "Compile photos/receipts into PDFs, or extract all PDF pages as high-res PNG/JPG.", Icons.Rounded.Collections, DesktopNavTab.CONVERT),
            ToolItem("Multi-Core Batch Queue", "Drop 50+ PDFs and process them in parallel across your desktop CPU cores.", Icons.Rounded.Layers, DesktopNavTab.BATCH),
            ToolItem("Reflow Reader", "Read PDFs and EPUBs with continuous typography, dark mode & sepia themes.", Icons.AutoMirrored.Rounded.MenuBook, DesktopNavTab.READER),
            ToolItem("Encrypt & Protect", "Lock documents with 128/256-bit AES encryption or remove passwords safely.", Icons.Rounded.Lock, DesktopNavTab.SECURITY)
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
                                .height(145.dp),
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
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

        // The Developer's Lifetime Manifesto Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AllInclusive, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            "The Lifetime Promise: From The People, To The People",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "100% Free of Charge • Direct Developer Attention",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "\"Lifetime of updates until I personally die — and 100% free of charge. All you have to do is make a solid, valid request and it will be done and implemented; cuz it's from the people to the people.\n\nIt may or may not be the same as a corpo app would do, but at least it's gonna be free and I will make it as best as I possibly can. If I can't, well I can't and that's that — at least you have an option, oh you enigmatic edge case that you are.\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "— Andrei Ioan Cucoș (John), Developer",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = onOpenTipJar,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                                contentColor = Color(0xFFFF4081)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF4081))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("☕ Tip Jar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                try {
                                    if (java.awt.Desktop.isDesktopSupported()) {
                                        java.awt.Desktop.getDesktop().browse(java.net.URI("https://github.com/cucosandrei/pdfchemy/issues/new"))
                                    }
                                } catch (_: Exception) {}
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Request a Feature / Edge Case", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private data class ToolItem(val title: String, val desc: String, val icon: ImageVector, val tab: DesktopNavTab)

// -------------------------------------------------------------------------------------------------
// 2. VISUAL PAGE STUDIO (GAP 1: THE PDF ARRANGER KILLER)
// -------------------------------------------------------------------------------------------------
@Composable
private fun PageStudioView(file: File?, onFileChange: (File) -> Unit, onOpenTipJar: () -> Unit = {}) {
    var pageItems by remember { mutableStateOf<List<PageItemSpec>>(emptyList()) }
    val thumbnails = remember { mutableStateMapOf<Int, ImageBitmap>() }
    var isLoadingThumbnails by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Load pages when file changes
    LaunchedEffect(file) {
        if (file != null) {
            thumbnails.clear()
            isLoadingThumbnails = true
            scope.launch(Dispatchers.IO) {
                try {
                    val count = DesktopPdfEngine.getPageCount(file)
                    val specs = (0 until count).map { PageItemSpec(originalPageIndex = it, rotation = 0) }
                    withContext(Dispatchers.Main) {
                        pageItems = specs
                    }
                    // Render thumbnails asynchronously in background
                    for (i in 0 until count) {
                        val bimg = DesktopPdfEngine.renderThumbnail(file, i, targetWidth = 240)
                        val bitmap = bimg.toComposeImageBitmap()
                        withContext(Dispatchers.Main) {
                            thumbnails[i] = bitmap
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        statusText = "Error loading document: ${e.message}"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoadingThumbnails = false
                    }
                }
            }
        } else {
            pageItems = emptyList()
            thumbnails.clear()
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Studio Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Visual Page Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (file != null) "${file.name} — ${pageItems.size} pages" else "Load a document to organize and reorder pages visually",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (file != null && pageItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        // Rotate all pages 90 deg clockwise
                        pageItems = pageItems.map { it.copy(rotation = (it.rotation + 90) % 360) }
                    }) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotate All 90°")
                    }

                    Button(
                        onClick = {
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_organized.pdf") ?: return@Button
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DesktopPdfEngine.saveReorderedPdf(file, outFile, pageItems)
                                    withContext(Dispatchers.Main) {
                                        statusText = "Document organized and saved to:\n${outFile.absolutePath}"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        statusText = "Save failed: ${e.message}"
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Organized PDF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (statusText != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusText!!, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (statusText!!.contains("saved", ignoreCase = true) || statusText!!.contains("organized", ignoreCase = true)) {
                        FilledTonalButton(
                            onClick = onOpenTipJar,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                                contentColor = Color(0xFFFF4081)
                            )
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4081))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("☕ Tip Jar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (file == null) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select a PDF to View & Rearrange Pages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rotate, reorder, delete, and export pages visually without terminal commands.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = {
                        val f = DesktopFileDialog.openPdf()
                        if (f != null) onFileChange(f)
                    }) {
                        Text("Open Document")
                    }
                }
            }
        } else {
            // Visual Page Cards Grid
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                val columns = 4
                val rows = (pageItems.size + columns - 1) / columns

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            for (c in 0 until columns) {
                                val index = r * columns + c
                                if (index < pageItems.size) {
                                    val item = pageItems[index]
                                    val thumb = thumbnails[item.originalPageIndex]

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Page Number & Rotation Badge
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("Page ${index + 1}", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                if (item.rotation != 0) {
                                                    Text("${item.rotation}°", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Thumbnail Preview Image
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (thumb != null) {
                                                    Image(
                                                        bitmap = thumb,
                                                        contentDescription = "Page ${index + 1}",
                                                        modifier = Modifier.fillMaxSize().padding(4.dp)
                                                    )
                                                } else {
                                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                                }
                                            }

                                            // Page Action Controls
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Move Left
                                                IconButton(
                                                    onClick = {
                                                        if (index > 0) {
                                                            val mutable = pageItems.toMutableList()
                                                            val tmp = mutable[index]
                                                            mutable[index] = mutable[index - 1]
                                                            mutable[index - 1] = tmp
                                                            pageItems = mutable
                                                        }
                                                    },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Move Left")
                                                }

                                                // Rotate 90 deg
                                                IconButton(
                                                    onClick = {
                                                        val mutable = pageItems.toMutableList()
                                                        mutable[index] = item.copy(rotation = (item.rotation + 90) % 360)
                                                        pageItems = mutable
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate")
                                                }

                                                // Duplicate Page
                                                IconButton(
                                                    onClick = {
                                                        val mutable = pageItems.toMutableList()
                                                        mutable.add(index + 1, item.copy())
                                                        pageItems = mutable
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate")
                                                }

                                                // Delete Page
                                                IconButton(
                                                    onClick = {
                                                        pageItems = pageItems.filterIndexed { i, _ -> i != index }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                }

                                                // Move Right
                                                IconButton(
                                                    onClick = {
                                                        if (index < pageItems.size - 1) {
                                                            val mutable = pageItems.toMutableList()
                                                            val tmp = mutable[index]
                                                            mutable[index] = mutable[index + 1]
                                                            mutable[index + 1] = tmp
                                                            pageItems = mutable
                                                        }
                                                    },
                                                    enabled = index < pageItems.size - 1,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Move Right")
                                                }
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
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. SMART TARGET-SIZE COMPRESSOR (GAP 2: BEAT CLOUD PAYWALLS)
// -------------------------------------------------------------------------------------------------
@Composable
private fun CompressView(file: File?, onFileChange: (File) -> Unit, onOpenTipJar: () -> Unit = {}) {
    var isTargetSizeMode by remember { mutableStateOf(false) }
    var targetMb by remember { mutableFloatStateOf(2.0f) }
    var qualityLevel by remember { mutableFloatStateOf(0.7f) }
    var targetDpi by remember { mutableFloatStateOf(140f) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Smart Document Compressor", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        if (file == null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(260.dp),
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

            // Mode Selector: Presets vs Target File Size
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = !isTargetSizeMode,
                    onClick = { isTargetSizeMode = false },
                    label = { Text("Quality Presets") },
                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = isTargetSizeMode,
                    onClick = { isTargetSizeMode = true },
                    label = { Text("Target File Size (e.g. Under 2MB)") },
                    leadingIcon = { Icon(Icons.Rounded.Straighten, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (isTargetSizeMode) {
                // Target File Size Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Desired Maximum Size: ${DecimalFormat("#0.0").format(targetMb)} MB", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("PDFchemy will automatically tune DPI and compression to guarantee the file fits under this size for email or portal uploads.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Slider(
                            value = targetMb,
                            onValueChange = { targetMb = it },
                            valueRange = 0.5f..10.0f,
                            steps = 19
                        )

                        // Quick Presets
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5f to "500 KB", 1.0f to "1 MB", 2.0f to "2 MB (Government/Email)", 5.0f to "5 MB").forEach { (mb, label) ->
                                AssistChip(
                                    onClick = { targetMb = mb },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Presets Mode
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
            }

            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(progressText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            if (resultText != null) {
                Surface(
                    color = if (resultText!!.startsWith("Success")) Color(0xFF00E676).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            resultText!!,
                            modifier = Modifier.weight(1f),
                            color = if (resultText!!.startsWith("Success")) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        if (resultText!!.startsWith("Success")) {
                            FilledTonalButton(
                                onClick = onOpenTipJar,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                                    contentColor = Color(0xFFFF4081)
                                )
                            ) {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF4081))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("☕ Tip Jar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_compressed.pdf") ?: return@Button
                    isProcessing = true
                    resultText = null
                    scope.launch(Dispatchers.IO) {
                        try {
                            val newSize = if (isTargetSizeMode) {
                                val targetBytes = (targetMb * 1024 * 1024).toLong()
                                DesktopPdfEngine.compressToTargetSize(file, outFile, targetBytes) { msg ->
                                    progressText = msg
                                }
                            } else {
                                DesktopPdfEngine.compressPdf(file, outFile, targetDpi, qualityLevel) { cur, tot ->
                                    progressText = "Compressing page $cur of $tot..."
                                }
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
                Text("Start Smart Compression", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. CONVERT STUDIO (GAP 3: IMAGES ⇄ PDF CONVERSION)
// -------------------------------------------------------------------------------------------------
@Composable
private fun ConvertView(file: File?, onFileChange: (File) -> Unit, onOpenTipJar: () -> Unit = {}) {
    var statusText by remember { mutableStateOf<String?>(null) }
    var selectedImages by remember { mutableStateOf<List<File>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Document & Image Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Images to PDF Card
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
                        Text("Images to PDF", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Compile camera photos, receipts, or screenshots into a crisp PDF document.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {
                        val imgs = DesktopFileDialog.openMultipleImages()
                        if (imgs.isNotEmpty()) selectedImages = selectedImages + imgs
                    }) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Images")
                    }
                }

                if (selectedImages.isNotEmpty()) {
                    Text("${selectedImages.size} images selected:", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        selectedImages.forEachIndexed { i, img ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${i + 1}. ${img.name}", style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { selectedImages = selectedImages.filterIndexed { idx, _ -> idx != i } }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val outFile = DesktopFileDialog.savePdf(suggestedName = "compiled_images.pdf") ?: return@Button
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        DesktopPdfEngine.imagesToPdf(selectedImages, outFile)
                                        withContext(Dispatchers.Main) {
                                            statusText = "Images compiled into PDF:\n${outFile.absolutePath}"
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            statusText = "Compilation failed: ${e.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create PDF (${selectedImages.size} images)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // PDF to Images Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("PDF to High-Res Images (PNG)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Extract every page of the active PDF as standalone high-resolution images.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        val folder = DesktopFileDialog.chooseDirectory() ?: return@Button
                        scope.launch(Dispatchers.IO) {
                            try {
                                val files = DesktopPdfEngine.extractPagesToImages(file, folder, format = "png", dpi = 150f)
                                withContext(Dispatchers.Main) {
                                    statusText = "Extracted ${files.size} pages as PNG images to:\n${folder.absolutePath}"
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
                    Icon(Icons.Rounded.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Pages to Images")
                }
            }
        }

        // Extract Plain Text Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Extract Plain Text (.txt)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    Spacer(modifier = Modifier.width(8.dp))
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusText!!, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    FilledTonalButton(
                        onClick = onOpenTipJar,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF4081)
                        )
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4081))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("☕ Tip Jar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 5. BATCH QUEUE VIEW (GAP 4: MULTI-CORE BULK PROCESSING)
// -------------------------------------------------------------------------------------------------
@Composable
private fun BatchQueueView(onOpenTipJar: () -> Unit = {}) {
    var queueFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Multi-Core Batch Queue", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                        Text("Batch Document Processing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Process 10, 20, or 50+ PDFs simultaneously across all CPU cores.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {
                        val picked = DesktopFileDialog.openMultiplePdfs()
                        if (picked.isNotEmpty()) queueFiles = queueFiles + picked
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add PDF Files")
                    }
                }

                if (queueFiles.isNotEmpty()) {
                    Text("${queueFiles.size} documents in queue:", fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        queueFiles.forEachIndexed { idx, f ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}. ${f.name} (${formatFileSize(f.length())})", style = MaterialTheme.typography.bodyMedium)
                                    IconButton(onClick = { queueFiles = queueFiles.filterIndexed { i, _ -> i != idx } }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }

                    if (isProcessing) {
                        LinearProgressIndicator(progress = { currentProgress }, modifier = Modifier.fillMaxWidth())
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                val outDir = DesktopFileDialog.chooseDirectory() ?: return@Button
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        var count = 0
                                        queueFiles.forEachIndexed { i, f ->
                                            val outFile = File(outDir, "${f.nameWithoutExtension}_compressed.pdf")
                                            DesktopPdfEngine.compressPdf(f, outFile)
                                            count++
                                            currentProgress = count.toFloat() / queueFiles.size.toFloat()
                                        }
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            statusText = "Batch compression complete! ${queueFiles.size} files saved to:\n${outDir.absolutePath}"
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            statusText = "Batch failed: ${e.message}"
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Batch Compress All")
                        }

                        OutlinedButton(
                            onClick = {
                                val outFile = DesktopFileDialog.savePdf(suggestedName = "batch_merged.pdf") ?: return@OutlinedButton
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        DesktopPdfEngine.mergePdfs(queueFiles, outFile)
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            statusText = "Successfully merged ${queueFiles.size} files into:\n${outFile.absolutePath}"
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            statusText = "Batch merge failed: ${e.message}"
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.CallMerge, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Merge All Into One")
                        }
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusText!!, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    FilledTonalButton(
                        onClick = onOpenTipJar,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFF4081).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF4081)
                        )
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4081))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("☕ Tip Jar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. READER VIEW
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

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Text("Open Document")
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
// 7. SECURITY VIEW
// -------------------------------------------------------------------------------------------------
@Composable
private fun SecurityView(file: File?, onFileChange: (File) -> Unit) {
    var password by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                                        statusText = "Document encrypted and saved to:\n${outFile.absolutePath}"
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
                                        statusText = "Password removed! Saved to:\n${outFile.absolutePath}"
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

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

@Composable
private fun DonationDialog(onDismiss: () -> Unit) {
    var copiedToClipboard by remember { mutableStateOf(false) }

    fun openBrowser(url: String) {
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(java.net.URI(url))
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun copyToClipboard(text: String) {
        try {
            val selection = java.awt.datatransfer.StringSelection(text)
            java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            copiedToClipboard = true
        } catch (e: Exception) {
            // Ignored
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFFFF4081).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFFF4081), modifier = Modifier.size(32.dp))
            }
        },
        title = {
            Text(
                "Support PDFchemy",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .width(480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.AllInclusive, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("Lifetime Updates • From The People, To The People", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "\"Lifetime of updates until I personally die and free of charge; and all you have to do is make a solid valid request and it will be done and implemented; cuz it's from the people to the people; it may or may not be the same as a corpo app would do but at least it's gonna be free and I will make it as best as I possibly can; if I can't well I can't and that's that at least you have an option oh you enigmatic edge case that you are.\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("— Andrei Ioan Cucoș (John), Developer", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Quick Amount Tier Buttons
                Text("Choose a Support Tier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("☕ $3", "Coffee", "https://buymeacoffee.com/cucosandrei"),
                        Triple("🍕 $10", "Lunch", "https://paypal.me/cucosandrei/10"),
                        Triple("💖 $25", "Sponsor", "https://paypal.me/cucosandrei/25"),
                        Triple("⭐ $50", "Patron", "https://paypal.me/cucosandrei/50")
                    ).forEach { (label, sub, url) ->
                        Card(
                            onClick = { openBrowser(url) },
                            modifier = Modifier.weight(1f).height(65.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Payment Methods
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Buy Me a Coffee Button
                    Button(
                        onClick = { openBrowser("https://buymeacoffee.com/cucosandrei") },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDD00), contentColor = Color(0xFF000000))
                    ) {
                        Icon(Icons.Rounded.Coffee, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buy Me a Coffee (Card / Apple & Google Pay)", fontWeight = FontWeight.Bold)
                    }

                    // PayPal Button
                    Button(
                        onClick = { openBrowser("https://paypal.me/cucosandrei") },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0070BA), contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Payment, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Donate via PayPal / Credit Card", fontWeight = FontWeight.Bold)
                    }

                    // GitHub Sponsors
                    OutlinedButton(
                        onClick = { openBrowser("https://github.com/sponsors/cucosandrei") },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFFFF4081))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sponsor on GitHub", fontWeight = FontWeight.Bold)
                    }
                }

                // Copy Support / PayPal Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { copyToClipboard("cucosandreiioan@gmail.com") }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedToClipboard) "Copied to clipboard!" else "PayPal / Contact: cucosandreiioan@gmail.com", fontSize = 12.sp)
                    }
                }
            }
        }
    )
}
