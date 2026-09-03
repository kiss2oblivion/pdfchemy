package com.pdfchemy.app.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfchemy.app.R
import com.pdfchemy.app.logic.OutlineBookmark
import com.pdfchemy.app.logic.PdfOutlineReader
import com.pdfchemy.app.logic.ReflowSection
import com.pdfchemy.app.utils.FileUtils
import kotlinx.coroutines.launch

enum class ReaderTheme(val bg: Color, val text: Color, val label: String) {
    LIGHT(Color(0xFFFFFFFF), Color(0xFF1E293B), "Light"),
    SEPIA(Color(0xFFFBF0D9), Color(0xFF5F4B32), "Sepia"),
    DARK(Color(0xFF1E293B), Color(0xFFF1F5F9), "Dark"),
    OLED(Color(0xFF000000), Color(0xFFE2E8F0), "OLED")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflowReaderScreen(
    initialUri: Uri? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var selectedPdfUri by remember { mutableStateOf<Uri?>(initialUri) }
    var reflowSections by remember { mutableStateOf<List<ReflowSection>>(emptyList()) }
    var bookmarks by remember { mutableStateOf<List<OutlineBookmark>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            isLoading = true
            reflowSections = PdfOutlineReader.extractReflowContent(context, initialUri)
            bookmarks = PdfOutlineReader.extractOutline(context, initialUri)
            isLoading = false
        }
    }

    var selectedTheme by remember { mutableStateOf(ReaderTheme.LIGHT) }
    var fontSizeSp by remember { mutableStateOf(16f) }
    var useSerifFont by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val listState = rememberLazyListState()

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            selectedPdfUri = uri
            isLoading = true
            scope.launch {
                reflowSections = PdfOutlineReader.extractReflowContent(context, uri)
                bookmarks = PdfOutlineReader.extractOutline(context, uri)
                isLoading = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = bookmarks.isNotEmpty(),
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Text(
                    text = "Table of Contents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                if (bookmarks.isEmpty()) {
                    Text(
                        text = "No document outlines found in this PDF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(bookmarks) { bookmark ->
                            NavigationDrawerItem(
                                label = { Text(bookmark.title, maxLines = 1) },
                                badge = { Text("P.${bookmark.pageNumber}") },
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        drawerState.close()
                                        val targetIdx = (bookmark.pageNumber - 1).coerceIn(0, (reflowSections.size - 1).coerceAtLeast(0))
                                        listState.animateScrollToItem(targetIdx)
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = selectedTheme.bg,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedPdfUri != null) FileUtils.getFileName(context, selectedPdfUri!!) ?: "Reader" else stringResource(R.string.menu_reflow_reader),
                            color = selectedTheme.text,
                            maxLines = 1
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = selectedTheme.bg,
                        scrolledContainerColor = selectedTheme.bg
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back), tint = selectedTheme.text)
                        }
                    },
                    actions = {
                        if (bookmarks.isNotEmpty()) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = "Table of Contents", tint = selectedTheme.text)
                            }
                        }
                        IconButton(onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "application/zip", "application/octet-stream")) }) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = "Open Document", tint = selectedTheme.text)
                        }
                    }
                )
            },
            bottomBar = {
                if (selectedPdfUri != null && reflowSections.isNotEmpty()) {
                    Surface(
                        color = selectedTheme.bg,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Theme Selector
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ReaderTheme.values().forEach { theme ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(theme.bg, CircleShape)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    selectedTheme = theme
                                                }
                                                .padding(2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selectedTheme == theme) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = theme.text
                                                )
                                            }
                                        }
                                    }
                                }

                                // Font Sizing Controls
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { fontSizeSp = (fontSizeSp - 2).coerceAtLeast(12f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("A-", color = selectedTheme.text, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${fontSizeSp.toInt()}sp", color = selectedTheme.text, style = MaterialTheme.typography.bodySmall)
                                    IconButton(
                                        onClick = { fontSizeSp = (fontSizeSp + 2).coerceAtMost(32f) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("A+", color = selectedTheme.text, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = { useSerifFont = !useSerifFont },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text(if (useSerifFont) "Serif" else "Sans", color = selectedTheme.text, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (selectedPdfUri == null) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 500.dp)
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = stringResource(R.string.reflow_reader_headline),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = stringResource(R.string.reflow_reader_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf", "application/epub+zip", "application/zip", "application/octet-stream")) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 50.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.select_pdf_for_reading),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(reflowSections) { section ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = selectedTheme.text.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PAGE ${section.pageNumber}",
                                        color = selectedTheme.text.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                section.paragraphs.forEach { p ->
                                    Text(
                                        text = p,
                                        color = selectedTheme.text,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp * 1.55f).sp,
                                        fontFamily = if (useSerifFont) FontFamily.Serif else FontFamily.SansSerif,
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                            HorizontalDivider(color = selectedTheme.text.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}
