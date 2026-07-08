package com.example.shrinkpdf.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shrinkpdf.Screen
import com.example.shrinkpdf.ToolCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeCategoryScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler { onBack() }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Organize") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToolCard(
                title = "Merge PDFs",
                subtitle = "Combine multiple PDFs into a single document",
                icon = Icons.Rounded.Merge,
                onClick = { onNavigate(Screen.MergePdf) }
            )
            ToolCard(
                title = "Split PDF",
                subtitle = "Extract pages or separate a PDF into multiple files",
                icon = Icons.Rounded.CallSplit,
                onClick = { onNavigate(Screen.SplitPdf) }
            )
        }
    }
}

data class PdfItem(val uri: Uri, val name: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePdfScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler { onBack() }
    val context = LocalContext.current
    var selectedFiles by remember { mutableStateOf(listOf<PdfItem>()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newItems = uris.map { uri ->
                val name = com.example.shrinkpdf.utils.FileUtils.getFileName(context, uri) ?: "Document.pdf"
                PdfItem(uri, name)
            }
            selectedFiles = (selectedFiles + newItems).distinctBy { it.uri }
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            viewModel.mergePdfs(context, selectedFiles.map { it.uri }, destUri)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Merge PDFs") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            if (selectedFiles.size >= 2) {
                ExtendedFloatingActionButton(
                    onClick = { createDocLauncher.launch("merged_document.pdf") },
                    icon = { Icon(Icons.Rounded.Merge, "Merge") },
                    text = { Text("Merge") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                Text("Select PDFs")
            }

            if (selectedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select at least 2 PDFs to merge", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text("Long press and drag to reorder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        items = selectedFiles,
                        key = { _, item -> item.uri.toString() }
                    ) { _, item ->
                        var isDragging by remember { mutableStateOf(false) }
                        var accumulatedY by remember { mutableStateOf(0f) }
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .shadow(elevation, RoundedCornerShape(12.dp))
                                .pointerInput(item) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { 
                                            isDragging = true 
                                            accumulatedY = 0f
                                        },
                                        onDragEnd = { isDragging = false },
                                        onDragCancel = { isDragging = false },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            accumulatedY += dragAmount.y
                                            val currentIndex = selectedFiles.indexOf(item)
                                            if (currentIndex != -1) {
                                                if (accumulatedY > 150f && currentIndex < selectedFiles.size - 1) {
                                                    val list = selectedFiles.toMutableList()
                                                    val temp = list[currentIndex]
                                                    list[currentIndex] = list[currentIndex + 1]
                                                    list[currentIndex + 1] = temp
                                                    selectedFiles = list
                                                    accumulatedY = 0f
                                                } else if (accumulatedY < -150f && currentIndex > 0) {
                                                    val list = selectedFiles.toMutableList()
                                                    val temp = list[currentIndex]
                                                    list[currentIndex] = list[currentIndex - 1]
                                                    list[currentIndex - 1] = temp
                                                    selectedFiles = list
                                                    accumulatedY = 0f
                                                }
                                            }
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.DragHandle, contentDescription = "Drag", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(item.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitPdfScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler { onBack() }
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<PdfItem?>(null) }
    var extractMode by remember { mutableStateOf(0) } // 0 = All, 1 = Custom Range
    var customRange by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = com.example.shrinkpdf.utils.FileUtils.getFileName(context, uri) ?: "Document.pdf"
            selectedFile = PdfItem(uri, name)
        }
    }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null && selectedFile != null) {
            val range = if (extractMode == 1) customRange else null
            viewModel.splitPdf(context, selectedFile!!.uri, treeUri, range)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Split PDF") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            if (selectedFile != null) {
                ExtendedFloatingActionButton(
                    onClick = { directoryPickerLauncher.launch(null) },
                    icon = { Icon(Icons.Rounded.CallSplit, "Split") },
                    text = { Text("Split into Folder") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select PDF")
            }

            if (selectedFile != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(selectedFile!!.name, fontWeight = FontWeight.Bold)
                    }
                }

                Text("Extraction Options", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = extractMode == 0, onClick = { extractMode = 0 })
                    Text("Extract all pages as individual files", modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = extractMode == 1, onClick = { extractMode = 1 })
                    Text("Extract custom range", modifier = Modifier.padding(start = 8.dp))
                }

                if (extractMode == 1) {
                    OutlinedTextField(
                        value = customRange,
                        onValueChange = { customRange = it },
                        label = { Text("e.g. 1-3, 5, 7-10") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
