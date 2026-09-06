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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.pdfchemy.desktop.engine.HeaderFooterPos
import com.pdfchemy.desktop.engine.TextAnnotationItem
import java.time.LocalDate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import java.awt.geom.Point2D
import javax.imageio.ImageIO
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pdfchemy.desktop.i18n.DesktopLanguage
import com.pdfchemy.desktop.i18n.DesktopLocalization
import com.pdfchemy.desktop.i18n.DesktopStrings
import com.pdfchemy.desktop.engine.DesktopPdfEngine
import com.pdfchemy.desktop.engine.DesktopPdfMetadata
import com.pdfchemy.desktop.engine.DesktopUpdateManager
import com.pdfchemy.desktop.engine.PageItemSpec
import com.pdfchemy.desktop.engine.ReleaseInfo
import com.pdfchemy.desktop.engine.AcroFieldType
import com.pdfchemy.desktop.engine.DesktopAcroField
import com.pdfchemy.desktop.engine.PageDiff
import com.pdfchemy.desktop.engine.PdfDiffSummary
import com.pdfchemy.desktop.engine.DesktopBatesConfig
import com.pdfchemy.desktop.engine.DesktopBatesPosition
import com.pdfchemy.desktop.engine.RedactPattern
import com.pdfchemy.desktop.engine.DesktopSanitizeResult
import com.pdfchemy.desktop.engine.DesktopRepairResult
import com.pdfchemy.desktop.engine.DesktopCropConfig
import com.pdfchemy.desktop.engine.DesktopAttachment
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

enum class DesktopNavTab(val icon: ImageVector) {
    HOME(Icons.Rounded.Dashboard),
    COMPRESS(Icons.Rounded.Speed),
    ORGANIZE(Icons.Rounded.GridView),
    MERGE(Icons.AutoMirrored.Rounded.CallMerge),
    SIGN(Icons.Rounded.AddPhotoAlternate),
    CONVERT(Icons.AutoMirrored.Rounded.Notes),
    READER(Icons.AutoMirrored.Rounded.MenuBook),
    SECURITY(Icons.Rounded.Lock),
    BATCH(Icons.Rounded.Layers),
    COMPARE(Icons.Rounded.Collections);

