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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.material.icons.rounded.RotateRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shrinkpdf.Screen
import com.example.shrinkpdf.ToolCard
import android.widget.Toast
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.ui.res.stringResource
import com.example.shrinkpdf.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeCategoryScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler { onBack() }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.organize)) },
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
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ToolCard(
                    title = stringResource(R.string.menu_merge),
                    subtitle = stringResource(R.string.menu_merge_desc),
                    icon = Icons.Rounded.Merge,
                    onClick = { onNavigate(Screen.MergePdf) }
                )
            }
            item {
                ToolCard(
                    title = stringResource(R.string.menu_split),
                    subtitle = stringResource(R.string.menu_split_desc),
                    icon = Icons.Rounded.CallSplit,
                    onClick = { onNavigate(Screen.SplitPdf) }
                )
            }
            item {
                ToolCard(
                    title = stringResource(R.string.menu_delete_pages),
                    subtitle = stringResource(R.string.menu_delete_pages_desc),
                    icon = Icons.Rounded.Delete,
                    onClick = { onNavigate(Screen.DeletePages) }
                )
            }
            item {
                ToolCard(
                    title = stringResource(R.string.menu_extract_images),
                    subtitle = stringResource(R.string.menu_extract_images_desc),
                    icon = Icons.Rounded.Image,
                    onClick = { onNavigate(Screen.ExtractImages) }
                )
            }
            item {
                ToolCard(
                    title = stringResource(R.string.menu_rotate_pages),
                    subtitle = stringResource(R.string.menu_rotate_pages_desc),
                    icon = Icons.Rounded.RotateRight,
                    onClick = { onNavigate(Screen.RotatePdf) }
                )
            }
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
    var showHistorySheet by remember { mutableStateOf(false) }
    val historyRepo = remember { com.example.shrinkpdf.logic.HistoryRepository(context) }
    var historyItems by remember { mutableStateOf(historyRepo.getHistory()) }

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

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(stringResource(R.string.select_from_recent), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (historyItems.isEmpty()) {
                    Text(stringResource(R.string.no_recent_files_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                        items(items = historyItems) { item ->
                            val isSelected = selectedFiles.any { it.uri.toString() == item.uriString }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val uri = Uri.parse(item.uriString)
                                        if (isSelected) {
                                            selectedFiles = selectedFiles.filterNot { it.uri == uri }
                                        } else {
                                            selectedFiles = selectedFiles + PdfItem(uri, item.name)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(item.action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.merge_pdfs)) },
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
                    text = { Text(stringResource(R.string.merge)) }
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.select_pdfs))
                }
                FilledTonalButton(
                    onClick = { 
                        historyItems = historyRepo.getHistory()
                        showHistorySheet = true 
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.recent))
                }
            }

            if (selectedFiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.select_at_least_2_pdfs_to_merg), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(stringResource(R.string.long_press_and_drag_to_reorder), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
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
        if (treeUri != null) {
            val file = selectedFile ?: return@rememberLauncherForActivityResult
            val range = if (extractMode == 1) customRange else null
            viewModel.splitPdf(context, file.uri, treeUri, range)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.split_pdf)) },
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
                    text = { Text(stringResource(R.string.split_into_folder)) }
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
                Text(stringResource(R.string.select_pdf))
            }

                val file = selectedFile
                if (file != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(file.name, fontWeight = FontWeight.Bold)
                        }
                    }

                Text(stringResource(R.string.extraction_options), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = extractMode == 0, onClick = { extractMode = 0 })
                    Text(stringResource(R.string.extract_all_pages_as_individua), modifier = Modifier.padding(start = 8.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = extractMode == 1, onClick = { extractMode = 1 })
                    Text(stringResource(R.string.extract_custom_range), modifier = Modifier.padding(start = 8.dp))
                }

                if (extractMode == 1) {
                    OutlinedTextField(
                        value = customRange,
                        onValueChange = { customRange = it },
                        label = { Text(stringResource(R.string.e_g_1_3_5_7_10)) },
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
            if (documentFile != null) {
                val pdfUri = selectedPdfUri ?: return@rememberLauncherForActivityResult
                viewModel.extractImagesFromPdf(pdfUri, documentFile, context) { extracted, errors ->
                    if (extracted > 0) {
                        Toast.makeText(context, context.getString(R.string.msg_extracted_images, extracted), Toast.LENGTH_LONG).show()
                    } else if (errors > 0) {
                        Toast.makeText(context, context.getString(R.string.msg_failed_extract_images, errors), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, context.getString(R.string.msg_no_images_found), Toast.LENGTH_SHORT).show()
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
                title = { Text(stringResource(R.string.extract_images), fontWeight = FontWeight.Bold) },
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
                    text = { Text(stringResource(R.string.extract_images)) },
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
                        Text(stringResource(R.string.select_pdf))
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
                            Text(stringResource(R.string.ready_to_extract_all_images_ta), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) }) {
                                Text(stringResource(R.string.change_pdf))
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
                        Text(stringResource(R.string.extracting_images), style = MaterialTheme.typography.bodyMedium)
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
        if (uri != null && pagesToDelete.isNotBlank()) {
            val file = selectedFile ?: return@rememberLauncherForActivityResult
            viewModel.deletePages(context, file.uri, uri, pagesToDelete)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.delete_pages)) },
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
                        val file = selectedFile ?: return@ExtendedFloatingActionButton
                        val originalName = com.example.shrinkpdf.utils.FileUtils.getFileName(context, file.uri) ?: "Document"
                        val suggestedName = "${originalName.substringBeforeLast(".")}_deleted.pdf"
                        createDocumentLauncher.launch(suggestedName)
                    },
                    icon = { Icon(Icons.Rounded.Delete, "Delete") },
                    text = { Text(stringResource(R.string.save_document)) }
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
                Text(stringResource(R.string.select_pdf))
            }

            val file = selectedFile
            if (file != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(file.name, fontWeight = FontWeight.Bold)
                    }
                }

                Text(stringResource(R.string.pages_to_delete), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                OutlinedTextField(
                    value = pagesToDelete,
                    onValueChange = { pagesToDelete = it },
                    label = { Text(stringResource(R.string.e_g_1_3_5_10)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.enter_comma_separated_page_num)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatePdfScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<androidx.documentfile.provider.DocumentFile?>(null) }
    var pageRange by remember { mutableStateOf("") }
    var rotationDegrees by remember { mutableIntStateOf(90) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val docFile = androidx.documentfile.provider.DocumentFile.fromSingleUri(context, it)
            selectedFile = docFile
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { destUri ->
            selectedFile?.uri?.let { sourceUri ->
                viewModel.rotatePdf(context, sourceUri, destUri, rotationDegrees, pageRange)
                onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.rotate_pages)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedFile != null) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        val file = selectedFile ?: return@ExtendedFloatingActionButton
                        saveFileLauncher.launch("rotated_${file.name}") 
                    },
                    icon = { Icon(Icons.Rounded.RotateRight, contentDescription = "Save") },
                    text = { Text(stringResource(R.string.rotate_save)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
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
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.select_pdf))
            }

            val file = selectedFile
            if (file != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(file.name ?: "Document", fontWeight = FontWeight.Bold)
                    }
                }

                Text(stringResource(R.string.rotation_angle), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = rotationDegrees == 90,
                        onClick = { rotationDegrees = 90 },
                        label = { Text(stringResource(R.string.str_90_cw)) }
                    )
                    FilterChip(
                        selected = rotationDegrees == 180,
                        onClick = { rotationDegrees = 180 },
                        label = { Text(stringResource(R.string.str_180)) }
                    )
                    FilterChip(
                        selected = rotationDegrees == 270,
                        onClick = { rotationDegrees = 270 },
                        label = { Text(stringResource(R.string.str_90_ccw)) }
                    )
                }

                Text(stringResource(R.string.pages_to_rotate_optional), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                OutlinedTextField(
                    value = pageRange,
                    onValueChange = { pageRange = it },
                    label = { Text(stringResource(R.string.e_g_1_3_5_10)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.leave_blank_to_rotate_all_page)) }
                )
            }
        }
    }
}
