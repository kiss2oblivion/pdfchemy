package com.pdfchemy.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfchemy.app.R
import com.pdfchemy.app.logic.DrawingPath
import com.pdfchemy.app.logic.DrawingPoint
import com.pdfchemy.app.logic.EditorTool
import com.pdfchemy.app.logic.FileUtil
import com.pdfchemy.app.logic.PageModification
import com.pdfchemy.app.logic.PdfEditor
import com.pdfchemy.app.logic.ShareUtil
import com.pdfchemy.app.logic.StampAnnotation
import com.pdfchemy.app.logic.StampType
import com.pdfchemy.app.logic.TextAnnotation
import com.pdfchemy.app.utils.FileUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    viewModel: MainViewModel,
    initialPdfUri: Uri? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    BackHandler { onBack() }

    var selectedPdfUri by remember { mutableStateOf<Uri?>(initialPdfUri) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPage by remember { mutableStateOf(false) }

    // Page modifications map (PageIndex -> PageModification)
    val pageModifications = remember { mutableStateMapOf<Int, PageModification>() }

    // Active Editor Tool & Tool Settings
    var activeTool by remember { mutableStateOf(EditorTool.VIEW) }
    var selectedColor by remember { mutableStateOf(Color(0xFFD32F2F)) } // Red default
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var selectedStampType by remember { mutableStateOf(StampType.APPROVED) }

    // Active In-Progress Stroke
    var currentStrokePoints by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }

    // Dialogs & Sheets
    var showTextDialog by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf<Offset?>(null) }
    var textInputContent by remember { mutableStateOf("") }
    var textColorOption by remember { mutableStateOf(Color.Black) }

    var showStampPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var exportedPdfUri by remember { mutableStateOf<Uri?>(null) }

    // Load initial PDF bounds / page count
    LaunchedEffect(selectedPdfUri) {
        selectedPdfUri?.let { uri ->
            totalPages = PdfEditor.getPageCount(context, uri)
            currentPageIndex = 0
            pageModifications.clear()
        }
    }

    // Render current page when index changes
    LaunchedEffect(selectedPdfUri, currentPageIndex, totalPages) {
        selectedPdfUri?.let { uri ->
            if (totalPages > 0 && currentPageIndex in 0 until totalPages) {
                isRenderingPage = true
                currentPageBitmap = PdfEditor.renderPageBitmap(context, uri, currentPageIndex, targetWidth = 1080)
                isRenderingPage = false
            }
        }
    }

    val currentMod = pageModifications[currentPageIndex] ?: PageModification(pageIndex = currentPageIndex)

    // PDF File Picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
        }
    }

    // Save Modified PDF Launcher
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null && selectedPdfUri != null) {
            viewModel.exportEditedPdf(
                context = context,
                sourceUri = selectedPdfUri!!,
                destUri = destUri,
                modifications = pageModifications
            ) { success ->
                if (success) {
                    exportedPdfUri = destUri
                    showSuccessDialog = true
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedPdfUri != null) FileUtils.getFileName(context, selectedPdfUri!!) ?: stringResource(R.string.menu_edit_pdf) else stringResource(R.string.menu_edit_pdf),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (totalPages > 0) {
                            Text(
                                text = stringResource(R.string.editor_page_indicator, currentPageIndex + 1, totalPages),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                },
                actions = {
                    if (selectedPdfUri != null && totalPages > 0) {
                        // Undo Last Stroke / Annotation
                        IconButton(
                            onClick = {
                                val mod = pageModifications[currentPageIndex] ?: PageModification(pageIndex = currentPageIndex)
                                if (mod.drawings.isNotEmpty()) {
                                    pageModifications[currentPageIndex] = mod.copy(drawings = mod.drawings.dropLast(1))
                                } else if (mod.textAnnotations.isNotEmpty()) {
                                    pageModifications[currentPageIndex] = mod.copy(textAnnotations = mod.textAnnotations.dropLast(1))
                                } else if (mod.stamps.isNotEmpty()) {
                                    pageModifications[currentPageIndex] = mod.copy(stamps = mod.stamps.dropLast(1))
                                }
                            },
                            enabled = currentMod.drawings.isNotEmpty() || currentMod.textAnnotations.isNotEmpty() || currentMod.stamps.isNotEmpty()
                        ) {
                            Icon(Icons.Rounded.Undo, contentDescription = stringResource(R.string.action_undo))
                        }

                        // Rotate Current Page
                        IconButton(
                            onClick = {
                                val mod = pageModifications[currentPageIndex] ?: PageModification(pageIndex = currentPageIndex)
                                pageModifications[currentPageIndex] = mod.copy(rotationDegrees = (mod.rotationDegrees + 90) % 360)
                            }
                        ) {
                            Icon(Icons.Rounded.RotateRight, contentDescription = stringResource(R.string.action_rotate))
                        }

                        // Save Modified PDF
                        FilledTonalButton(
                            onClick = {
                                val suggestedName = FileUtil.generateSuggestedName(selectedPdfUri, "edited", "Document", "pdf")
                                savePdfLauncher.launch(suggestedName)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.save), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedPdfUri != null && totalPages > 0) {
                EditorBottomBar(
                    activeTool = activeTool,
                    onToolSelect = { tool ->
                        activeTool = tool
                        if (tool == EditorTool.STAMP) showStampPicker = true
                    },
                    selectedColor = selectedColor,
                    onColorClick = { showColorPicker = true },
                    currentPage = currentPageIndex,
                    totalPages = totalPages,
                    onPrevPage = { if (currentPageIndex > 0) currentPageIndex-- },
                    onNextPage = { if (currentPageIndex < totalPages - 1) currentPageIndex++ }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (selectedPdfUri == null) {
                // Empty Picker View
                EmptyPdfPickerView(
                    onPickClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                )
            } else if (isRenderingPage || currentPageBitmap == null) {
                // Loading Page Spinner
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.editor_rendering_page, currentPageIndex + 1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Interactive PDF Canvas
                var canvasSize by remember { mutableStateOf(IntSize.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(currentPageBitmap!!.width.toFloat() / currentPageBitmap!!.height.toFloat())
                            .shadow(12.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .rotate(currentMod.rotationDegrees.toFloat())
                            .onSizeChanged { canvasSize = it }
                            .pointerInput(activeTool, currentPageIndex) {
                                when (activeTool) {
                                    EditorTool.PEN, EditorTool.HIGHLIGHTER -> {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                if (canvasSize.width > 0 && canvasSize.height > 0) {
                                                    currentStrokePoints = listOf(
                                                        DrawingPoint(offset.x / canvasSize.width, offset.y / canvasSize.height)
                                                    )
                                                }
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                if (canvasSize.width > 0 && canvasSize.height > 0) {
                                                    val pt = DrawingPoint(
                                                        (change.position.x / canvasSize.width).coerceIn(0f, 1f),
                                                        (change.position.y / canvasSize.height).coerceIn(0f, 1f)
                                                    )
                                                    currentStrokePoints = currentStrokePoints + pt
                                                }
                                            },
                                            onDragEnd = {
                                                if (currentStrokePoints.size >= 2) {
                                                    val newPath = DrawingPath(
                                                        points = currentStrokePoints,
                                                        color = if (activeTool == EditorTool.HIGHLIGHTER) selectedColor.copy(alpha = 0.45f).hashCode() else selectedColor.hashCode(),
                                                        strokeWidth = if (activeTool == EditorTool.HIGHLIGHTER) strokeWidth * 2.5f else strokeWidth,
                                                        isHighlighter = activeTool == EditorTool.HIGHLIGHTER
                                                    )
                                                    val mod = pageModifications[currentPageIndex] ?: PageModification(pageIndex = currentPageIndex)
                                                    pageModifications[currentPageIndex] = mod.copy(drawings = mod.drawings + newPath)
                                                }
                                                currentStrokePoints = emptyList()
                                            }
                                        )
                                    }
                                    EditorTool.TEXT -> {
                                        detectTapGestures { offset ->
                                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                                pendingTextPosition = offset
                                                showTextDialog = true
                                            }
                                        }
                                    }
                                    EditorTool.STAMP -> {
                                        detectTapGestures { offset ->
                                            if (canvasSize.width > 0 && canvasSize.height > 0) {
                                                val xR = offset.x / canvasSize.width
                                                val yR = offset.y / canvasSize.height
                                                val newStamp = StampAnnotation(
                                                    type = selectedStampType,
                                                    xRatio = xR,
                                                    yRatio = yR
                                                )
                                                val mod = pageModifications[currentPageIndex] ?: PageModification(pageIndex = currentPageIndex)
                                                pageModifications[currentPageIndex] = mod.copy(stamps = mod.stamps + newStamp)
                                            }
                                        }
                                    }
                                    EditorTool.VIEW -> { /* View mode allows page inspection */ }
                                }
                            }
                    ) {
                        // 1. Render Background PDF Page Bitmap
                        Image(
                            bitmap = currentPageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // 2. Render Overlay Annotations (Drawings, Texts, Stamps)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Render Saved Drawings
                            for (drawing in currentMod.drawings) {
                                drawPathStroke(drawing, size.width, size.height)
                            }
                            // Render In-Progress Stroke
                            if (currentStrokePoints.size >= 2) {
                                val tempDrawing = DrawingPath(
                                    points = currentStrokePoints,
                                    color = if (activeTool == EditorTool.HIGHLIGHTER) selectedColor.copy(alpha = 0.45f).hashCode() else selectedColor.hashCode(),
                                    strokeWidth = if (activeTool == EditorTool.HIGHLIGHTER) strokeWidth * 2.5f else strokeWidth,
                                    isHighlighter = activeTool == EditorTool.HIGHLIGHTER
                                )
                                drawPathStroke(tempDrawing, size.width, size.height)
                            }
                        }

                        // Render Text Annotations
                        for (textAnn in currentMod.textAnnotations) {
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = (canvasSize.width * textAnn.xRatio).dp,
                                        y = (canvasSize.height * textAnn.yRatio).dp
                                    )
                                    .background(Color(textAnn.backgroundColor), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = textAnn.text,
                                    color = Color(textAnn.textColor),
                                    fontSize = textAnn.fontSize.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Render Stamps
                        for (stamp in currentMod.stamps) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(
                                        x = (canvasSize.width * stamp.xRatio - 45).dp,
                                        y = (canvasSize.height * stamp.yRatio - 18).dp
                                    )
                                    .rotate(stamp.rotation)
                                    .border(2.5.dp, Color(stamp.type.colorHex), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stamp.type.text,
                                    color = Color(stamp.type.colorHex),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADD TEXT ANNOTATION DIALOG ---
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text(stringResource(R.string.dialog_add_text_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = textInputContent,
                        onValueChange = { textInputContent = it },
                        placeholder = { Text(stringResource(R.string.dialog_add_text_placeholder)) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInputContent.isNotBlank() && pendingTextPosition != null) {
                            val newTextAnn = TextAnnotation(
                                text = textInputContent.trim(),
                                xRatio = (pendingTextPosition!!.x / 1000f).coerceIn(0.05f, 0.85f),
                                yRatio = (pendingTextPosition!!.y / 1000f).coerceIn(0.05f, 0.85f),
                                fontSize = 16f,
                                textColor = selectedColor.hashCode()
                            )
                            val mod = pageModifications[currentPageIndex] ?: PageModification(pageIndex = currentPageIndex)
                            pageModifications[currentPageIndex] = mod.copy(textAnnotations = mod.textAnnotations + newTextAnn)
                        }
                        textInputContent = ""
                        showTextDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // --- STAMP PICKER SHEET ---
    if (showStampPicker) {
        ModalBottomSheet(onDismissRequest = { showStampPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.stamps_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.stamps_picker_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(StampType.values()) { stamp ->
                        OutlinedButton(
                            onClick = {
                                selectedStampType = stamp
                                showStampPicker = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedStampType == stamp) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color(stamp.colorHex)))
                        ) {
                            Text(
                                text = stamp.text,
                                color = Color(stamp.colorHex),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- COLOR PICKER SHEET ---
    if (showColorPicker) {
        ModalBottomSheet(onDismissRequest = { showColorPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.color_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val colors = listOf(
                    Color(0xFFD32F2F), // Red
                    Color(0xFF1976D2), // Blue
                    Color(0xFF388E3C), // Green
                    Color(0xFFFBC02D), // Yellow / Neon
                    Color(0xFFE91E63), // Pink
                    Color(0xFF7B1FA2), // Purple
                    Color(0xFF000000)  // Black
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable {
                                    selectedColor = c
                                    showColorPicker = false
                                }
                                .border(
                                    width = if (selectedColor == c) 3.dp else 1.dp,
                                    color = if (selectedColor == c) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- EXPORT SUCCESS DIALOG ---
    if (showSuccessDialog && exportedPdfUri != null) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
            title = { Text(stringResource(R.string.editor_export_success_title)) },
            text = { Text(stringResource(R.string.editor_export_success_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        ShareUtil.shareFile(context, exportedPdfUri!!, "application/pdf")
                    }
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.share))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

private fun DrawScope.drawPathStroke(drawing: DrawingPath, canvasWidth: Float, canvasHeight: Float) {
    if (drawing.points.size < 2) return
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
        width = drawing.strokeWidth,
        cap = androidx.compose.ui.graphics.StrokeCap.Round,
        join = androidx.compose.ui.graphics.StrokeJoin.Round
    )

    val path = androidx.compose.ui.graphics.Path()
    val p0 = drawing.points[0]
    path.moveTo(p0.x * canvasWidth, p0.y * canvasHeight)
    for (i in 1 until drawing.points.size) {
        val p = drawing.points[i]
        path.lineTo(p.x * canvasWidth, p.y * canvasHeight)
    }
    drawPath(path = path, color = Color(drawing.color), style = stroke)
}

@Composable
fun EmptyPdfPickerView(onPickClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Draw,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = stringResource(R.string.editor_select_pdf_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.editor_select_pdf_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onPickClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.editor_open_pdf_btn), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EditorBottomBar(
    activeTool: EditorTool,
    onToolSelect: (EditorTool) -> Unit,
    selectedColor: Color,
    onColorClick: () -> Unit,
    currentPage: Int,
    totalPages: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Tools Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorToolButton(
                    icon = Icons.Rounded.PanTool,
                    label = stringResource(R.string.tool_view),
                    selected = activeTool == EditorTool.VIEW,
                    onClick = { onToolSelect(EditorTool.VIEW) }
                )
                EditorToolButton(
                    icon = Icons.Rounded.Draw,
                    label = stringResource(R.string.tool_pen),
                    selected = activeTool == EditorTool.PEN,
                    onClick = { onToolSelect(EditorTool.PEN) }
                )
                EditorToolButton(
                    icon = Icons.Rounded.Highlight,
                    label = stringResource(R.string.tool_highlight),
                    selected = activeTool == EditorTool.HIGHLIGHTER,
                    onClick = { onToolSelect(EditorTool.HIGHLIGHTER) }
                )
                EditorToolButton(
                    icon = Icons.Rounded.TextFields,
                    label = stringResource(R.string.tool_text),
                    selected = activeTool == EditorTool.TEXT,
                    onClick = { onToolSelect(EditorTool.TEXT) }
                )
                EditorToolButton(
                    icon = Icons.Rounded.Approval,
                    label = stringResource(R.string.tool_stamp),
                    selected = activeTool == EditorTool.STAMP,
                    onClick = { onToolSelect(EditorTool.STAMP) }
                )

                // Color Picker Quick Dot
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                        .clickable(onClick = onColorClick)
                        .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Page Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevPage, enabled = currentPage > 0) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.action_prev_page))
                }

                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onNextPage, enabled = currentPage < totalPages - 1) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.action_next_page))
                }
            }
        }
    }
}

@Composable
fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
    }
}
