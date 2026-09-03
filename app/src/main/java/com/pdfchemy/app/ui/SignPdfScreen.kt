package com.pdfchemy.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfchemy.app.R
import com.pdfchemy.app.logic.PlacedSignature
import com.pdfchemy.app.logic.SignatureEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignPdfScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    androidx.activity.compose.BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var savedSignatures by remember { mutableStateOf<List<Pair<String, Bitmap>>>(emptyList()) }
    var showSignaturePad by remember { mutableStateOf(false) }

    // Placed signatures on the document
    var placedSignatures by remember { mutableStateOf<List<PlacedSignature>>(emptyList()) }
    var includeDateStamp by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Load saved signatures
    LaunchedEffect(Unit) {
        savedSignatures = SignatureEngine.loadSignatures(context)
    }

    // PDF Page Renderer
    fun renderPage(uri: Uri, index: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = PdfRenderer(pfd)
                totalPages = renderer.pageCount
                if (index in 0 until totalPages) {
                    val page = renderer.openPage(index)
                    val originalWidth = page.width.coerceAtLeast(1)
                    val originalHeight = page.height.coerceAtLeast(1)
                    val scale = (1080f / originalWidth).coerceAtMost(1.5f)
                    val renderWidth = (originalWidth * scale).toInt().coerceAtLeast(1)
                    val renderHeight = (originalHeight * scale).toInt().coerceAtLeast(1)

                    val bmp = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bmp)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    withContext(Dispatchers.Main) {
                        currentPageBitmap?.recycle()
                        currentPageBitmap = bmp
                    }
                }
                renderer.close()
                pfd.close()
            } catch (e: Exception) {
                com.pdfchemy.app.utils.AppLogger.e("Failed to render PDF page: ${e.message}", e)
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedPdfUri = uri
            currentPageIndex = 0
            placedSignatures = emptyList()
            renderPage(uri, 0)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null && selectedPdfUri != null) {
            isSaving = true
            viewModel.applySignatures(context, selectedPdfUri!!, destUri, placedSignatures) { success ->
                isSaving = false
                if (success) {
                    onBack()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_sign_pdf), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedPdfUri == null) {
                // Empty PDF Picker
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Draw, contentDescription = null, modifier = Modifier.size(46.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.sign_pdf_headline), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.sign_pdf_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.select_pdf_to_sign), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Interactive Sign Canvas & Toolbar
                var canvasSize by remember { mutableStateOf(IntSize.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .shadow(8.dp, RoundedCornerShape(12.dp))
                        .onSizeChanged { canvasSize = it },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPageBitmap != null) {
                        Image(
                            bitmap = currentPageBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // Render Placed Signatures on Current Page
                        val pageSignatures = placedSignatures.filter { it.pageIndex == currentPageIndex }
                        for (sig in pageSignatures) {
                            val bmp = remember(sig) { BitmapFactory.decodeByteArray(sig.bitmapBytes, 0, sig.bitmapBytes.size) }
                            if (bmp != null) {
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (sig.xRatio * canvasSize.width).roundToInt(),
                                                (sig.yRatio * canvasSize.height).roundToInt()
                                            )
                                        }
                                        .size(
                                            (sig.widthRatio * canvasSize.width).dp.coerceAtLeast(80.dp),
                                            (sig.heightRatio * canvasSize.height).dp.coerceAtLeast(40.dp)
                                        )
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .background(Color.Transparent)
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            contentScale = ContentScale.Fit
                                        )
                                        if (!sig.dateStamp.isNullOrBlank()) {
                                            Text(
                                                text = "Signed: ${sig.dateStamp}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Page Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) {
                                currentPageIndex--
                                renderPage(selectedPdfUri!!, currentPageIndex)
                            }
                        },
                        enabled = currentPageIndex > 0
                    ) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                    }

                    Text("${currentPageIndex + 1} / $totalPages", fontWeight = FontWeight.Bold)

                    IconButton(
                        onClick = {
                            if (currentPageIndex < totalPages - 1) {
                                currentPageIndex++
                                renderPage(selectedPdfUri!!, currentPageIndex)
                            }
                        },
                        enabled = currentPageIndex < totalPages - 1
                    ) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                    }
                }

                // Saved Signatures Tray
                Text(stringResource(R.string.label_saved_signatures), style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        OutlinedButton(
                            onClick = { showSignaturePad = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.action_new_signature))
                        }
                    }

                    items(savedSignatures) { (name, bmp) ->
                        ElevatedCard(
                            modifier = Modifier
                                .size(100.dp, 56.dp)
                                .clickable {
                                    // Place signature in the center of the current page
                                    val stream = ByteArrayOutputStream()
                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val dateStr = if (includeDateStamp) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else null
                                    val newSig = PlacedSignature(
                                        pageIndex = currentPageIndex,
                                        xRatio = 0.35f,
                                        yRatio = 0.70f,
                                        widthRatio = 0.30f,
                                        heightRatio = 0.12f,
                                        bitmapBytes = stream.toByteArray(),
                                        dateStamp = dateStr
                                    )
                                    placedSignatures = placedSignatures + newSig
                                },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(4.dp), contentAlignment = Alignment.Center) {
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = name, contentScale = ContentScale.Fit)
                            }
                        }
                    }
                }

                // Save Action Button
                Button(
                    onClick = { savePdfLauncher.launch("signed_document_${System.currentTimeMillis()}.pdf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = placedSignatures.isNotEmpty() && !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_save_signed_pdf, placedSignatures.size),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Signature Drawing Pad Bottom Sheet
    if (showSignaturePad) {
        ModalBottomSheet(onDismissRequest = { showSignaturePad = false }) {
            var signaturePaths by remember { mutableStateOf<List<Path>>(emptyList()) }
            var currentPath by remember { mutableStateOf<Path?>(null) }
            var strokePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.title_draw_signature), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF7F8FA))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val p = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = p
                                    strokePoints = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    strokePoints = strokePoints + change.position
                                    val p = Path()
                                    if (strokePoints.isNotEmpty()) {
                                        p.moveTo(strokePoints[0].x, strokePoints[0].y)
                                        for (i in 1 until strokePoints.size) {
                                            p.lineTo(strokePoints[i].x, strokePoints[i].y)
                                        }
                                    }
                                    currentPath = p
                                },
                                onDragEnd = {
                                    if (currentPath != null) {
                                        signaturePaths = signaturePaths + currentPath!!
                                    }
                                    currentPath = null
                                    strokePoints = emptyList()
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (path in signaturePaths) {
                            drawPath(path, color = Color.Black, style = Stroke(width = 5f))
                        }
                        currentPath?.let {
                            drawPath(it, color = Color.Black, style = Stroke(width = 5f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        signaturePaths = emptyList()
                        currentPath = null
                    }) {
                        Text(stringResource(R.string.action_clear), color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            if (signaturePaths.isNotEmpty()) {
                                val bmp = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.BLACK
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeWidth = 6f
                                }
                                for (p in signaturePaths) {
                                    canvas.drawPath(p.asAndroidPath(), paint)
                                }

                                coroutineScope.launch {
                                    val sigName = "sig_${System.currentTimeMillis()}"
                                    SignatureEngine.saveSignature(context, sigName, bmp)
                                    savedSignatures = SignatureEngine.loadSignatures(context)
                                    showSignaturePad = false
                                }
                            }
                        },
                        enabled = signaturePaths.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.action_save_signature))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