    fun label(strings: DesktopStrings): String = when (this) {
        HOME -> strings.tabAllTools
        COMPRESS -> strings.tabCompress
        ORGANIZE -> strings.tabOrganize
        MERGE -> strings.tabMerge
        SIGN -> strings.tabSign
        CONVERT -> strings.tabConvert
        READER -> strings.tabReader
        SECURITY -> strings.tabSecurity
        BATCH -> strings.tabBatch
        COMPARE -> strings.tabCompare
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopApp(
    initialFile: File? = null,
    initialShowSetup: Boolean = false,
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    currentTab: DesktopNavTab = DesktopNavTab.HOME,
    onTabChange: (DesktopNavTab) -> Unit = {}
) {
    val currentLang by DesktopLocalization.currentLanguageState
    val strings = DesktopLocalization.strings
    var showSetupDialog by remember { mutableStateOf(initialShowSetup) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    var activeTab by remember { mutableStateOf(currentTab) }
    LaunchedEffect(currentTab) {
        activeTab = currentTab
    }

    var selectedFile by remember { mutableStateOf<File?>(initialFile) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showManifestoDialog by remember { mutableStateOf(false) }
    var showTipJarDialog by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<ReleaseInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Silent background check for updates on launch
    LaunchedEffect(Unit) {
        val res = DesktopUpdateManager.checkForUpdates()
        res.onSuccess { info ->
            if (info.isNewer && info.tagName != DesktopUpdateManager.dismissedTag) {
                availableUpdate = info
            }
        }
    }

    fun performManualUpdateCheck() {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        scope.launch {
            val res = DesktopUpdateManager.checkForUpdates()
            isCheckingUpdate = false
            res.onSuccess { info ->
                if (info.isNewer) {
                    availableUpdate = info
                    showUpdateDialog = true
                } else {
                    updateFeedbackMessage = String.format(strings.upToDateDesc, DesktopUpdateManager.CURRENT_VERSION)
                }
            }.onFailure {
                updateFeedbackMessage = strings.updateCheckFailed
            }
        }
    }

    LaunchedEffect(selectedFile) {
        if (selectedFile != null && selectedFile!!.exists()) {
            RecentDocumentsManager.addRecent(selectedFile!!)
        }
    }

    if (showUpdateDialog && availableUpdate != null) {
        UpdateAvailableDialog(
            release = availableUpdate!!,
            onDismiss = { showUpdateDialog = false },
            onDismissForever = {
                DesktopUpdateManager.dismissedTag = availableUpdate!!.tagName
                showUpdateDialog = false
                availableUpdate = null
            }
        )
    }

    if (updateFeedbackMessage != null) {
        AlertDialog(
            onDismissRequest = { updateFeedbackMessage = null },
            confirmButton = {
                Button(onClick = { updateFeedbackMessage = null }) {
                    Text(strings.ok)
                }
            },
            title = {
                Text(strings.upToDateTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(updateFeedbackMessage!!)
            }
        )
    }

    if (showTipJarDialog) {
        TipJarDialog(onDismiss = { showTipJarDialog = false })
    }

    if (showManifestoDialog) {
        ManifestoDialog(
            onDismiss = { showManifestoDialog = false },
            onOpenTipJar = { showTipJarDialog = true }
        )
    }

    if (showSetupDialog) {
        InstallationSetupDialog(
            onDismiss = {
                DesktopLocalization.completeSetup(DesktopLocalization.currentLanguage)
                showSetupDialog = false
            }
        )
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
                        Text(strings.appTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                strings.desktopEdition,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Update Available Badge Button
                    if (availableUpdate != null) {
                        FilledTonalButton(
                            onClick = { showUpdateDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF00E676).copy(alpha = 0.2f),
                                contentColor = Color(0xFF00C853)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00C853))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update ${availableUpdate!!.tagName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // The Tip Jar TopBar Button
                    FilledTonalButton(
                        onClick = { showTipJarDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFF5E5B).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFF5E5B)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF5E5B))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnOpenTipJar, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Language Selector Dropdown Menu
                    Box {
                        FilledTonalButton(
                            onClick = { showLanguageMenu = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Translate, contentDescription = strings.selectLanguage, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentLang.nativeName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.heightIn(max = 420.dp)
                        ) {
                            DesktopLanguage.entries.forEach { lang ->
                                val isSelected = currentLang == lang
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${lang.nativeName} (${lang.englishName})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(
                                                    Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        DesktopLocalization.currentLanguage = lang
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Open File Button
                    Button(
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
                        Text(strings.openPdfCtrlO, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Theme Toggle Button
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = strings.toggleTheme
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
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DesktopNavTab.values().forEach { tab ->
                        NavigationRailItem(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab; onTabChange(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.label(strings)) },
                            label = { Text(tab.label(strings), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                // Tip Jar Rail Button
                IconButton(
                    onClick = { showTipJarDialog = true },
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = strings.tipJarTitle, tint = Color(0xFFFF5E5B))
                }

                // Bottom Manifesto Badge
                IconButton(
                    onClick = { showManifestoDialog = true },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Rounded.AllInclusive, contentDescription = "The Manifesto", tint = MaterialTheme.colorScheme.primary)
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
                        onSelectTab = { activeTab = it; onTabChange(it) },
                        selectedFile = selectedFile,
                        onSelectFile = { selectedFile = it },
                        onOpenManifesto = { showManifestoDialog = true },
                        onOpenTipJar = { showTipJarDialog = true },
                        availableUpdate = availableUpdate,
                        onOpenUpdateDialog = { showUpdateDialog = true },
                        isCheckingUpdate = isCheckingUpdate,
                        onCheckForUpdates = { performManualUpdateCheck() }
                    )
                    DesktopNavTab.COMPRESS -> CompressView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.ORGANIZE -> PageStudioView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.MERGE -> MergeView()
                    DesktopNavTab.SIGN -> SignAndStampView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.CONVERT -> ConvertView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.READER -> ReaderView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.SECURITY -> SecurityView(selectedFile, onFileChange = { selectedFile = it })
                    DesktopNavTab.BATCH -> BatchQueueView()
                    DesktopNavTab.COMPARE -> CompareView(selectedFile, onFileChange = { selectedFile = it })
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
    onOpenManifesto: () -> Unit = {},
    onOpenTipJar: () -> Unit = {},
    availableUpdate: ReleaseInfo? = null,
    onOpenUpdateDialog: () -> Unit = {},
    isCheckingUpdate: Boolean = false,
    onCheckForUpdates: () -> Unit = {}
) {
    val strings = DesktopLocalization.strings
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // New Version Available Banner
        if (availableUpdate != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                strings.updateAvailableTitle.format(availableUpdate.tagName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                strings.updateAvailableBanner,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onOpenUpdateDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnDownloadUpdate, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Hero Drag & Drop Visual Dropzone Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.homeHeroTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Drag & Drop any PDF anywhere onto this window, or click Browse to select.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedFile != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    String.format(strings.activeDocument, selectedFile.name, formatFileSize(selectedFile.length())),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                    Text(strings.selectPdf, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Recent Documents Tray
        var recentsList by remember { mutableStateOf(RecentDocumentsManager.getRecents()) }
        LaunchedEffect(selectedFile) {
            recentsList = RecentDocumentsManager.getRecents()
        }

        if (recentsList.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Recent Documents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            RecentDocumentsManager.clearRecents()
                            recentsList = emptyList()
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear Recents", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recentsList.take(3).forEach { recentFile ->
                        val isCurrent = selectedFile?.absolutePath == recentFile.absolutePath
                        Card(
                            onClick = { onSelectFile(recentFile) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                if (isCurrent) 1.5.dp else 1.dp,
                                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text(
                                        recentFile.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(formatFileSize(recentFile.length()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        if (isCurrent) "Active" else "Load Document",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(strings.documentSuperpowers, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        // 3-Column Tool Cards Grid
        val tools = listOf(
            ToolItem(strings.toolOrganizeTitle, strings.toolOrganizeDesc, Icons.Rounded.GridView, DesktopNavTab.ORGANIZE),
            ToolItem(strings.toolCompressTitle, strings.toolCompressDesc, Icons.Rounded.Speed, DesktopNavTab.COMPRESS),
            ToolItem(strings.toolMergeTitle, strings.toolMergeDesc, Icons.AutoMirrored.Rounded.CallMerge, DesktopNavTab.MERGE),
            ToolItem(strings.toolSignTitle, strings.toolSignDesc, Icons.Rounded.AddPhotoAlternate, DesktopNavTab.SIGN),
            ToolItem(strings.toolConvertTitle, strings.toolConvertDesc, Icons.AutoMirrored.Rounded.Notes, DesktopNavTab.CONVERT),
            ToolItem(strings.toolBatchTitle, strings.toolBatchDesc, Icons.Rounded.Layers, DesktopNavTab.BATCH),
            ToolItem(strings.toolReaderTitle, strings.toolReaderDesc, Icons.AutoMirrored.Rounded.MenuBook, DesktopNavTab.READER),
            ToolItem(strings.toolSecurityTitle, strings.toolSecurityDesc, Icons.Rounded.Lock, DesktopNavTab.SECURITY),
            ToolItem(strings.toolCompareTitle, strings.toolCompareDesc, Icons.Rounded.Collections, DesktopNavTab.COMPARE)
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

        // The Tip Jar / Indie Support Card on Home Dashboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ),
            border = BorderStroke(1.dp, Color(0xFFFF5E5B).copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFF5E5B).copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFFF5E5B), modifier = Modifier.size(24.dp))
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            strings.tipJarHomeCardTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            strings.tipJarHomeCardDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onOpenTipJar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5E5B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.btnOpenTipJar, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false).height(24.dp))

        // Subtle, Understated Footer
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "PDFchemy Tools v${DesktopUpdateManager.CURRENT_VERSION}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                TextButton(
                    onClick = onCheckForUpdates,
                    enabled = !isCheckingUpdate,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.checkingForUpdates, fontSize = 11.sp)
                    } else {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.checkForUpdates, fontSize = 11.sp)
                    }
                }
            }
            TextButton(
                onClick = onOpenManifesto,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Rounded.AllInclusive, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.ourManifesto, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private data class ToolItem(val title: String, val desc: String, val icon: ImageVector, val tab: DesktopNavTab)

// -------------------------------------------------------------------------------------------------
// 2. VISUAL PAGE STUDIO (GAP 1: THE PDF ARRANGER KILLER)
// -------------------------------------------------------------------------------------------------
@Composable
private fun PageStudioView(file: File?, onFileChange: (File) -> Unit) {
    val strings = DesktopLocalization.strings
    var pageItems by remember { mutableStateOf<List<PageItemSpec>>(emptyList()) }
    var selectedPageIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val thumbnails = remember { mutableStateMapOf<Int, ImageBitmap>() }
    var isLoadingThumbnails by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var lastSavedFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    var showCropDialog by remember { mutableStateOf(false) }
    var showBookletDialog by remember { mutableStateOf(false) }
    var showSplitDialog by remember { mutableStateOf(false) }
    var isDeskewing by remember { mutableStateOf(false) }
    var cropPresetIndex by remember { mutableStateOf(0) }
    var cropLeftPt by remember { mutableStateOf(28.35f) }
    var cropRightPt by remember { mutableStateOf(28.35f) }
    var cropTopPt by remember { mutableStateOf(28.35f) }
    var cropBottomPt by remember { mutableStateOf(28.35f) }
    var cropApplyToAll by remember { mutableStateOf(true) }
    var isDetectingCrop by remember { mutableStateOf(false) }

    LaunchedEffect(file) {
        selectedPageIndices = emptySet()
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
                    DesktopPdfEngine.renderAllThumbnails(file, targetWidth = 240) { pageIdx, bimg ->
                        val bitmap = bimg.toComposeImageBitmap()
                        scope.launch(Dispatchers.Main) {
                            thumbnails[pageIdx] = bitmap
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

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when {
                        // Ctrl+A: Select All
                        keyEvent.isCtrlPressed && keyEvent.key == Key.A && pageItems.isNotEmpty() -> {
                            selectedPageIndices = pageItems.indices.toSet()
                            true
                        }
                        // Delete / Backspace: Remove selected pages
                        (keyEvent.key == Key.Delete || keyEvent.key == Key.Backspace) && selectedPageIndices.isNotEmpty() -> {
                            val toKeep = pageItems.filterIndexed { idx, _ -> idx !in selectedPageIndices }
                            pageItems = toKeep
                            selectedPageIndices = emptySet()
                            true
                        }
                        // R: Rotate selected (or all if none selected) 90° clockwise
                        keyEvent.key == Key.R && pageItems.isNotEmpty() -> {
                            val targets = if (selectedPageIndices.isEmpty()) pageItems.indices.toSet() else selectedPageIndices
                            pageItems = pageItems.mapIndexed { idx, item ->
                                if (idx in targets) item.copy(rotation = (item.rotation + 90) % 360)
                                else item
                            }
                            true
                        }
                        // Ctrl+Z: Revert to original document pages
                        keyEvent.isCtrlPressed && keyEvent.key == Key.Z && file != null -> {
                            scope.launch(Dispatchers.IO) {
                                val count = DesktopPdfEngine.getPageCount(file)
                                withContext(Dispatchers.Main) {
                                    pageItems = (0 until count).map { PageItemSpec(originalPageIndex = it, rotation = 0) }
                                    selectedPageIndices = emptySet()
                                }
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Visual Page Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (file != null) "${file.name} — ${pageItems.size} pages • ${String.format(java.util.Locale.US, "%.1f", file.length() / (1024.0 * 1024.0))} MB" else "Load a document to organize and reorder pages visually",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file != null && pageItems.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⌨ Shortcuts:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text("Ctrl+A (Select All)", style = MaterialTheme.typography.labelSmall)
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("R (Rotate 90°)", style = MaterialTheme.typography.labelSmall)
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("Del (Remove)", style = MaterialTheme.typography.labelSmall)
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Text("Ctrl+Z (Revert)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (file != null && pageItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            val count = DesktopPdfEngine.getPageCount(file)
                            withContext(Dispatchers.Main) {
                                pageItems = (0 until count).map { PageItemSpec(originalPageIndex = it, rotation = 0) }
                                selectedPageIndices = emptySet()
                            }
                        }
                    }) {
                        Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset")
                    }

                    OutlinedButton(onClick = {
                        pageItems = pageItems.map { it.copy(rotation = (it.rotation + 90) % 360) }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rotate All 90°")
                    }

                    OutlinedButton(onClick = {
                        pageItems = pageItems.reversed()
                        selectedPageIndices = selectedPageIndices.map { pageItems.size - 1 - it }.toSet()
                    }) {
                        Text("⇄", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reverse")
                    }

                    OutlinedButton(
                        onClick = {
                            if (isDeskewing) return@OutlinedButton
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_straightened.pdf") ?: return@OutlinedButton
                            isDeskewing = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val count = DesktopPdfEngine.deskewDocument(file, outFile)
                                    withContext(Dispatchers.Main) {
                                        isDeskewing = false
                                        lastSavedFile = outFile
                                        statusText = if (count > 0) String.format(strings.deskewSuccess, count, outFile.absolutePath) else strings.deskewNoSkew
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isDeskewing = false
                                        statusText = "Deskew error: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = !isDeskewing
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDeskewing) "Straightening..." else "Auto-Deskew")
                    }

                    OutlinedButton(onClick = { showBookletDialog = true }) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Booklet / N-Up")
                    }

                    OutlinedButton(onClick = { showSplitDialog = true }) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Split Studio")
                    }

                    OutlinedButton(onClick = {
                        showCropDialog = true
                    }) {
                        Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.toolCropTitle)
                    }

                    Button(
                        onClick = {
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_organized.pdf") ?: return@Button
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DesktopPdfEngine.saveReorderedPdf(file, outFile, pageItems)
                                    withContext(Dispatchers.Main) {
                                        lastSavedFile = outFile
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

        // Multi-Select Contextual Action Bar
        if (file != null && pageItems.isNotEmpty()) {
            Surface(
                color = if (selectedPageIndices.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selectedPageIndices.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = {
                                selectedPageIndices = if (selectedPageIndices.size == pageItems.size) {
                                    emptySet()
                                } else {
                                    pageItems.indices.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (selectedPageIndices.size == pageItems.size) "Deselect All" else "Select All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                selectedPageIndices = pageItems.indices.filter { it !in selectedPageIndices }.toSet()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⇄ Invert", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        if (selectedPageIndices.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${selectedPageIndices.size} selected",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text("Select pages to rotate, delete, or extract into a separate PDF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (selectedPageIndices.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    pageItems = pageItems.mapIndexed { idx, item ->
                                        if (idx in selectedPageIndices) item.copy(rotation = (item.rotation + 90) % 360) else item
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rotate Selected", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val newItems = mutableListOf<PageItemSpec>()
                                    pageItems.forEachIndexed { idx, item ->
                                        newItems.add(item)
                                        if (idx in selectedPageIndices) {
                                            newItems.add(item.copy())
                                        }
                                    }
                                    pageItems = newItems
                                    selectedPageIndices = emptySet()
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Duplicate Selected", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    pageItems = pageItems.filterIndexed { idx, _ -> idx !in selectedPageIndices }
                                    selectedPageIndices = emptySet()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Selected", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_extracted.pdf") ?: return@Button
                                    val itemsToExtract = pageItems.filterIndexed { idx, _ -> idx in selectedPageIndices }
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            DesktopPdfEngine.saveReorderedPdf(file, outFile, itemsToExtract)
                                            withContext(Dispatchers.Main) {
                                                lastSavedFile = outFile
                                                statusText = "Extracted ${itemsToExtract.size} pages into:\n${outFile.absolutePath}"
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                statusText = "Extraction failed: ${e.message}"
                                            }
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Extract to New PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(statusText!!, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (lastSavedFile != null && lastSavedFile!!.exists()) {
                            if (!lastSavedFile!!.isDirectory) {
                                Button(
                                    onClick = { openDocument(lastSavedFile!!) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("📄 Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { openFileInExplorer(lastSavedFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
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

                                    val isSelected = index in selectedPageIndices

                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        ),
                                        border = BorderStroke(
                                            if (isSelected) 2.dp else 1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            selectedPageIndices = if (checked) selectedPageIndices + index else selectedPageIndices - index
                                                        },
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Surface(
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            "Page ${index + 1}",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                if (item.rotation != 0) {
                                                    Text("${item.rotation}°", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .background(Color.White, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedPageIndices = if (isSelected) selectedPageIndices - index else selectedPageIndices + index
                                                    },
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

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        if (index > 0) {
                                                            val mutable = pageItems.toMutableList()
                                                            val tmp = mutable[index]
                                                            mutable[index] = mutable[index - 1]
                                                            mutable[index - 1] = tmp
                                                            pageItems = mutable
                                                            selectedPageIndices = selectedPageIndices.map {
                                                                when (it) {
                                                                    index -> index - 1
                                                                    index - 1 -> index
                                                                    else -> it
                                                                }
                                                            }.toSet()
                                                        }
                                                    },
                                                    enabled = index > 0,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Move Left")
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val mutable = pageItems.toMutableList()
                                                        mutable[index] = item.copy(rotation = (item.rotation + 90) % 360)
                                                        pageItems = mutable
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Refresh, contentDescription = "Rotate")
                                                }

                                                IconButton(
                                                    onClick = {
                                                        val mutable = pageItems.toMutableList()
                                                        mutable.add(index + 1, item.copy())
                                                        pageItems = mutable
                                                        selectedPageIndices = emptySet()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate")
                                                }

                                                IconButton(
                                                    onClick = {
                                                        pageItems = pageItems.filterIndexed { i, _ -> i != index }
                                                        selectedPageIndices = selectedPageIndices.filter { it != index }.map { if (it > index) it - 1 else it }.toSet()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (index < pageItems.size - 1) {
                                                            val mutable = pageItems.toMutableList()
                                                            val tmp = mutable[index]
                                                            mutable[index] = mutable[index + 1]
                                                            mutable[index + 1] = tmp
                                                            pageItems = mutable
                                                            selectedPageIndices = selectedPageIndices.map {
                                                                when (it) {
                                                                    index -> index + 1
                                                                    index + 1 -> index
                                                                    else -> it
                                                                }
                                                            }.toSet()
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

        if (showCropDialog && file != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = { showCropDialog = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(520.dp)
                        .clickable(enabled = false, onClick = {})
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.toolCropTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(strings.toolCropDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showCropDialog = false }) {
                                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Presets Row
                        Text("Presets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = cropPresetIndex == 1,
                                onClick = {
                                    cropPresetIndex = 1
                                    cropLeftPt = 28.35f
                                    cropRightPt = 28.35f
                                    cropTopPt = 28.35f
                                    cropBottomPt = 28.35f
                                },
                                label = { Text(strings.cropPreset10mm, fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = cropPresetIndex == 2,
                                onClick = {
                                    cropPresetIndex = 2
                                    cropLeftPt = 42.52f
                                    cropRightPt = 42.52f
                                    cropTopPt = 42.52f
                                    cropBottomPt = 42.52f
                                },
                                label = { Text(strings.cropPreset15mm, fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = cropPresetIndex == 3,
                                onClick = {
                                    cropPresetIndex = 3
                                    cropLeftPt = 56.69f
                                    cropRightPt = 56.69f
                                    cropTopPt = 56.69f
                                    cropBottomPt = 56.69f
                                },
                                label = { Text(strings.cropPreset20mm, fontSize = 11.sp) }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                isDetectingCrop = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val firstSelected = selectedPageIndices.minOrNull() ?: 0
                                        val detected = DesktopPdfEngine.detectContentCrop(file, pageIndex = firstSelected)
                                        withContext(Dispatchers.Main) {
                                            cropPresetIndex = 0
                                            cropLeftPt = detected.leftPt
                                            cropRightPt = detected.rightPt
                                            cropTopPt = detected.topPt
                                            cropBottomPt = detected.bottomPt
                                            isDetectingCrop = false
                                        }
                                    } catch (_: Exception) {
                                        withContext(Dispatchers.Main) { isDetectingCrop = false }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isDetectingCrop
                        ) {
                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isDetectingCrop) "Detecting content margins..." else strings.cropPresetAuto)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = "%.1f".format(cropLeftPt),
                                onValueChange = {
                                    cropLeftPt = it.toFloatOrNull() ?: cropLeftPt
                                    cropPresetIndex = 0
                                },
                                label = { Text("Left (pt)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = "%.1f".format(cropTopPt),
                                onValueChange = {
                                    cropTopPt = it.toFloatOrNull() ?: cropTopPt
                                    cropPresetIndex = 0
                                },
                                label = { Text("Top (pt)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = "%.1f".format(cropRightPt),
                                onValueChange = {
                                    cropRightPt = it.toFloatOrNull() ?: cropRightPt
                                    cropPresetIndex = 0
                                },
                                label = { Text("Right (pt)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = "%.1f".format(cropBottomPt),
                                onValueChange = {
                                    cropBottomPt = it.toFloatOrNull() ?: cropBottomPt
                                    cropPresetIndex = 0
                                },
                                label = { Text("Bottom (pt)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = cropApplyToAll,
                                onCheckedChange = { cropApplyToAll = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (cropApplyToAll || selectedPageIndices.isEmpty())
                                    strings.cropApplyAll
                                else
                                    strings.cropApplyCurrent,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCropDialog = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    showCropDialog = false
                                    val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_cropped.pdf") ?: return@Button
                                    val config = DesktopCropConfig(
                                        leftPt = cropLeftPt,
                                        topPt = cropTopPt,
                                        rightPt = cropRightPt,
                                        bottomPt = cropBottomPt,
                                        applyToAllPages = cropApplyToAll || selectedPageIndices.isEmpty()
                                    )
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            DesktopPdfEngine.cropMargins(file, outFile, config)
                                            withContext(Dispatchers.Main) {
                                                lastSavedFile = outFile
                                                statusText = strings.cropSuccess.format(outFile.name) + "\n${outFile.absolutePath}"
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                statusText = "Crop failed: ${e.message}"
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.btnApplyCrop, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showBookletDialog && file != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = { showBookletDialog = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(520.dp)
                        .clickable(enabled = false, onClick = {}),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(strings.toolBookletTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(strings.toolBookletDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showBookletDialog = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }

                        // Saddle-Stitch Booklet Option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📖 Saddle-Stitch Folded Booklet", fontWeight = FontWeight.Bold)
                                Text("Reorders pages so that printed sheets can be folded in half to create a reading booklet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        showBookletDialog = false
                                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_booklet.pdf") ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                DesktopPdfEngine.generateBooklet(file, outFile)
                                                withContext(Dispatchers.Main) {
                                                    lastSavedFile = outFile
                                                    statusText = String.format(strings.bookletSuccess, outFile.absolutePath)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    statusText = "Booklet failed: ${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.btnGenerateBooklet)
                                }
                            }
                        }

                        // 2-Up Imposition Option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📑 2-Up Imposition (Side-by-Side)", fontWeight = FontWeight.Bold)
                                Text("Places 2 pages side-by-side onto landscape sheets. Saves 50% paper.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        showBookletDialog = false
                                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_2up.pdf") ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                DesktopPdfEngine.generateNUp(file, outFile, pagesPerSheet = 2)
                                                withContext(Dispatchers.Main) {
                                                    lastSavedFile = outFile
                                                    statusText = String.format(strings.nupSuccess, 2, outFile.absolutePath)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    statusText = "2-Up failed: ${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.btnGenerateNUp2)
                                }
                            }
                        }

                        // 4-Up Imposition Option
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📄 4-Up Imposition (4 per sheet)", fontWeight = FontWeight.Bold)
                                Text("Places 4 pages in a 2x2 grid onto portrait sheets. Ideal for handouts and slides.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        showBookletDialog = false
                                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_4up.pdf") ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                DesktopPdfEngine.generateNUp(file, outFile, pagesPerSheet = 4)
                                                withContext(Dispatchers.Main) {
                                                    lastSavedFile = outFile
                                                    statusText = String.format(strings.nupSuccess, 4, outFile.absolutePath)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    statusText = "4-Up failed: ${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(strings.btnGenerateNUp4)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSplitDialog && file != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = { showSplitDialog = false }),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(520.dp)
                        .clickable(enabled = false, onClick = {}),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Document Splitting Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Choose your splitting mode and destination directory", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { showSplitDialog = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }

                        // Split Every Page
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📄 Split into Single Pages", fontWeight = FontWeight.Bold)
                                Text("Creates a separate PDF file for every single page in the document.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        showSplitDialog = false
                                        val targetDir = DesktopFileDialog.chooseDirectory() ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val created = DesktopPdfEngine.splitPdf(file, targetDir, splitEveryNPages = 1)
                                                withContext(Dispatchers.Main) {
                                                    lastSavedFile = targetDir
                                                    statusText = String.format(strings.splitSuccess, created.size, targetDir.absolutePath)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    statusText = "Split failed: ${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Split Every Page")
                                }
                            }
                        }

                        // Split by Blank Pages
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⬜ " + strings.splitModeBlank, fontWeight = FontWeight.Bold)
                                Text(strings.splitBlankDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        showSplitDialog = false
                                        val targetDir = DesktopFileDialog.chooseDirectory() ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val created = DesktopPdfEngine.splitByBlankPages(file, targetDir)
                                                withContext(Dispatchers.Main) {
                                                    lastSavedFile = targetDir
                                                    statusText = if (created.isNotEmpty()) {
                                                        String.format(strings.splitSuccess, created.size, targetDir.absolutePath)
                                                    } else {
                                                        strings.splitNoBlanks
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    statusText = "Split by blank pages error: ${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Split by Blank Pages")
                                }
                            }
                        }

                        // Split by Bookmarks
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📑 " + strings.splitModeBookmarks, fontWeight = FontWeight.Bold)
                                Text(strings.splitBookmarksDesc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Button(
                                    onClick = {
                                        showSplitDialog = false
                                        val targetDir = DesktopFileDialog.chooseDirectory() ?: return@Button
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val created = DesktopPdfEngine.splitByBookmarks(file, targetDir)
                                                withContext(Dispatchers.Main) {
                                                    lastSavedFile = targetDir
                                                    statusText = if (created.isNotEmpty()) {
                                                        String.format(strings.splitSuccess, created.size, targetDir.absolutePath)
                                                    } else {
                                                        strings.splitNoBookmarks
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    statusText = "Split by bookmarks error: ${e.message}"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Split by Bookmarks")
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
private fun CompressView(file: File?, onFileChange: (File) -> Unit) {
    val strings = DesktopLocalization.strings
    var isTargetSizeMode by remember { mutableStateOf(false) }
    var targetMb by remember { mutableFloatStateOf(2.0f) }
    var qualityLevel by remember { mutableFloatStateOf(0.7f) }
    var targetDpi by remember { mutableFloatStateOf(140f) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf<String?>(null) }
    var lastCompressedFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(strings.compressTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                    Text(strings.selectPdf, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        val f = DesktopFileDialog.openPdf()
                        if (f != null) onFileChange(f)
                    }) {
                        Text(strings.openPdf)
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0.5f to "500 KB", 1.0f to "1 MB", 2.0f to "2 MB (Government/Email)", 5.0f to "5 MB").forEach { (mb, label) ->
                                FilterChip(
                                    selected = kotlin.math.abs(targetMb - mb) < 0.05f,
                                    onClick = { targetMb = mb },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            } else {
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
                                lastCompressedFile = outFile
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
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (lastCompressedFile != null && lastCompressedFile!!.exists()) {
                                Button(
                                    onClick = { openDocument(lastCompressedFile!!) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("📄 Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { openFileInExplorer(lastCompressedFile!!) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
// MERGE PDF VIEW (JOIN MULTIPLE DOCUMENTS)
// -------------------------------------------------------------------------------------------------
@Composable
private fun MergeView() {
    val strings = DesktopLocalization.strings
    var pdfFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var pageCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isMerging by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var mergedFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pdfFiles) {
        scope.launch(Dispatchers.IO) {
            val counts = mutableMapOf<String, Int>()
            pdfFiles.forEach { file ->
                if (!pageCounts.containsKey(file.absolutePath)) {
                    try {
                        counts[file.absolutePath] = DesktopPdfEngine.getPageCount(file)
                    } catch (_: Exception) {
                        counts[file.absolutePath] = 0
                    }
                }
            }
            if (counts.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    pageCounts = pageCounts + counts
                }
            }
        }
    }

    val totalPages = remember(pdfFiles, pageCounts) {
        pdfFiles.sumOf { pageCounts[it.absolutePath] ?: 0 }
    }

    val totalSizeMb = remember(pdfFiles) {
        pdfFiles.sumOf { it.length() } / (1024.0 * 1024.0)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.mergeHeader, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(strings.mergeSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pdfFiles.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            pdfFiles = emptyList()
                            statusText = null
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnClearList, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        val picked = DesktopFileDialog.openMultiplePdfs()
                        if (picked.isNotEmpty()) {
                            val existingPaths = pdfFiles.map { it.absolutePath }.toSet()
                            val newFiles = picked.filter { it.absolutePath !in existingPaths }
                            pdfFiles = pdfFiles + newFiles
                            statusText = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.btnAddPdfs, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Status banner if any
        if (statusText != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (isError) Icons.Rounded.DeleteOutline else Icons.Rounded.Save,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(statusText!!, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    if (mergedFile != null && !isError) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { openDocument(mergedFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { openFileInExplorer(mergedFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Summary Bar when files present
        if (pdfFiles.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            strings.totalFilesAndPages.format(pdfFiles.size, totalPages),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "•  ${String.format(java.util.Locale.US, "%.1f", totalSizeMb)} MB",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val defaultName = if (pdfFiles.size >= 2) "${pdfFiles[0].nameWithoutExtension}_merged.pdf" else "merged.pdf"
                            val target = DesktopFileDialog.savePdf(suggestedName = defaultName)
                            if (target != null) {
                                isMerging = true
                                statusText = strings.mergingPdfs
                                isError = false
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val success = DesktopPdfEngine.mergePdfs(pdfFiles, target)
                                        withContext(Dispatchers.Main) {
                                            isMerging = false
                                            if (success) {
                                                mergedFile = target
                                                statusText = strings.mergedSuccess.format(
                                                    pdfFiles.size,
                                                    target.name,
                                                    formatFileSize(target.length())
                                                )
                                                isError = false
                                            } else {
                                                statusText = "Error merging PDF documents. Please check the files."
                                                isError = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isMerging = false
                                            statusText = "Merge error: ${e.message}"
                                            isError = true
                                        }
                                    }
                                }
                            }
                        },
                        enabled = pdfFiles.size >= 2 && !isMerging,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isMerging) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.mergingPdfs, fontSize = 13.sp)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.btnMergeNow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // File List or Empty State
        if (pdfFiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(68.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.CallMerge, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(strings.noPdfsSelected, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select 2 or more PDF documents to join them in sequence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            val picked = DesktopFileDialog.openMultiplePdfs()
                            if (picked.isNotEmpty()) {
                                pdfFiles = picked
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnAddPdfs, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pdfFiles.forEachIndexed { index, file ->
                    val pages = pageCounts[file.absolutePath]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(file.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${formatFileSize(file.length())}  •  ${if (pages != null) "$pages pages" else "Reading..."}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = pdfFiles.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index - 1]
                                            mutable[index - 1] = temp
                                            pdfFiles = mutable
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Text("▲", fontWeight = FontWeight.Bold, color = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                }

                                IconButton(
                                    onClick = {
                                        if (index < pdfFiles.size - 1) {
                                            val mutable = pdfFiles.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index + 1]
                                            mutable[index + 1] = temp
                                            pdfFiles = mutable
                                        }
                                    },
                                    enabled = index < pdfFiles.size - 1
                                ) {
                                    Text("▼", fontWeight = FontWeight.Bold, color = if (index < pdfFiles.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                }

                                IconButton(
                                    onClick = {
                                        pdfFiles = pdfFiles.filterIndexed { i, _ -> i != index }
                                    }
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.removeFile, tint = MaterialTheme.colorScheme.error)
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
// 3B. SIGN & STAMP VIEW (DIGITAL SIGNATURES, PNG SEALS & BUSINESS STAMPS)
// -------------------------------------------------------------------------------------------------
private enum class SignTabMode { DRAW, UPLOAD, STAMP, TYPE, ACRO_FORM, BATES, PKI }
private enum class FormAnnotationTool { TEXT, CHECK, CROSS, DATE }

private enum class SignStampPos(val xRatio: Float, val yRatio: Float) {
    BOTTOM_RIGHT(0.60f, 0.80f),
    BOTTOM_LEFT(0.06f, 0.80f),
    TOP_RIGHT(0.60f, 0.06f),
    CENTER(0.32f, 0.42f)
}

private enum class SignStampScale(val widthRatio: Float) {
    SMALL(0.25f),
    MEDIUM(0.35f),
    LARGE(0.50f)
}

private data class PresetStampItem(val title: String, val colorHex: String, val subtext: String? = null)

@Composable
private fun SignAndStampView(file: File?, onFileChange: (File) -> Unit) {
    val strings = DesktopLocalization.strings
    val scope = rememberCoroutineScope()

    if (file == null || !file.exists()) {
        Card(
            modifier = Modifier.fillMaxWidth().height(380.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(strings.signHeader, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    strings.signSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 500.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        val picked = DesktopFileDialog.openPdf()
                        if (picked != null) onFileChange(picked)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.selectPdf, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    var totalPages by remember(file) { mutableStateOf(1) }
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                totalPages = DesktopPdfEngine.getPageCount(file).coerceAtLeast(1)
            } catch (_: Exception) {
                totalPages = 1
            }
        }
    }

    var currentPageIndex by remember(file) { mutableStateOf(0) }
    var selectedMode by remember { mutableStateOf(SignTabMode.DRAW) }
    var inkColorHex by remember { mutableStateOf("#1E3A8A") } // Official Blue
    var selectedPos by remember { mutableStateOf(SignStampPos.BOTTOM_RIGHT) }
    var selectedScale by remember { mutableStateOf(SignStampScale.MEDIUM) }

    // DRAW mode states
    val strokes = remember { mutableStateListOf<List<Point2D.Float>>() }
    var currentStroke by remember { mutableStateOf<MutableList<Point2D.Float>?>(null) }

    // UPLOAD mode states
    var uploadedImageFile by remember { mutableStateOf<File?>(null) }
    var uploadedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // STAMP mode states
    val presetStamps = remember {
        listOf(
            PresetStampItem(strings.stampConform, "#1E3A8A", "ORIGINAL VERIFIED"),
            PresetStampItem(strings.stampApproved, "#16A34A", "OFFICIAL APPROVAL"),
            PresetStampItem(strings.stampConfidential, "#DC2626", "INTERNAL USE ONLY"),
            PresetStampItem(strings.stampPaid, "#7C3AED", "PAYMENT SETTLED"),
            PresetStampItem(strings.stampDraft, "#D97706", "SUBJECT TO REVISION")
        )
    }
    var selectedStamp by remember { mutableStateOf(presetStamps[0]) }
    var customSubtext by remember { mutableStateOf("") }

    // TYPE / FORM FILLER mode states
    val placedAnnotations = remember { mutableStateListOf<TextAnnotationItem>() }
    var activeAnnotationTool by remember { mutableStateOf(FormAnnotationTool.TEXT) }
    var customAnnotationText by remember { mutableStateOf("") }
    var annotationFontSize by remember { mutableStateOf(14f) }
    var annotationColorHex by remember { mutableStateOf("#18181B") }

    // WATERMARK & PAGE NUMBERS suite
    var enableWatermark by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf(strings.watermarkConfidential) }
    var watermarkOpacity by remember { mutableStateOf(0.22f) }
    var watermarkRotation by remember { mutableStateOf(45f) }

    var enablePageNumbers by remember { mutableStateOf(false) }
    var pageNumPos by remember { mutableStateOf(HeaderFooterPos.BOTTOM_CENTER) }
    var pageNumFormat by remember { mutableStateOf("Page %1\$d of %2\$d") }

    // Interactive AcroForm Studio states
    var hasAcroForm by remember(file) { mutableStateOf(false) }
    var acroFields by remember(file) { mutableStateOf<List<DesktopAcroField>>(emptyList()) }
    val acroFormValues = remember(file) { mutableStateMapOf<String, String>() }
    var flattenOnSave by remember { mutableStateOf(true) }

    // Legal Bates Stamping states
    var batesPrefix by remember { mutableStateOf("CASE-2026-") }
    var batesSuffix by remember { mutableStateOf("") }
    var batesStartNum by remember { mutableStateOf(1) }
    var batesDigits by remember { mutableStateOf(6) }
    var batesPosition by remember { mutableStateOf(DesktopBatesPosition.BOTTOM_RIGHT) }

    // PKI Digital Signatures states
    var pkiSignerName by remember { mutableStateOf("") }
    var pkiReason by remember { mutableStateOf("I agree to the terms of this document") }
    var pkiLocation by remember { mutableStateOf("Local Device") }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            try {
                val detected = DesktopPdfEngine.hasAcroForm(file)
                hasAcroForm = detected
                if (detected) {
                    val fields = DesktopPdfEngine.extractAcroFields(file)
                    acroFields = fields
                    acroFormValues.clear()
                    fields.forEach { f ->
                        acroFormValues[f.name] = f.value
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Processing states
    var isProcessing by remember { mutableStateOf(false) }
    var signedFile by remember { mutableStateOf<File?>(null) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Page preview bitmap
    var pageThumbnail by remember(file, currentPageIndex) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file, currentPageIndex) {
        withContext(Dispatchers.IO) {
            try {
                val img = DesktopPdfEngine.renderThumbnail(file, currentPageIndex, targetWidth = 440)
                pageThumbnail = img.toComposeImageBitmap()
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.signHeader, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${file.name}  •  ${formatFileSize(file.length())}  •  ${totalPages} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = {
                    val picked = DesktopFileDialog.openPdf()
                    if (picked != null) onFileChange(picked)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Switch PDF", fontSize = 12.sp)
            }
        }

        // Two-column workspace: Builder on Left, Preview on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            // LEFT: Builder & Controls Card
            Card(
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Interactive AcroForm Discovery Banner
                    if (hasAcroForm) {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedMode = SignTabMode.ACRO_FORM },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text(strings.acroFormDetected.format(acroFields.size), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Interactive PDF Form detected • Tap to edit & seal fields", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { selectedMode = SignTabMode.ACRO_FORM },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(strings.modeAcroForm, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Mode Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple(SignTabMode.DRAW, strings.modeDraw, Icons.Rounded.Tune),
                            Triple(SignTabMode.UPLOAD, strings.modeUpload, Icons.Rounded.CloudUpload),
                            Triple(SignTabMode.STAMP, strings.modeStamp, Icons.Rounded.Shield),
                            Triple(SignTabMode.TYPE, strings.modeType, Icons.AutoMirrored.Rounded.Notes),
                            Triple(SignTabMode.ACRO_FORM, strings.modeAcroForm, Icons.Rounded.PictureAsPdf),
                            Triple(SignTabMode.BATES, strings.modeBates, Icons.Rounded.Straighten),
                            Triple(SignTabMode.PKI, "PKI Sign", Icons.Rounded.Shield)
                        ).forEach { (mode, label, icon) ->
                            val isSel = selectedMode == mode
                            FilledTonalButton(
                                onClick = { selectedMode = mode },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(label, fontSize = 10.5.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    // Mode 1: DRAW SIGNATURE
                    if (selectedMode == SignTabMode.DRAW) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Color Selector Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ink Color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        Triple("#1E3A8A", strings.inkBlue, Color(0xFF1E3A8A)),
                                        Triple("#18181B", strings.inkBlack, Color(0xFF18181B)),
                                        Triple("#DC2626", strings.inkRed, Color(0xFFDC2626))
                                    ).forEach { (hex, label, c) ->
                                        val isChosen = inkColorHex == hex
                                        FilterChip(
                                            selected = isChosen,
                                            onClick = { inkColorHex = hex },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.size(12.dp).background(c, CircleShape))
                                                    Text(label, fontSize = 11.sp)
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // Interactive Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .pointerInput(inkColorHex) {
                                        detectDragGestures(
                                            onDragStart = { offset ->
                                                val stroke = mutableListOf(Point2D.Float(offset.x, offset.y))
                                                currentStroke = stroke
                                                strokes.add(stroke)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                currentStroke?.add(Point2D.Float(change.position.x, change.position.y))
                                            },
                                            onDragEnd = { currentStroke = null },
                                            onDragCancel = { currentStroke = null }
                                        )
                                    }
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val drawColor = try { Color(java.awt.Color.decode(inkColorHex).rgb) } catch (_: Exception) { Color(0xFF1E3A8A) }
                                    for (stroke in strokes) {
                                        if (stroke.size < 2) {
                                            if (stroke.size == 1) {
                                                val p = stroke[0]
                                                drawCircle(drawColor, radius = 2f, center = Offset(p.x, p.y))
                                            }
                                            continue
                                        }
                                        for (i in 0 until stroke.size - 1) {
                                            val p1 = stroke[i]
                                            val p2 = stroke[i + 1]
                                            drawLine(
                                                color = drawColor,
                                                start = Offset(p1.x, p1.y),
                                                end = Offset(p2.x, p2.y),
                                                strokeWidth = 3.5f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }

                                if (strokes.isEmpty()) {
                                    Text(
                                        strings.drawHint,
                                        modifier = Modifier.align(Alignment.Center),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Canvas Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex)
                                    },
                                    enabled = strokes.isNotEmpty()
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Undo Stroke", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { strokes.clear() },
                                    enabled = strokes.isNotEmpty(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.clearSignature, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Mode 2: UPLOAD IMAGE / SEAL
                    if (selectedMode == SignTabMode.UPLOAD) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (uploadedImageFile == null) {
                                OutlinedButton(
                                    onClick = {
                                        val picked = DesktopFileDialog.openImage()
                                        if (picked != null) {
                                            uploadedImageFile = picked
                                            try {
                                                val bimg = ImageIO.read(picked)
                                                if (bimg != null) uploadedImageBitmap = bimg.toComposeImageBitmap()
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text(strings.btnPickSignatureFile, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Supports transparent PNG, JPG & WEBP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (uploadedImageBitmap != null) {
                                            Image(
                                                bitmap = uploadedImageBitmap!!,
                                                contentDescription = null,
                                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(Color.White)
                                            )
                                        }
                                        Column {
                                            Text(uploadedImageFile!!.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(formatFileSize(uploadedImageFile!!.length()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val picked = DesktopFileDialog.openImage()
                                            if (picked != null) {
                                                uploadedImageFile = picked
                                                try {
                                                    val bimg = ImageIO.read(picked)
                                                    if (bimg != null) uploadedImageBitmap = bimg.toComposeImageBitmap()
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Change", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Mode 3: BUSINESS STAMP PRESETS
                    if (selectedMode == SignTabMode.STAMP) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select Stamp Preset:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            presetStamps.forEach { item ->
                                val isChosen = selectedStamp == item
                                Card(
                                    onClick = { selectedStamp = item },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChosen) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    ),
                                    border = BorderStroke(
                                        if (isChosen) 1.5.dp else 1.dp,
                                        if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(Color(java.awt.Color.decode(item.colorHex).rgb), CircleShape)
                                            )
                                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        if (item.subtext != null) {
                                            Text(item.subtext, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Optional Subtext / Note Input
                            OutlinedTextField(
                                value = customSubtext,
                                onValueChange = { customSubtext = it },
                                label = { Text("Optional Subtext (Date, Registration #, Note)", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Mode 4: TYPE & FORM FILLER
                    if (selectedMode == SignTabMode.TYPE) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Tool selector: Text, Check, Cross, Date
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Pair(FormAnnotationTool.TEXT, strings.toolAddText),
                                    Pair(FormAnnotationTool.CHECK, strings.toolCheckmark),
                                    Pair(FormAnnotationTool.CROSS, strings.toolCrossmark),
                                    Pair(FormAnnotationTool.DATE, strings.toolInsertDate)
                                ).forEach { (tool, label) ->
                                    val isTool = activeAnnotationTool == tool
                                    FilterChip(
                                        selected = isTool,
                                        onClick = { activeAnnotationTool = tool },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isTool) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            if (activeAnnotationTool == FormAnnotationTool.TEXT) {
                                OutlinedTextField(
                                    value = customAnnotationText,
                                    onValueChange = { customAnnotationText = it },
                                    label = { Text("Text to Place", fontSize = 12.sp) },
                                    placeholder = { Text("Type name, notes, or form data...", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            // Font Size & Ink Color Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(strings.fontSizeLabel + ":", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    listOf(10f to "10", 12f to "12", 14f to "14", 18f to "18", 24f to "24").forEach { (sz, lbl) ->
                                        FilterChip(
                                            selected = annotationFontSize == sz,
                                            onClick = { annotationFontSize = sz },
                                            label = { Text(lbl, fontSize = 10.sp) },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "#18181B" to Color(0xFF18181B),
                                        "#1E3A8A" to Color(0xFF1E3A8A),
                                        "#DC2626" to Color(0xFFDC2626)
                                    ).forEach { (hex, col) ->
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(col, CircleShape)
                                                .border(if (annotationColorHex == hex) 2.dp else 1.dp, if (annotationColorHex == hex) MaterialTheme.colorScheme.primary else Color.LightGray, CircleShape)
                                                .clickable { annotationColorHex = hex }
                                        )
                                    }
                                }
                            }

                            // Click to place instructions banner
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        strings.clickToPlaceTextHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Placed annotations count & clear button
                            if (placedAnnotations.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${placedAnnotations.size} marks placed on page ${currentPageIndex + 1}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    TextButton(
                                        onClick = { placedAnnotations.clear() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(strings.clearAnnotations, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Mode 5: INTERACTIVE ACROFORM STUDIO
                    if (selectedMode == SignTabMode.ACRO_FORM) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (acroFields.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(strings.noAcroFields, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("This PDF does not contain interactive PDF form fields. You can use Form Filler (Type) to place custom text & checkmarks instead.", fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                Text("${acroFields.size} Interactive Form Fields", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Column(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    acroFields.forEach { field ->
                                        when (field.type) {
                                            AcroFieldType.CHECKBOX -> {
                                                val currentVal = acroFormValues[field.name] ?: field.value
                                                val isChecked = currentVal.equals("Yes", ignoreCase = true) || currentVal.equals("true", ignoreCase = true) || currentVal.equals("On", ignoreCase = true)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().clickable {
                                                        acroFormValues[field.name] = if (isChecked) "Off" else "Yes"
                                                    },
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            acroFormValues[field.name] = if (checked) "Yes" else "Off"
                                                        }
                                                    )
                                                    Column {
                                                        Text(field.name, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                                        if (field.isReadOnly) Text("Read-only", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                            AcroFieldType.CHOICE -> {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(field.name, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                                    val currentVal = acroFormValues[field.name] ?: field.value
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        field.options.forEach { opt ->
                                                            FilterChip(
                                                                selected = currentVal == opt,
                                                                onClick = { acroFormValues[field.name] = opt },
                                                                label = { Text(opt, fontSize = 11.sp) },
                                                                shape = RoundedCornerShape(6.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                val currentVal = acroFormValues[field.name] ?: field.value
                                                OutlinedTextField(
                                                    value = currentVal,
                                                    onValueChange = { acroFormValues[field.name] = it },
                                                    label = { Text(field.name, fontSize = 11.sp) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(strings.btnFlattenForm, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(strings.flattenFormDesc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = flattenOnSave,
                                            onCheckedChange = { flattenOnSave = it },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Mode 7: PKI DIGITAL SIGNATURES
                    if (selectedMode == SignTabMode.PKI) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("PKI Digital Signature", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Applies a self-signed digital certificate cryptographically verifying document integrity.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            OutlinedTextField(
                                value = pkiSignerName,
                                onValueChange = { pkiSignerName = it },
                                label = { Text("Signer Name (e.g. John Doe)", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pkiReason,
                                onValueChange = { pkiReason = it },
                                label = { Text("Reason for Signing", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = pkiLocation,
                                onValueChange = { pkiLocation = it },
                                label = { Text("Location", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }

                    // Mode 6: LEGAL BATES STAMPING
                    if (selectedMode == SignTabMode.BATES) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(strings.modeBates, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Sequential legal numbering across all pages with zero-padding and customizable placement.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = batesPrefix,
                                    onValueChange = { batesPrefix = it },
                                    label = { Text(strings.batesPrefix, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = batesSuffix,
                                    onValueChange = { batesSuffix = it },
                                    label = { Text(strings.batesSuffix, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = batesStartNum.toString(),
                                    onValueChange = { batesStartNum = it.toIntOrNull() ?: 1 },
                                    label = { Text(strings.batesStartNum, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = batesDigits.toString(),
                                    onValueChange = { batesDigits = (it.toIntOrNull() ?: 6).coerceIn(1, 10) },
                                    label = { Text(strings.batesDigits, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }

                            Text("Bates Placement:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    Pair(DesktopBatesPosition.BOTTOM_RIGHT, "Bottom Right"),
                                    Pair(DesktopBatesPosition.BOTTOM_CENTER, "Bottom Center"),
                                    Pair(DesktopBatesPosition.BOTTOM_LEFT, "Bottom Left")
                                ).forEach { (pos, label) ->
                                    FilterChip(
                                        selected = batesPosition == pos,
                                        onClick = { batesPosition = pos },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    Pair(DesktopBatesPosition.TOP_RIGHT, "Top Right"),
                                    Pair(DesktopBatesPosition.TOP_CENTER, "Top Center"),
                                    Pair(DesktopBatesPosition.TOP_LEFT, "Top Left")
                                ).forEach { (pos, label) ->
                                    FilterChip(
                                        selected = batesPosition == pos,
                                        onClick = { batesPosition = pos },
                                        label = { Text(label, fontSize = 10.sp) }
                                    )
                                }
                            }

                            val sampleNum = "%0${batesDigits}d".format(batesStartNum)
                            val sampleText = "$batesPrefix$sampleNum$batesSuffix"
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(strings.batesPreviewSample.format(sampleText), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Applies to all $totalPages pages", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // PAGE & POSITION CONTROLS
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Page Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                strings.selectPageToSign.format(currentPageIndex + 1, totalPages),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                    enabled = currentPageIndex > 0
                                ) {
                                    Text("◀", fontWeight = FontWeight.Bold)
                                }
                                Text("${currentPageIndex + 1} / $totalPages", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                IconButton(
                                    onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                                    enabled = currentPageIndex < totalPages - 1
                                ) {
                                    Text("▶", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (selectedMode != SignTabMode.TYPE && selectedMode != SignTabMode.ACRO_FORM && selectedMode != SignTabMode.BATES) {
                            // Position Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Position:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Pair(SignStampPos.BOTTOM_RIGHT, strings.positionBottomRight),
                                        Pair(SignStampPos.BOTTOM_LEFT, strings.positionBottomLeft),
                                        Pair(SignStampPos.TOP_RIGHT, strings.positionTopRight),
                                        Pair(SignStampPos.CENTER, strings.positionCenter)
                                    ).forEach { (pos, label) ->
                                        val isPos = selectedPos == pos
                                        FilterChip(
                                            selected = isPos,
                                            onClick = { selectedPos = pos },
                                            label = { Text(label, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // Size Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.stampSize + ":", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        Pair(SignStampScale.SMALL, "Small (25%)"),
                                        Pair(SignStampScale.MEDIUM, "Medium (35%)"),
                                        Pair(SignStampScale.LARGE, "Large (50%)")
                                    ).forEach { (sc, label) ->
                                        val isSc = selectedScale == sc
                                        FilterChip(
                                            selected = isSc,
                                            onClick = { selectedScale = sc },
                                            label = { Text(label, fontSize = 11.sp) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // EXPANDABLE SECTION: DOCUMENT WATERMARK & PAGE NUMBERS (ACROBAT PARITY)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Watermark Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Switch(
                                        checked = enableWatermark,
                                        onCheckedChange = { enableWatermark = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text(strings.watermarkTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (enableWatermark) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(strings.watermarkDraft, strings.watermarkConfidential, strings.watermarkCopy).forEach { preset ->
                                            FilterChip(
                                                selected = watermarkText == preset,
                                                onClick = { watermarkText = preset },
                                                label = { Text(preset, fontSize = 10.sp) },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (enableWatermark) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = watermarkText,
                                        onValueChange = { watermarkText = it },
                                        label = { Text("Watermark Text", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(strings.watermarkOpacity + ":", fontSize = 10.sp)
                                        listOf(0.12f to "12%", 0.22f to "22%", 0.38f to "38%").forEach { (op, lbl) ->
                                            FilterChip(
                                                selected = watermarkOpacity == op,
                                                onClick = { watermarkOpacity = op },
                                                label = { Text(lbl, fontSize = 9.sp) },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            // Page Numbers Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Switch(
                                        checked = enablePageNumbers,
                                        onCheckedChange = { enablePageNumbers = it },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text(strings.pageNumbersTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                if (enablePageNumbers) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(
                                            HeaderFooterPos.BOTTOM_CENTER to strings.positionBottomCenter,
                                            HeaderFooterPos.BOTTOM_RIGHT to strings.positionBottomRight,
                                            HeaderFooterPos.TOP_RIGHT to strings.positionTopRight
                                        ).forEach { (pos, lbl) ->
                                            FilterChip(
                                                selected = pageNumPos == pos,
                                                onClick = { pageNumPos = pos },
                                                label = { Text(lbl, fontSize = 10.sp) },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Button: Sign & Save PDF
                    val canApply = when (selectedMode) {
                        SignTabMode.DRAW -> strokes.isNotEmpty()
                        SignTabMode.UPLOAD -> uploadedImageFile != null
                        SignTabMode.STAMP -> true
                        SignTabMode.TYPE -> placedAnnotations.isNotEmpty() || enableWatermark || enablePageNumbers
                        SignTabMode.ACRO_FORM -> acroFields.isNotEmpty()
                        SignTabMode.BATES -> true
                        SignTabMode.PKI -> pkiSignerName.isNotBlank()
                    }

                    Button(
                        onClick = {
                            if (selectedMode == SignTabMode.BATES) {
                                val target = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_bates.pdf")
                                if (target != null) {
                                    isProcessing = true
                                    statusText = null
                                    isError = false
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val config = DesktopBatesConfig(
                                                prefix = batesPrefix,
                                                suffix = batesSuffix,
                                                startNumber = batesStartNum,
                                                digits = batesDigits,
                                                position = batesPosition,
                                                fontSize = 10f
                                            )
                                            val ok = DesktopPdfEngine.applyBatesStamping(file, target, config)
                                            withContext(Dispatchers.Main) {
                                                isProcessing = false
                                                if (ok && target.exists() && target.length() > 0) {
                                                    signedFile = target
                                                    statusText = strings.batesSuccess.format(target.name)
                                                    isError = false
                                                } else {
                                                    statusText = "Failed to apply Bates numbering."
                                                    isError = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isProcessing = false
                                                statusText = "Error: ${e.message}"
                                                isError = true
                                            }
                                        }
                                    }
                                }
                                return@Button
                            }

                            if (selectedMode == SignTabMode.PKI) {
                                val target = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_signed_pki.pdf")
                                if (target != null) {
                                    isProcessing = true
                                    statusText = null
                                    isError = false
                                    scope.launch(Dispatchers.IO) {
                                        val result = DesktopPdfEngine.signDocument(
                                            inputFile = file,
                                            outputFile = target,
                                            signerName = pkiSignerName,
                                            reason = pkiReason,
                                            location = pkiLocation
                                        )
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            if (result.isSuccess && target.exists() && target.length() > 0) {
                                                signedFile = target
                                                statusText = "Successfully cryptographically signed and saved to ${target.name}."
                                                isError = false
                                            } else {
                                                statusText = "Failed to sign document: ${result.exceptionOrNull()?.message}"
                                                isError = true
                                            }
                                        }
                                    }
                                }
                                return@Button
                            }

                            if (selectedMode == SignTabMode.ACRO_FORM) {
                                val defaultName = if (flattenOnSave) "${file.nameWithoutExtension}_flattened.pdf" else "${file.nameWithoutExtension}_filled.pdf"
                                val target = DesktopFileDialog.savePdf(suggestedName = defaultName)
                                if (target != null) {
                                    isProcessing = true
                                    statusText = null
                                    isError = false
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val ok = DesktopPdfEngine.fillAndFlattenAcroForm(
                                                inputFile = file,
                                                outputFile = target,
                                                fieldValues = acroFormValues.toMap(),
                                                flatten = flattenOnSave
                                            )
                                            withContext(Dispatchers.Main) {
                                                isProcessing = false
                                                if (ok && target.exists() && target.length() > 0) {
                                                    signedFile = target
                                                    statusText = "Saved form successfully to ${target.name} (${formatFileSize(target.length())})."
                                                    isError = false
                                                } else {
                                                    statusText = "Failed to save form fields."
                                                    isError = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                isProcessing = false
                                                statusText = "Error: ${e.message}"
                                                isError = true
                                            }
                                        }
                                    }
                                }
                                return@Button
                            }

                            val defaultName = if (selectedMode == SignTabMode.TYPE) {
                                "${file.nameWithoutExtension}_annotated.pdf"
                            } else {
                                "${file.nameWithoutExtension}_signed.pdf"
                            }
                            val target = DesktopFileDialog.savePdf(suggestedName = defaultName)
                            if (target != null) {
                                isProcessing = true
                                statusText = null
                                isError = false
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        var currentInput = file
                                        val tempFiles = mutableListOf<File>()

                                        // 1. Text Annotations (Form Filler)
                                        if (selectedMode == SignTabMode.TYPE && placedAnnotations.isNotEmpty()) {
                                            val stepOut = File.createTempFile("pdfchemy_annotated_", ".pdf")
                                            tempFiles.add(stepOut)
                                            val ok = DesktopPdfEngine.addTextAnnotations(
                                                inputFile = currentInput,
                                                outputFile = stepOut,
                                                pageIndex = currentPageIndex,
                                                items = placedAnnotations.toList()
                                            )
                                            if (ok) currentInput = stepOut
                                        } else if (selectedMode != SignTabMode.TYPE) {
                                            val stampImage: java.awt.image.BufferedImage = when (selectedMode) {
                                                SignTabMode.DRAW -> {
                                                    DesktopPdfEngine.renderStrokesToImage(
                                                        strokes = strokes,
                                                        canvasWidth = 460,
                                                        canvasHeight = 180,
                                                        colorHex = inkColorHex,
                                                        strokeWidth = 4.5f
                                                    )
                                                }
                                                SignTabMode.UPLOAD -> {
                                                    ImageIO.read(uploadedImageFile!!)
                                                }
                                                SignTabMode.STAMP -> {
                                                    DesktopPdfEngine.createBusinessStamp(
                                                        title = selectedStamp.title,
                                                        subtext = if (customSubtext.isNotBlank()) customSubtext else selectedStamp.subtext,
                                                        colorHex = selectedStamp.colorHex
                                                    )
                                                }
                                                else -> throw IllegalStateException()
                                            }

                                            val stepOut = File.createTempFile("pdfchemy_stamped_", ".pdf")
                                            tempFiles.add(stepOut)
                                            val ok = DesktopPdfEngine.stampDocument(
                                                inputFile = currentInput,
                                                outputFile = stepOut,
                                                pageIndex = currentPageIndex,
                                                stampImage = stampImage,
                                                xRatio = selectedPos.xRatio,
                                                yRatio = selectedPos.yRatio,
                                                widthRatio = selectedScale.widthRatio
                                            )
                                            if (ok) currentInput = stepOut
                                        }

                                        // 2. Watermark if enabled
                                        if (enableWatermark && watermarkText.isNotBlank()) {
                                            val stepOut = File.createTempFile("pdfchemy_watermark_", ".pdf")
                                            tempFiles.add(stepOut)
                                            val ok = DesktopPdfEngine.addWatermark(
                                                inputFile = currentInput,
                                                outputFile = stepOut,
                                                watermarkText = watermarkText,
                                                opacity = watermarkOpacity,
                                                rotationDegrees = watermarkRotation
                                            )
                                            if (ok) currentInput = stepOut
                                        }

                                        // 3. Page Numbers if enabled
                                        if (enablePageNumbers) {
                                            val stepOut = File.createTempFile("pdfchemy_pagenums_", ".pdf")
                                            tempFiles.add(stepOut)
                                            val ok = DesktopPdfEngine.addPageNumbers(
                                                inputFile = currentInput,
                                                outputFile = stepOut,
                                                formatPattern = pageNumFormat,
                                                position = pageNumPos
                                            )
                                            if (ok) currentInput = stepOut
                                        }

                                        currentInput.copyTo(target, overwrite = true)
                                        tempFiles.forEach { runCatching { it.delete() } }

                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            if (target.exists() && target.length() > 0) {
                                                signedFile = target
                                                statusText = if (selectedMode == SignTabMode.TYPE) {
                                                    strings.annotatedSuccess.format(target.name)
                                                } else {
                                                    strings.signedSuccess.format(target.name)
                                                }
                                                isError = false
                                            } else {
                                                statusText = "Error saving PDF document."
                                                isError = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            statusText = "Failed to process PDF: ${e.message}"
                                            isError = true
                                        }
                                    }
                                }
                            }
                        },
                        enabled = canApply && !isProcessing,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.signingPdf)
                        } else {
                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when (selectedMode) {
                                    SignTabMode.TYPE -> strings.btnAnnotateAndSave
                                    SignTabMode.ACRO_FORM -> if (flattenOnSave) strings.btnFlattenForm else "Save Form Data"
                                    SignTabMode.BATES -> strings.btnApplyBates
                                    SignTabMode.PKI -> "Apply Digital Signature"
                                    else -> strings.btnSignAndSave
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // RIGHT: Document Preview & Placement Visualizer Card
            Card(
                modifier = Modifier.weight(0.9f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live Page Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Page ${currentPageIndex + 1} of $totalPages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // PDF Page Surface with Visual Stamp & Annotation Placement Overlay
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .then(
                                if (selectedMode == SignTabMode.TYPE) {
                                    Modifier.pointerInput(activeAnnotationTool, customAnnotationText, annotationFontSize, annotationColorHex) {
                                        detectTapGestures { offset ->
                                            val xRatio = (offset.x / size.width.toFloat()).coerceIn(0.02f, 0.95f)
                                            val yRatio = (offset.y / size.height.toFloat()).coerceIn(0.02f, 0.95f)
                                            val todayStr = java.time.LocalDate.now().toString()
                                            val textToPlace = when (activeAnnotationTool) {
                                                FormAnnotationTool.CHECK -> "✓"
                                                FormAnnotationTool.CROSS -> "✕"
                                                FormAnnotationTool.DATE -> todayStr
                                                FormAnnotationTool.TEXT -> if (customAnnotationText.isNotBlank()) customAnnotationText else "Text"
                                            }
                                            placedAnnotations.add(
                                                TextAnnotationItem(
                                                    text = textToPlace,
                                                    xRatio = xRatio,
                                                    yRatio = yRatio,
                                                    fontSize = annotationFontSize,
                                                    colorHex = annotationColorHex
                                                )
                                            )
                                        }
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageThumbnail != null) {
                            Image(
                                bitmap = pageThumbnail!!,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().padding(6.dp)
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }

                        // Watermark preview overlay
                        if (enableWatermark && watermarkText.isNotBlank()) {
                            Text(
                                text = watermarkText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Red.copy(alpha = watermarkOpacity.coerceAtLeast(0.15f)),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // Page numbers preview badge
                        if (enablePageNumbers) {
                            val pageAlign = when (pageNumPos) {
                                HeaderFooterPos.BOTTOM_CENTER -> Alignment.BottomCenter
                                HeaderFooterPos.BOTTOM_RIGHT -> Alignment.BottomEnd
                                HeaderFooterPos.BOTTOM_LEFT -> Alignment.BottomStart
                                HeaderFooterPos.TOP_CENTER -> Alignment.TopCenter
                                HeaderFooterPos.TOP_RIGHT -> Alignment.TopEnd
                            }
                            Text(
                                text = "Page ${currentPageIndex + 1} of $totalPages",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray.copy(alpha = 0.8f),
                                modifier = Modifier.align(pageAlign).padding(8.dp)
                            )
                        }

                        if (selectedMode == SignTabMode.TYPE) {
                            // Placed Annotations Badges
                            for (item in placedAnnotations.toList()) {
                                val awtColor = try { Color(java.awt.Color.decode(item.colorHex).rgb) } catch (_: Exception) { Color.Black }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = (maxWidth * item.xRatio).coerceIn(0.dp, maxWidth - 48.dp),
                                            y = (maxHeight * item.yRatio).coerceIn(0.dp, maxHeight - 28.dp)
                                        )
                                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                                        .border(1.dp, awtColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                        .clickable { placedAnnotations.remove(item) }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            item.text,
                                            fontSize = (item.fontSize * 0.72f).sp,
                                            fontWeight = FontWeight.Bold,
                                            color = awtColor
                                        )
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(10.dp),
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        } else {
                            // Simulated Overlay Box showing where the signature or stamp will land
                            val align = when (selectedPos) {
                                SignStampPos.BOTTOM_RIGHT -> Alignment.BottomEnd
                                SignStampPos.BOTTOM_LEFT -> Alignment.BottomStart
                                SignStampPos.TOP_RIGHT -> Alignment.TopEnd
                                SignStampPos.CENTER -> Alignment.Center
                            }

                            val stampWidthFraction = selectedScale.widthRatio

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(stampWidthFraction)
                                    .height(56.dp)
                                    .align(align)
                                    .padding(12.dp)
                                    .background(
                                        when (selectedMode) {
                                            SignTabMode.DRAW -> Color(0xFF1E3A8A).copy(alpha = 0.18f)
                                            SignTabMode.UPLOAD -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                                            SignTabMode.STAMP -> Color(java.awt.Color.decode(selectedStamp.colorHex).rgb).copy(alpha = 0.22f)
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.5.dp,
                                        when (selectedMode) {
                                            SignTabMode.DRAW -> Color(0xFF1E3A8A)
                                            SignTabMode.UPLOAD -> MaterialTheme.colorScheme.secondary
                                            SignTabMode.STAMP -> Color(java.awt.Color.decode(selectedStamp.colorHex).rgb)
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (selectedMode) {
                                        SignTabMode.DRAW -> "✍️ Signature"
                                        SignTabMode.UPLOAD -> "🖼️ Seal"
                                        SignTabMode.STAMP -> selectedStamp.title
                                        SignTabMode.ACRO_FORM -> "📋 Form"
                                        SignTabMode.BATES -> "⚖️ $batesPrefix${"%0${batesDigits}d".format(batesStartNum)}$batesSuffix"
                                        SignTabMode.PKI -> "🔒 PKI Cert: $pkiSignerName"
                                        else -> ""
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (selectedMode) {
                                        SignTabMode.DRAW -> Color(0xFF1E3A8A)
                                        SignTabMode.UPLOAD -> MaterialTheme.colorScheme.secondary
                                        SignTabMode.STAMP -> Color(java.awt.Color.decode(selectedStamp.colorHex).rgb)
                                        SignTabMode.PKI -> Color(0xFF059669) // Green shade for security
                                        else -> Color.Black
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text(
                        "The stamp/signature is scaled and applied to the PDF content stream with 100% vector accuracy.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Status Card
        if (statusText != null) {
            Surface(
                color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            if (isError) Icons.Rounded.DeleteOutline else Icons.Rounded.Save,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Text(statusText!!, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    if (signedFile != null && !isError) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = { openDocument(signedFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = { openFileInExplorer(signedFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Show in Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. CONVERT STUDIO (GAP 3: IMAGES ⇄ PDF CONVERSION)
// -------------------------------------------------------------------------------------------------
@Composable
private fun ConvertView(file: File?, onFileChange: (File) -> Unit) {
    val strings = DesktopLocalization.strings
    var statusText by remember { mutableStateOf<String?>(null) }
    var lastConvertedTarget by remember { mutableStateOf<File?>(null) }
    var selectedImages by remember { mutableStateOf<List<File>>(emptyList()) }
    var extractedCsvText by remember { mutableStateOf<String?>(null) }
    var isExtractingCsv by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(strings.convertTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                        Text(strings.tabImagesToPdf, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Compile camera photos, receipts, or screenshots into a crisp PDF document.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {
                        val imgs = DesktopFileDialog.openMultipleImages()
                        if (imgs.isNotEmpty()) selectedImages = selectedImages + imgs
                    }) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.addImages)
                    }
                }

                if (selectedImages.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedImages.size} images selected:", fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { selectedImages = emptyList() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("✕ Clear All", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
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
                                    Text("${i + 1}. ${img.name}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (i > 0) {
                                                    val list = selectedImages.toMutableList()
                                                    val item = list.removeAt(i)
                                                    list.add(i - 1, item)
                                                    selectedImages = list
                                                }
                                            },
                                            enabled = i > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("▲", fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                if (i < selectedImages.size - 1) {
                                                    val list = selectedImages.toMutableList()
                                                    val item = list.removeAt(i)
                                                    list.add(i + 1, item)
                                                    selectedImages = list
                                                }
                                            },
                                            enabled = i < selectedImages.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("▼", fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = { selectedImages = selectedImages.filterIndexed { idx, _ -> idx != i } },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
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
                                            lastConvertedTarget = outFile
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
                                    lastConvertedTarget = folder
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
                                    lastConvertedTarget = txtFile
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

        // OCR Searchable PDF Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("OCR Searchable PDF", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Scan images/documents and inject an invisible, selectable text layer using offline OCR.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Uses 100% offline Tesseract OCR. Processes completely on-device without data leaving your machine.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                var isOcrRunning by remember { mutableStateOf(false) }
                var ocrProgress by remember { mutableStateOf(0f) }

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_searchable.pdf") ?: return@Button
                        isOcrRunning = true
                        ocrProgress = 0f
                        scope.launch(Dispatchers.IO) {
                            try {
                                val result = DesktopPdfEngine.makeSearchable(file, outFile) { current, total ->
                                    ocrProgress = current.toFloat() / total.toFloat()
                                }
                                withContext(Dispatchers.Main) {
                                    isOcrRunning = false
                                    if (result.isSuccess) {
                                        lastConvertedTarget = outFile
                                        statusText = "OCR completed successfully:\n${outFile.absolutePath}"
                                    } else {
                                        statusText = "OCR failed: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isOcrRunning = false
                                    statusText = "OCR error: ${e.message}"
                                }
                            }
                        }
                    },
                    enabled = file != null && !isOcrRunning,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isOcrRunning) {
                        CircularProgressIndicator(
                            progress = { ocrProgress },
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing... ${(ocrProgress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Make Searchable (OCR)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PDF to PDF/A-1b Archival Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(strings.tabPdfToPdfA, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(strings.pdfaDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Compliant with ISO 19005-1 (PDF/A-1b). Embeds standard device-independent sRGB OutputIntent and compliant XMP pdfaid metadata.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_pdfa.pdf") ?: return@Button
                        scope.launch(Dispatchers.IO) {
                            try {
                                val ok = DesktopPdfEngine.convertToPdfA(file, outFile)
                                withContext(Dispatchers.Main) {
                                    if (ok) {
                                        lastConvertedTarget = outFile
                                        statusText = strings.pdfaSuccess.format(outFile.absolutePath)
                                    } else {
                                        statusText = "PDF/A conversion failed."
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    statusText = "Conversion error: ${e.message}"
                                }
                            }
                        }
                    },
                    enabled = file != null,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.btnConvertToPdfA, fontWeight = FontWeight.Bold)
                }
            }
        }

        // PDF Table to CSV Extractor Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(strings.tabTableToCsv, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(strings.tableToCsvDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Uses spatial 2D clustering to detect tabular boundaries, headers, and cell alignments without lossy cloud conversion. RFC 4180 compliant.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        isExtractingCsv = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val csv = DesktopPdfEngine.extractTablesToCsv(file)
                                withContext(Dispatchers.Main) {
                                    isExtractingCsv = false
                                    extractedCsvText = csv
                                    if (csv.isBlank()) {
                                        statusText = strings.csvNoTablesFound
                                    } else {
                                        val lineCount = csv.lines().filter { it.isNotBlank() }.size
                                        statusText = "Extracted $lineCount table rows from ${file.name}"
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isExtractingCsv = false
                                    statusText = "Table extraction failed: ${e.message}"
                                }
                            }
                        }
                    },
                    enabled = file != null && !isExtractingCsv,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.GridView, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isExtractingCsv) "Detecting & Extracting Tables..." else "Extract Tables to CSV", fontWeight = FontWeight.Bold)
                }

                if (!extractedCsvText.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CSV Data Preview:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp)
                        ) {
                            SelectionContainer {
                                Text(
                                    text = extractedCsvText!!.take(2000) + if (extractedCsvText!!.length > 2000) "\n... [truncated preview]" else "",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val outFile = DesktopFileDialog.saveCsv(suggestedName = "${file?.nameWithoutExtension ?: "extracted"}_table.csv") ?: return@Button
                                    try {
                                        outFile.writeText(extractedCsvText!!)
                                        lastConvertedTarget = outFile
                                        statusText = String.format(strings.csvExportSuccess, outFile.absolutePath)
                                    } catch (e: Exception) {
                                        statusText = "Export failed: ${e.message}"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.btnExportCsv)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(extractedCsvText), null)
                                        statusText = strings.csvCopied
                                    } catch (e: Exception) {
                                        statusText = "Copy failed: ${e.message}"
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.btnCopyCsv)
                            }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (lastConvertedTarget != null && lastConvertedTarget!!.exists()) {
                            if (lastConvertedTarget!!.isFile) {
                                Button(
                                    onClick = { openDocument(lastConvertedTarget!!) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("📄 Open Output", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { openFileInExplorer(lastConvertedTarget!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
private fun BatchQueueView() {
    val strings = DesktopLocalization.strings
    var queueFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var lastBatchTarget by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(strings.batchTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

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
                        Text(strings.addFiles)
                    }
                }

                if (queueFiles.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${queueFiles.size} documents in queue:", fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = { queueFiles = emptyList() },
                            enabled = !isProcessing,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("✕ Clear Queue", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
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
                                    Text("${idx + 1}. ${f.name} (${formatFileSize(f.length())})", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val list = queueFiles.toMutableList()
                                                    val item = list.removeAt(idx)
                                                    list.add(idx - 1, item)
                                                    queueFiles = list
                                                }
                                            },
                                            enabled = !isProcessing && idx > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("▲", fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx < queueFiles.size - 1) {
                                                    val list = queueFiles.toMutableList()
                                                    val item = list.removeAt(idx)
                                                    list.add(idx + 1, item)
                                                    queueFiles = list
                                                }
                                            },
                                            enabled = !isProcessing && idx < queueFiles.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("▼", fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = { queueFiles = queueFiles.filterIndexed { i, _ -> i != idx } },
                                            enabled = !isProcessing,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isProcessing) {
                        LinearProgressIndicator(progress = { currentProgress }, modifier = Modifier.fillMaxWidth())
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val outDir = DesktopFileDialog.chooseDirectory() ?: return@Button
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        var count = 0
                                        val targetBytes = 2 * 1024 * 1024L
                                        queueFiles.forEachIndexed { _, f ->
                                            val outFile = File(outDir, "${f.nameWithoutExtension}_under2MB.pdf")
                                            DesktopPdfEngine.compressToTargetSize(f, outFile, targetBytes)
                                            count++
                                            currentProgress = count.toFloat() / queueFiles.size.toFloat()
                                        }
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            lastBatchTarget = outDir
                                            statusText = "Bureaucracy batch complete! All ${queueFiles.size} files guaranteed under 2MB, saved to:\n${outDir.absolutePath}"
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
                            modifier = Modifier.weight(1.2f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Under 2MB (Portal)")
                        }

                        OutlinedButton(
                            onClick = {
                                val outDir = DesktopFileDialog.chooseDirectory() ?: return@OutlinedButton
                                isProcessing = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        var count = 0
                                        queueFiles.forEachIndexed { _, f ->
                                            val outFile = File(outDir, "${f.nameWithoutExtension}_compressed.pdf")
                                            DesktopPdfEngine.compressPdf(f, outFile)
                                            count++
                                            currentProgress = count.toFloat() / queueFiles.size.toFloat()
                                        }
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            lastBatchTarget = outDir
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
                            Text("Standard Compress")
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
                                            lastBatchTarget = outFile
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
                            Text("Merge Into One")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (lastBatchTarget != null && lastBatchTarget!!.exists()) {
                            if (lastBatchTarget!!.isFile) {
                                Button(
                                    onClick = { openDocument(lastBatchTarget!!) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("📄 Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { openFileInExplorer(lastBatchTarget!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
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
    val strings = DesktopLocalization.strings
    var extractedText by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableFloatStateOf(16f) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(file) {
        searchQuery = ""
        if (file != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val text = DesktopPdfEngine.extractText(file)
                    withContext(Dispatchers.Main) { extractedText = text }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { extractedText = "Could not extract text: ${e.message}" }
                }
            }
        } else {
            extractedText = null
        }
    }

    val matches = remember(extractedText, searchQuery) {
        val text = extractedText
        if (text.isNullOrBlank() || searchQuery.isBlank()) emptyList()
        else {
            try {
                Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE).findAll(text).toList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(matches) {
        currentMatchIndex = 0
    }

    val annotatedText = remember(extractedText, searchQuery, currentMatchIndex) {
        val text = extractedText ?: return@remember AnnotatedString("")
        if (searchQuery.isBlank() || matches.isEmpty()) {
            AnnotatedString(text)
        } else {
            buildAnnotatedString {
                append(text)
                for ((idx, match) in matches.withIndex()) {
                    val isActive = idx == currentMatchIndex
                    addStyle(
                        SpanStyle(
                            background = if (isActive) Color(0xFFFF5722) else Color(0xFFFFD54F), // Active is fiery orange, others golden yellow
                            color = if (isActive) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.F) {
                    focusRequester.requestFocus()
                    true
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(strings.readerTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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

        // Search & Filter Toolbar
        if (file != null && extractedText != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🔍", fontSize = 16.sp)

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Find text in document... (Ctrl+F, Enter for next)") },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter) {
                                    if (matches.isNotEmpty()) {
                                        if (keyEvent.isShiftPressed) {
                                            currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size
                                        } else {
                                            currentMatchIndex = (currentMatchIndex + 1) % matches.size
                                        }
                                    }
                                    true
                                } else false
                            },
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (searchQuery.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    if (matches.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex - 1 + matches.size) % matches.size
                                    }
                                },
                                enabled = matches.isNotEmpty(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("▲", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    if (matches.isNotEmpty()) {
                                        currentMatchIndex = (currentMatchIndex + 1) % matches.size
                                    }
                                },
                                enabled = matches.isNotEmpty(),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("▼", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Surface(
                                color = if (matches.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    if (matches.isNotEmpty()) "${currentMatchIndex + 1} of ${matches.size}" else "0 matches",
                                    color = if (matches.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
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
                            text = annotatedText,
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
    val strings = DesktopLocalization.strings
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var redactQuery by remember { mutableStateOf("") }
    var overlayText by remember { mutableStateOf("REDACTED") }
    var forensicSanitize by remember { mutableStateOf(true) }
    
    // Smart PII Redact State
    var smartRedactPatterns by remember { mutableStateOf<Set<RedactPattern>>(emptySet()) }

    var metadata by remember { mutableStateOf<DesktopPdfMetadata?>(null) }
    var isLoadingMetadata by remember { mutableStateOf(false) }

    var threatAudit by remember { mutableStateOf<DesktopSanitizeResult?>(null) }
    var purgeJs by remember { mutableStateOf(true) }
    var purgeMetadataOpt by remember { mutableStateOf(true) }
    var purgeAttachments by remember { mutableStateOf(true) }
    var repairResult by remember { mutableStateOf<DesktopRepairResult?>(null) }
    var attachmentsList by remember { mutableStateOf<List<DesktopAttachment>>(emptyList()) }
    var isLoadingAttachments by remember { mutableStateOf(false) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var lastSecurityFile by remember { mutableStateOf<File?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(file) {
        if (file != null && file.exists()) {
            isLoadingMetadata = true
            isLoadingAttachments = true
            scope.launch(Dispatchers.IO) {
                try {
                    val meta = DesktopPdfEngine.inspectMetadata(file)
                    val threats = DesktopPdfEngine.auditDocumentThreats(file)
                    val atts = DesktopPdfEngine.listAttachments(file)
                    withContext(Dispatchers.Main) {
                        metadata = meta
                        threatAudit = threats
                        attachmentsList = atts
                        isLoadingMetadata = false
                        isLoadingAttachments = false
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        metadata = null
                        threatAudit = null
                        attachmentsList = emptyList()
                        isLoadingMetadata = false
                        isLoadingAttachments = false
                    }
                }
            }
        } else {
            metadata = null
            threatAudit = null
            attachmentsList = emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(strings.securityTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Document Selection Bar
        if (file == null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(strings.selectPdf, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        val f = DesktopFileDialog.openPdf()
                        if (f != null) onFileChange(f)
                    }) {
                        Text(strings.openPdf)
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column {
                            Text(file.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(onClick = {
                        val f = DesktopFileDialog.openPdf()
                        if (f != null) onFileChange(f)
                    }) {
                        Text("Change PDF")
                    }
                }
            }
        }

        // 1. Deep Document Sanitizer & Threat Scrubber Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.sanitizeTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            strings.sanitizeSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val threatsCount = threatAudit?.threatsFound ?: 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (threatsCount > 0 || metadata?.hasAnyMetadata == true) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else Color(0xFF00E676).copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (threatsCount > 0) "$threatsCount Threat(s) Detected" else if (metadata?.hasAnyMetadata == true) "Metadata Present" else "Clean Document",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (threatsCount > 0 || metadata?.hasAnyMetadata == true) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                        )
                    }
                }

                if (file != null && (threatAudit != null || metadata != null)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("JavaScript Code Hooks:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if ((threatAudit?.jsCount ?: 0) > 0) "⚠️ ${threatAudit?.jsCount} Detected" else "✓ Clean", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Launch & Form Actions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if ((threatAudit?.launchActionsCount ?: 0) > 0) "⚠️ ${threatAudit?.launchActionsCount} Detected" else "✓ Clean", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Embedded File Attachments:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if ((threatAudit?.attachmentsPurged ?: 0) > 0) "⚠️ ${threatAudit?.attachmentsPurged} Embedded" else "✓ None", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Author & Software Trackers:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(metadata?.author ?: metadata?.creator ?: if (metadata?.hasAnyMetadata == true) "Present" else "✓ Anonymous", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = purgeJs, onCheckedChange = { purgeJs = it })
                        Text("Purge JS & Launch", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = purgeMetadataOpt, onCheckedChange = { purgeMetadataOpt = it })
                        Text("Wipe Metadata & UUIDs", fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = purgeAttachments, onCheckedChange = { purgeAttachments = it })
                        Text("Strip Attachments", fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_sanitized.pdf") ?: return@Button
                        isProcessing = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                DesktopPdfEngine.sanitizeDocument(
                                    inputFile = file,
                                    outputFile = outFile,
                                    purgeJs = purgeJs,
                                    purgeActions = purgeJs,
                                    purgeMetadata = purgeMetadataOpt,
                                    purgeAttachments = purgeAttachments
                                )
                                val newMeta = DesktopPdfEngine.inspectMetadata(outFile)
                                val newThreats = DesktopPdfEngine.auditDocumentThreats(outFile)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    metadata = newMeta
                                    threatAudit = newThreats
                                    lastSecurityFile = outFile
                                    statusText = strings.sanitizeSuccess.format(outFile.name)
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    statusText = "Sanitization failed: ${e.message}"
                                }
                            }
                        }
                    },
                    enabled = !isProcessing && file != null,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.btnSanitizeDocument, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. Forensic Redaction (Permanent Blackout) Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Forensic Text Redaction (Permanent Blackout)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Search confidential words, CNP, SSN, or IBAN numbers. Draws opaque black boxes and rasterizes the page so the text layer cannot be highlighted, recovered, or extracted via scrapers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = redactQuery,
                        onValueChange = { redactQuery = it },
                        label = { Text("Word or Phrase to Redact (e.g. CNP / Name)") },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = overlayText,
                        onValueChange = { overlayText = it },
                        label = { Text("Overlay Box Label") },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quick Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("CNP", "IBAN", "CONFIDENTIAL", "SSN").forEach { preset ->
                        AssistChip(
                            onClick = { redactQuery = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Forensic Vector Sanitization", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Rasterizes redacted pages to obliterate underlying text bytes completely.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = forensicSanitize,
                        onCheckedChange = { forensicSanitize = it }
                    )
                }

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        if (redactQuery.isBlank()) {
                            statusText = "Please enter text or number to redact."
                            return@Button
                        }
                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_redacted.pdf") ?: return@Button
                        isProcessing = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val matches = DesktopPdfEngine.redactPdf(
                                    inputFile = file,
                                    outputFile = outFile,
                                    query = redactQuery.trim(),
                                    overlayText = overlayText.trim(),
                                    forensicSanitize = forensicSanitize
                                )
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    lastSecurityFile = outFile
                                    statusText = if (matches > 0) {
                                        "Success! Redacted $matches occurrence(s) across document.\nSaved to: ${outFile.absolutePath}"
                                    } else {
                                        "No occurrences of '$redactQuery' found in document text."
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    statusText = "Redaction failed: ${e.message}"
                                }
                            }
                        }
                    },
                    enabled = !isProcessing && file != null,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Redact & Sanitize Document", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Smart PII Redaction Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Smart PII Redaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Automatically scan and permanently censor Personally Identifiable Information (PII). Uses Forensic Vector Sanitization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RedactPattern.values().forEach { pattern ->
                        FilterChip(
                            selected = smartRedactPatterns.contains(pattern),
                            onClick = {
                                if (smartRedactPatterns.contains(pattern)) {
                                    smartRedactPatterns -= pattern
                                } else {
                                    smartRedactPatterns += pattern
                                }
                            },
                            label = { Text(pattern.label) },
                            leadingIcon = if (smartRedactPatterns.contains(pattern)) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Button(
                    onClick = {
                        if (file == null) {
                            statusText = "Please select a PDF document first."
                            return@Button
                        }
                        if (smartRedactPatterns.isEmpty()) {
                            statusText = "Please select at least one PII pattern."
                            return@Button
                        }
                        val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_smart_redacted.pdf") ?: return@Button
                        isProcessing = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val out = DesktopPdfEngine.smartRedact(
                                    inputFile = file,
                                    outputFile = outFile,
                                    patterns = smartRedactPatterns.toList()
                                )
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    lastSecurityFile = out
                                    statusText = "Smart Redaction completed!\nSaved to: ${out.absolutePath}"
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    statusText = "Smart Redaction failed: ${e.message}"
                                }
                            }
                        }
                    },
                    enabled = !isProcessing && file != null && smartRedactPatterns.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Shield, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-Scrub Selected PII", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 4. Password Protection (128-bit AES)
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
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(
                            onClick = { showPassword = !showPassword },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (showPassword) "Hide" else "Show",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true
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
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DesktopPdfEngine.encryptPdf(file, outFile, password)
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        lastSecurityFile = outFile
                                        statusText = "Document encrypted and saved to:\n${outFile.absolutePath}"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        statusText = "Encryption failed: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing && file != null,
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
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    DesktopPdfEngine.decryptPdf(file, outFile, password)
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        lastSecurityFile = outFile
                                        statusText = "Password removed! Saved to:\n${outFile.absolutePath}"
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        statusText = "Decryption failed (check password): ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing && file != null,
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

        // 4. PDF Recovery & Repair Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(strings.repairTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    strings.repairSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (repairResult != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                strings.repairDiagnosticsTitle,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            repairResult!!.issuesRepaired.forEach { issue ->
                                Text("• $issue", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Pages Recovered: ${repairResult!!.pagesRecovered} | Output Size: ${formatFileSize(repairResult!!.repairedSize)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val targetFile = file ?: DesktopFileDialog.openPdf() ?: run {
                                statusText = "Please choose a corrupted or damaged PDF to repair."
                                return@Button
                            }
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${targetFile.nameWithoutExtension}_repaired.pdf") ?: return@Button
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                val result = DesktopPdfEngine.repairCorruptedPdf(targetFile, outFile)
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    repairResult = result
                                    if (result.isSuccess) {
                                        lastSecurityFile = outFile
                                        statusText = strings.repairSuccess.format(result.pagesRecovered, outFile.absolutePath)
                                    } else {
                                        statusText = strings.repairFailed.format(result.issuesRepaired.firstOrNull() ?: "Corrupted stream")
                                    }
                                }
                            }
                        },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.btnRepairPdf, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val picked = DesktopFileDialog.openPdf()
                            if (picked != null) onFileChange(picked)
                        },
                        modifier = Modifier.height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Damaged File")
                    }
                }
            }
        }

        // 5. Embedded File Attachments & Portfolio Studio Card
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.secAttachmentsTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            strings.secAttachmentsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (attachmentsList.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (attachmentsList.isNotEmpty()) String.format(strings.attachmentsCount, attachmentsList.size) else "0 Attachments",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (attachmentsList.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (attachmentsList.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        attachmentsList.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "${formatFileSize(att.sizeBytes)} • ${att.mimeType}${if (att.description != null) " • ${att.description}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (file == null) return@Button
                                            val targetDir = DesktopFileDialog.chooseDirectory() ?: return@Button
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val extracted = DesktopPdfEngine.extractAttachment(file, att.name, targetDir)
                                                    withContext(Dispatchers.Main) {
                                                        if (extracted != null) {
                                                            lastSecurityFile = extracted
                                                            statusText = "Extracted '${att.name}' to:\n${extracted.absolutePath}"
                                                        } else {
                                                            statusText = "Failed to extract '${att.name}'."
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        statusText = "Extraction error: ${e.message}"
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Extract", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                } else if (file != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            strings.noAttachmentsFound,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (file == null) {
                                statusText = "Please select a PDF document first."
                                return@Button
                            }
                            val toEmbed = DesktopFileDialog.openAnyFile(title = "Select File to Embed into PDF") ?: return@Button
                            val outFile = DesktopFileDialog.savePdf(suggestedName = "${file.nameWithoutExtension}_portfolio.pdf") ?: return@Button
                            isProcessing = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val ok = DesktopPdfEngine.embedAttachment(file, toEmbed, outFile)
                                    val updatedList = DesktopPdfEngine.listAttachments(outFile)
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        if (ok) {
                                            lastSecurityFile = outFile
                                            attachmentsList = updatedList
                                            statusText = String.format(strings.embedSuccess, outFile.absolutePath)
                                        } else {
                                            statusText = "Failed to embed file."
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isProcessing = false
                                        statusText = "Embed error: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = file != null && !isProcessing,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.btnEmbedFile, fontWeight = FontWeight.Bold)
                    }

                    if (attachmentsList.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (file == null) return@OutlinedButton
                                val targetDir = DesktopFileDialog.chooseDirectory() ?: return@OutlinedButton
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        var count = 0
                                        attachmentsList.forEach { att ->
                                            val res = DesktopPdfEngine.extractAttachment(file, att.name, targetDir)
                                            if (res != null) count++
                                        }
                                        withContext(Dispatchers.Main) {
                                            lastSecurityFile = targetDir
                                            statusText = String.format(strings.extractSuccess, targetDir.absolutePath)
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            statusText = "Batch extraction error: ${e.message}"
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.btnExtractAllAttachments)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (lastSecurityFile != null && lastSecurityFile!!.exists()) {
                            Button(
                                onClick = { openDocument(lastSecurityFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("📄 Open PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { openFileInExplorer(lastSecurityFile!!) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Folder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 8B. DOCUMENT COMPARE & REVISION DIFF VIEW (ACROBAT PARITY)
// -------------------------------------------------------------------------------------------------
@Composable
private fun CompareView(file: File?, onFileChange: (File) -> Unit) {
    val strings = DesktopLocalization.strings
    var docA by remember(file) { mutableStateOf<File?>(file) }
    var docB by remember { mutableStateOf<File?>(null) }
    var isComparing by remember { mutableStateOf(false) }
    var diffSummary by remember { mutableStateOf<PdfDiffSummary?>(null) }
    var currentPageIndex by remember { mutableStateOf(0) }
    var thumbnailA by remember { mutableStateOf<ImageBitmap?>(null) }
    var thumbnailB by remember { mutableStateOf<ImageBitmap?>(null) }

    // Auto-run comparison when both docA and docB are chosen
    LaunchedEffect(docA, docB) {
        val a = docA
        val b = docB
        if (a != null && b != null && a.exists() && b.exists()) {
            isComparing = true
            withContext(Dispatchers.IO) {
                try {
                    val summary = DesktopPdfEngine.compareDocuments(a, b)
                    diffSummary = summary
                    currentPageIndex = 0
                } catch (_: Exception) {
                    diffSummary = null
                } finally {
                    isComparing = false
                }
            }
        } else {
            diffSummary = null
        }
    }

    // Load thumbnails for current page
    LaunchedEffect(docA, docB, currentPageIndex) {
        val a = docA
        val b = docB
        withContext(Dispatchers.IO) {
            thumbnailA = if (a != null && a.exists()) {
                runCatching {
                    val totalA = DesktopPdfEngine.getPageCount(a)
                    if (currentPageIndex < totalA) {
                        DesktopPdfEngine.renderThumbnail(a, currentPageIndex, targetWidth = 360).toComposeImageBitmap()
                    } else null
                }.getOrNull()
            } else null

            thumbnailB = if (b != null && b.exists()) {
                runCatching {
                    val totalB = DesktopPdfEngine.getPageCount(b)
                    if (currentPageIndex < totalB) {
                        DesktopPdfEngine.renderThumbnail(b, currentPageIndex, targetWidth = 360).toComposeImageBitmap()
                    } else null
                }.getOrNull()
            } else null
        }
    }

    val maxPages = remember(docA, docB, diffSummary) {
        val countA = diffSummary?.pagesA ?: docA?.let { runCatching { DesktopPdfEngine.getPageCount(it) }.getOrDefault(1) } ?: 1
        val countB = diffSummary?.pagesB ?: docB?.let { runCatching { DesktopPdfEngine.getPageCount(it) }.getOrDefault(1) } ?: 1
        maxOf(countA, countB).coerceAtLeast(1)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.tabCompare, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(strings.toolCompareDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Swap Documents button if both exist
            if (docA != null && docB != null) {
                OutlinedButton(
                    onClick = {
                        val temp = docA
                        docA = docB
                        docB = temp
                        if (docA != null) onFileChange(docA!!)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.CallMerge, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Swap Docs", fontSize = 12.sp)
                }
            }
        }

        // Dual Document Pickers Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document A Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.compareDocA, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        Button(
                            onClick = {
                                val picked = DesktopFileDialog.openPdf()
                                if (picked != null) {
                                    docA = picked
                                    onFileChange(picked)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(strings.compareChooseDocA, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (docA != null) {
                        Text(docA!!.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatFileSize(docA!!.length()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("No document selected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Document B Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.compareDocB, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                        Button(
                            onClick = {
                                val picked = DesktopFileDialog.openPdf()
                                if (picked != null) docB = picked
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(strings.compareChooseDocB, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (docB != null) {
                        Text(docB!!.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatFileSize(docB!!.length()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("No revision document selected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Empty prompt if either doc is missing
        if (docA == null || docB == null) {
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Collections, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(strings.compareSelectBoth, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select both the original PDF and the revised version to see line-by-line diffs and synchronized page comparisons.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
        } else if (isComparing) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (diffSummary != null) {
            val summary = diffSummary!!

            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summary.isEntirelyIdentical) Color(0xFF16A34A).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, if (summary.isEntirelyIdentical) Color(0xFF16A34A).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            if (summary.isEntirelyIdentical) Icons.Rounded.Shield else Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = if (summary.isEntirelyIdentical) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                if (summary.isEntirelyIdentical) strings.compareIdentical else strings.compareDiffCount.format(summary.totalAddedLines + summary.totalRemovedLines),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (!summary.isEntirelyIdentical) {
                                Text(
                                    strings.compareStats.format(summary.totalAddedLines, summary.totalRemovedLines, summary.pageDiffs.size),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!summary.isEntirelyIdentical) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = Color(0xFF16A34A).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("+${summary.totalAddedLines}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Surface(color = Color(0xFFDC2626).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("-${summary.totalRemovedLines}", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Synchronized Page Navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Comparing Page ${currentPageIndex + 1} of $maxPages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                        enabled = currentPageIndex > 0
                    ) {
                        Text("◀", fontWeight = FontWeight.Bold)
                    }
                    Text("${currentPageIndex + 1} / $maxPages", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = { if (currentPageIndex < maxPages - 1) currentPageIndex++ },
                        enabled = currentPageIndex < maxPages - 1
                    ) {
                        Text("▶", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Side-by-Side Visual Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Doc A Page Preview
                Card(
                    modifier = Modifier.weight(1f).height(380.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Document A (Page ${currentPageIndex + 1})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            if (thumbnailA != null) {
                                Image(bitmap = thumbnailA!!, contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp))
                            } else {
                                Text("No page", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Doc B Page Preview
                Card(
                    modifier = Modifier.weight(1f).height(380.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Document B (Page ${currentPageIndex + 1})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            if (thumbnailB != null) {
                                Image(bitmap = thumbnailB!!, contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp))
                            } else {
                                Text("No page", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Current Page Textual Differences Drawer
            val currentPageDiff = summary.pageDiffs.getOrNull(currentPageIndex)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Page ${currentPageIndex + 1} Line-by-Line Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    if (currentPageDiff == null || currentPageDiff.isIdentical) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Text("No text differences detected on this page.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            currentPageDiff.removedLines.forEach { line ->
                                Surface(
                                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "- $line",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color(0xFFDC2626),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            currentPageDiff.addedLines.forEach { line ->
                                Surface(
                                    color = Color(0xFF16A34A).copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "+ $line",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = Color(0xFF16A34A),
                                        fontSize = 11.sp
                                    )
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
// REUSABLE COMMUNITY EDGE CASE CALLOUT BAR
// -------------------------------------------------------------------------------------------------
@Composable
private fun EdgeCaseCallout() {
    val strings = DesktopLocalization.strings
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AllInclusive, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    strings.edgeCaseCalloutText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = {
                    try {
                        if (java.awt.Desktop.isDesktopSupported()) {
                            java.awt.Desktop.getDesktop().browse(java.net.URI("https://github.com/kiss2oblivion/pdfchemy/issues/new"))
                        }
                    } catch (_: Exception) {}
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(strings.edgeCaseCalloutBtn, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

private fun openBrowser(url: String) {
    try {
        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        }
    } catch (_: Exception) {}
}

// -------------------------------------------------------------------------------------------------
// THE TIP JAR DIALOG (WINDOWS & LINUX INDEPENDENT SUPPORT)
// -------------------------------------------------------------------------------------------------
@Composable
private fun TipJarDialog(onDismiss: () -> Unit) {
    val strings = DesktopLocalization.strings
    var isTagCopied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.close)
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFFF5E5B).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF5E5B),
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    strings.tipJarTitle,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        strings.tipJarSubtitle,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .width(540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Developer Note Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text(
                            strings.tipJarDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Preset Tip Tiers (Quick Action Cards)
                Text("Select a Tip Tier:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Espresso $3
                    Card(
                        onClick = { openBrowser("https://ko-fi.com/andreiioancucos") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(strings.tierCoffee, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(strings.tierCoffeeDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Snack & Coffee $6
                    Card(
                        onClick = { openBrowser("https://ko-fi.com/andreiioancucos") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(strings.tierSnack, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(strings.tierSnackDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Late Night Pizza $12
                    Card(
                        onClick = { openBrowser("https://revolut.me/andreiy886") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(strings.tierPizza, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(strings.tierPizzaDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Super Supporter $25
                    Card(
                        onClick = { openBrowser("https://revolut.me/andreiy886") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color(0xFFFF5E5B).copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(strings.tierSupporter, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFF5E5B))
                            Text(strings.tierSupporterDesc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Direct Payment Buttons
                Text("Direct Channels:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { openBrowser("https://ko-fi.com/andreiioancucos") },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5E5B), contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnKofi, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { openBrowser("https://revolut.me/andreiy886") },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0075EB), contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnRevolut, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Copy Revolut Tag & GitHub Star Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val sel = java.awt.datatransfer.StringSelection("@andreiy886")
                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, null)
                                isTagCopied = true
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            if (isTagCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isTagCopied) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isTagCopied) strings.revolutTagCopied else strings.btnCopyRevolutTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { openBrowser("https://github.com/kiss2oblivion/pdfchemy") },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnStarGitHub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Friendly closing
                Text(
                    "❤️ Andrei Ioan Cucoș (John)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// UPDATE AVAILABLE DIALOG
// -------------------------------------------------------------------------------------------------
@Composable
private fun UpdateAvailableDialog(
    release: ReleaseInfo,
    onDismiss: () -> Unit,
    onDismissForever: () -> Unit
) {
    val strings = DesktopLocalization.strings

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    openBrowser(release.htmlUrl)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.btnDownloadUpdate, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismissForever) {
                    Text(strings.btnRemindLater)
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.close)
                }
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.CloudUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    strings.updateAvailableTitle.format(release.tagName),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Current: v${DesktopUpdateManager.CURRENT_VERSION}  ➔  New: ${release.tagName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    release.name.ifEmpty { "Release ${release.tagName}" },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (release.body.isNotBlank()) {
                    Text(
                        strings.whatsNew,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                release.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// THE LIFETIME MANIFESTO DIALOG
// -------------------------------------------------------------------------------------------------
@Composable
private fun ManifestoDialog(
    onDismiss: () -> Unit,
    onOpenTipJar: () -> Unit = {}
) {
    val strings = DesktopLocalization.strings

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.manifestoGotIt)
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AllInclusive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        },
        title = {
            Text(
                strings.manifestoDialogTitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .width(520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "\"Lifetime of updates until I personally die and free of charge; and all you have to do is make a solid valid request and it will be done and implemented; cuz it's from the people to the people; it may or may not be the same as a corpo app would do but at least it's gonna be free and I will make it as best as I possibly can; if I can't well I can't and that's that at least you have an option oh you enigmatic edge case that you are.\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "— Andrei Ioan Cucoș (John), Independent Developer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text("Our 4 Unbreakable Guarantees:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuaranteeRow(strings.guarantee1Title, strings.guarantee1Desc)
                    GuaranteeRow(strings.guarantee2Title, strings.guarantee2Desc)
                    GuaranteeRow(strings.guarantee3Title, strings.guarantee3Desc)
                    GuaranteeRow(strings.guarantee4Title, strings.guarantee4Desc)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { openBrowser("https://github.com/kiss2oblivion/pdfchemy/issues/new") },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnRequestEdgeCase, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { openBrowser("mailto:cucosandreiioan@gmail.com?subject=PDFchemy%20Feedback") },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.btnEmailJohn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onOpenTipJar()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5E5B))
                ) {
                    Icon(Icons.Rounded.Favorite, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFFFF5E5B))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.btnOpenTipJar, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun GuaranteeRow(title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun openFileInExplorer(target: File) {
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            val desktop = java.awt.Desktop.getDesktop()
            val fileToOpen = if (target.isDirectory) target else target.parentFile ?: target
            if (fileToOpen.exists()) {
                desktop.open(fileToOpen)
            }
        }
    } catch (_: Exception) {}
}

private fun openDocument(target: File) {
    try {
        if (java.awt.Desktop.isDesktopSupported() && target.exists()) {
            java.awt.Desktop.getDesktop().open(target)
        }
    } catch (_: Exception) {}
}

// -------------------------------------------------------------------------------------------------
// INSTALLATION & FIRST-RUN SETUP DIALOG
// -------------------------------------------------------------------------------------------------
@Composable
private fun InstallationSetupDialog(onDismiss: () -> Unit) {
    val currentLang by DesktopLocalization.currentLanguageState
    val strings = DesktopLocalization.strings
    val detectedSystemLang = remember { DesktopLanguage.detectSystemLanguage() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(strings.setupContinue, fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        },
        title = {
            Text(
                strings.setupWelcomeTitle,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .width(540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    strings.setupWelcomeSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Detected OS Language Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            String.format(strings.setupDetectedHint, "${detectedSystemLang.nativeName} (${detectedSystemLang.englishName})"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Grid of 21 locales / 20 languages
                val langs = DesktopLanguage.entries
                val columns = 2
                val rows = (langs.size + columns - 1) / columns

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until columns) {
                                val idx = r * columns + c
                                if (idx < langs.size) {
                                    val lang = langs[idx]
                                    val isSelected = currentLang == lang
                                    Surface(
                                        onClick = { DesktopLocalization.currentLanguage = lang },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    lang.nativeName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    lang.englishName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
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
    )
}


