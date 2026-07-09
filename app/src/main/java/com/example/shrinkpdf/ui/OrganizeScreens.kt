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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
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
            ToolCard(
                title = "Delete Pages",
                subtitle = "Remove specific pages from a PDF",
                icon = Icons.Rounded.Delete,
                onClick = { onNavigate(Screen.DeletePages) }
            )
            ToolCard(
                title = "Extract Images",
                subtitle = "Extract all images embedded in a PDF",
                icon = Icons.Rounded.Image,
                onClick = { onNavigate(Screen.ExtractImages) }
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
                    onClick = { createDocLauncher.launch(com.example.shrinkpdf.logic.FileUtil.generateSuggestedName(null, "merged")) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractImagesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPdfName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
            if (documentFile != null && selectedPdfUri != null) {
                viewModel.extractImagesFromPdf(selectedPdfUri!!, documentFile, context) { extracted, errors ->
                    if (extracted > 0) {
                        Toast.makeText(context, "Extracted $extracted images successfully!", Toast.LENGTH_LONG).show()
                    } else if (errors > 0) {
                        Toast.makeText(context, "Failed to extract images ($errors errors).", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "No images found in this PDF.", Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            selectedPdfName = com.example.shrinkpdf.utils.FileUtils.getFileName(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extract Images", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (selectedPdfUri != null) {
                ExtendedFloatingActionButton(
                    onClick = { directoryPickerLauncher.launch(null) },
                    icon = { Icon(Icons.Rounded.Image, contentDescription = "Extract") },
                    text = { Text("Extract Images") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedPdfUri == null) {
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Select a PDF to extract all embedded images.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select PDF")
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(selectedPdfName ?: "Document.pdf", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Ready to extract all images. Tap the button below and choose an output folder.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) }) {
                                Text("Change PDF")
                            }
                        }
                    }
                }
            }

            if (uiState is MainViewModel.UiState.Processing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Extracting images...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePagesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler { onBack() }
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<PdfItem?>(null) }
    var pagesToDelete by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = com.example.shrinkpdf.utils.FileUtils.getFileName(context, uri) ?: "Document.pdf"
            selectedFile = PdfItem(uri, name)
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && selectedFile != null && pagesToDelete.isNotBlank()) {
            viewModel.deletePages(context, selectedFile!!.uri, uri, pagesToDelete)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Delete Pages") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            if (selectedFile != null && pagesToDelete.isNotBlank()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val originalName = com.example.shrinkpdf.utils.FileUtils.getFileName(context, selectedFile!!.uri) ?: "Document"
                        val suggestedName = "${originalName.substringBeforeLast(".")}_deleted.pdf"
                        createDocumentLauncher.launch(suggestedName)
                    },
                    icon = { Icon(Icons.Rounded.Delete, "Delete") },
                    text = { Text("Save Document") }
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

                Text("Pages to Delete", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                OutlinedTextField(
                    value = pagesToDelete,
                    onValueChange = { pagesToDelete = it },
                    label = { Text("e.g. 1, 3, 5-10") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Enter comma-separated page numbers or ranges.") }
                )
            }
        }
    }
}
