package com.example.shrinkpdf

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shrinkpdf.ui.MainViewModel
import com.example.shrinkpdf.logic.PdfAnalysis
import com.example.shrinkpdf.ui.OrganizeCategoryScreen
import com.example.shrinkpdf.ui.MergePdfScreen
import com.example.shrinkpdf.ui.SplitPdfScreen
import com.example.shrinkpdf.ui.textconverter.TextConverterViewModel
import com.example.shrinkpdf.ui.textconverter.TextConverterScreen
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.font.FontWeight
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.TileMode

// --- Color Palette ---
val md_theme_light_primary = Color(0xFF0072B2)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD6E2F0)
val md_theme_light_onPrimaryContainer = Color(0xFF001D36)
val md_theme_light_secondary = Color(0xFF009E73)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFB3E6D8)
val md_theme_light_onSecondaryContainer = Color(0xFF002B1D)
val md_theme_light_tertiary = Color(0xFFD55E00)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFDBC7)
val md_theme_light_onTertiaryContainer = Color(0xFF451A00)
val md_theme_light_background = Color(0xFFF8F9FA)
val md_theme_light_onBackground = Color(0xFF1B1B1F)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF1B1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE1E2E8)
val md_theme_light_onSurfaceVariant = Color(0xFF44474E)
val md_theme_light_outline = Color(0xFF74777F)

val md_theme_dark_primary = Color(0xFF00E5FF) // Neon Cyan
val md_theme_dark_onPrimary = Color(0xFF00363D)
val md_theme_dark_primaryContainer = Color(0xFF004F58)
val md_theme_dark_onPrimaryContainer = Color(0xFF99F5FF)
val md_theme_dark_secondary = Color(0xFFD500F9) // Magic Purple
val md_theme_dark_onSecondary = Color(0xFF4A0059)
val md_theme_dark_secondaryContainer = Color(0xFF7B0094)
val md_theme_dark_onSecondaryContainer = Color(0xFFF4B3FF)
val md_theme_dark_tertiary = Color(0xFFFFEA00) // Glowing Gold
val md_theme_dark_onTertiary = Color(0xFF332F00)
val md_theme_dark_tertiaryContainer = Color(0xFF4D4700)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFF599)
val md_theme_dark_background = Color(0xFF0B0B11) // Deep Dark Navy/Charcoal
val md_theme_dark_onBackground = Color(0xFFE3E2E6)
val md_theme_dark_surface = Color(0xFF13131C) // Slightly lighter than bg
val md_theme_dark_onSurface = Color(0xFFE3E2E6)
val md_theme_dark_surfaceVariant = Color(0xFF282836)
val md_theme_dark_onSurfaceVariant = Color(0xFFC4C6D0)
val md_theme_dark_outline = Color(0xFF8E9099)

val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline
)

val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline
)

private val defaultTypography = Typography()
val AppTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = FontFamily.SansSerif),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = FontFamily.SansSerif),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
)

@Composable
fun ShrinkPdfTheme(
    useDarkTheme: Boolean = true, // Force Dark Theme
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        LightColors
    } else {
        DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

fun playFeedback(view: android.view.View, isHaptic: Boolean, isSfx: Boolean) {
    if (isSfx) {
        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
    }
    if (isHaptic) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        // Do not preload ads on launch while debugging the home UI.
        // On some emulator/WebView setups, ad preload can create a blank black surface over Compose.
        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            val useDark = isDarkTheme
            
            ShrinkPdfTheme(useDarkTheme = useDark) {
                MainApp(
                    isDarkTheme = useDark,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        )
    }
}

@Composable
fun MainApp(
    viewModel: MainViewModel = viewModel(),
    textConverterViewModel: TextConverterViewModel = viewModel(),
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initBilling(context)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            

        when (currentScreen) {
            Screen.Home -> HomeScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigate = { screen -> currentScreen = screen }
            )
            Screen.CompressCategory -> CompressCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.CompressPdf -> CompressPdfScreen(viewModel, 0) { currentScreen = Screen.CompressCategory }
            Screen.BatchCompressPdf -> CompressPdfScreen(viewModel, 1) { currentScreen = Screen.CompressCategory }
            Screen.TextToPdf -> TextToPdfScreen(viewModel) { currentScreen = Screen.CreateCategory }
            Screen.TextConverter -> TextConverterScreen(textConverterViewModel) { currentScreen = Screen.CreateCategory }
            Screen.Settings -> SettingsScreen(viewModel) { currentScreen = Screen.Home }
            Screen.CreateCategory -> CreateCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.OrganizeCategory -> OrganizeCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.MergePdf -> MergePdfScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.SplitPdf -> SplitPdfScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.CheckCategory -> PlaceholderCategoryScreen("Check", onBack = { currentScreen = Screen.Home })
        }

        if (uiState is MainViewModel.UiState.Processing || uiState is MainViewModel.UiState.BatchProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (uiState is MainViewModel.UiState.BatchProcessing) {
                        val state = uiState as MainViewModel.UiState.BatchProcessing
                        Text(
                            text = "Compressing ${state.current} of ${state.total} files...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.currentFileName,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.current.toFloat() / state.total },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    } else {
                        Text("Compressing PDF...", color = Color.White)
                    }
                }
            }
        }
        // Dynamic Result Dialog
        when (val state = uiState) {
            is MainViewModel.UiState.Success -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = { Text(state.title) },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text("Great")
                        }
                    }
                )
            }
            is MainViewModel.UiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = { Text("Error") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text("OK")
                        }
                    }
                )
            }
            else -> {}
        }

        val warning by viewModel.warning.collectAsState()
        LaunchedEffect(warning) {
            warning?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.dismissWarning()
            }
        }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object CompressPdf : Screen()
    object BatchCompressPdf : Screen()
    object TextToPdf : Screen()
    object TextConverter : Screen()
    object Settings : Screen()
    object CompressCategory : Screen()
    object CreateCategory : Screen()
    object OrganizeCategory : Screen()
    object MergePdf : Screen()
    object SplitPdf : Screen()
    object CheckCategory : Screen()
}

/**
 * Animated title with a subtle shimmer highlight that sweeps across the text.
 * Content is always visible — the shimmer is purely additive.
 */
@Composable
fun ShimmerTitle(text: String, style: androidx.compose.ui.text.TextStyle, baseColor: Color, isDarkTheme: Boolean) {
    val shimmerAnim = remember { Animatable(-1f) }
    
    LaunchedEffect(Unit) {
        // Run once initially
        shimmerAnim.animateTo(
            targetValue = 2f,
            animationSpec = tween(3000, easing = LinearEasing)
        )
        
        while (true) {
            // Random delay between 5 and 10 minutes (in milliseconds)
            val delayMillis = (300_000L..600_000L).random()
            delay(delayMillis)
            
            shimmerAnim.snapTo(-1f)
            shimmerAnim.animateTo(
                targetValue = 2f,
                animationSpec = tween(3000, easing = LinearEasing)
            )
        }
    }

    val shimmerOffset = shimmerAnim.value

    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.secondary
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(c1, c2, Color.White, c2, c1),
        start = Offset(shimmerOffset * 800f, 0f),
        end = Offset(shimmerOffset * 800f + 600f, 0f)
    )

    val textShadow = if (isDarkTheme) {
        androidx.compose.ui.graphics.Shadow(
            color = c1.copy(alpha = 0.8f),
            offset = Offset.Zero,
            blurRadius = 16f
        )
    } else {
        androidx.compose.ui.graphics.Shadow(
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f),
            offset = Offset(2f, 2f),
            blurRadius = 2f
        )
    }

    Text(
        text = text,
        style = style.copy(
            brush = shimmerBrush,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            shadow = textShadow
        )
    )
}

@Composable
fun AnimatedMeshBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")
    val color1 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        targetValue = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
        targetValue = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "color2"
    )

    Canvas(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.2f),
                radius = size.width * 0.8f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.8f),
                radius = size.width * 0.8f
            )
        )
    }
}

@Composable
fun HomeScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    // --- Safe entrance animation ---
    // Content is ALWAYS composed. Animation only affects visual properties (alpha, translation)
    // via graphicsLayer so the layout tree is never empty.
    val headerAlpha = remember { Animatable(0f) }
    val headerOffsetY = remember { Animatable(30f) }
    val cardsAlpha = remember { Animatable(0f) }
    val cardsOffsetY = remember { Animatable(30f) }

    // Run the entrance animation
    LaunchedEffect(Unit) {
        try {
            // Header fades in first
            launch {
                headerAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                headerOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
            // Cards follow with a short delay
            launch {
                delay(200)
                cardsAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                cardsOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
            }
        } catch (_: Exception) {
            // Safety: if animation fails for any reason, snap to fully visible
            headerAlpha.snapTo(1f)
            headerOffsetY.snapTo(0f)
            cardsAlpha.snapTo(1f)
            cardsOffsetY.snapTo(0f)
        }
    }

    // Safety timeout: force everything visible after 2 seconds no matter what
    LaunchedEffect(Unit) {
        delay(2000)
        if (headerAlpha.value < 1f) headerAlpha.snapTo(1f)
        if (headerOffsetY.value != 0f) headerOffsetY.snapTo(0f)
        if (cardsAlpha.value < 1f) cardsAlpha.snapTo(1f)
        if (cardsOffsetY.value != 0f) cardsOffsetY.snapTo(0f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedMeshBackground()

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { onNavigate(Screen.Settings) }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer {
                            alpha = headerAlpha.value
                            translationX = -headerOffsetY.value  // slide from left in landscape
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = CircleShape)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ShimmerTitle(
                        text = "PDFchemy Tools",
                        style = MaterialTheme.typography.headlineMedium,
                        baseColor = MaterialTheme.colorScheme.primary,
                        isDarkTheme = isDarkTheme
                    )
                    Text(
                        text = "FIX DOCUMENTS LOCALLY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .padding(vertical = 8.dp)
                        .graphicsLayer {
                            alpha = cardsAlpha.value
                            translationX = cardsOffsetY.value  // slide from right in landscape
                        }
                ) {
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryCard("Compress", "Make PDFs smaller, safely.", Icons.Rounded.Compress, { onNavigate(Screen.CompressCategory) }, Modifier.weight(1f).fillMaxHeight())
                        CategoryCard("Create", "Turn text into useful files.", Icons.Rounded.AddCircleOutline, { onNavigate(Screen.CreateCategory) }, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryCard("Organize", "Merge, split, rename.", Icons.Rounded.FolderOpen, { onNavigate(Screen.OrganizeCategory) }, Modifier.weight(1f).fillMaxHeight())
                        CategoryCard("Check", "Inspect before sending.", Icons.Rounded.FactCheck, { onNavigate(Screen.CheckCategory) }, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = headerAlpha.value
                        translationY = -headerOffsetY.value  // slide down from above
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), shape = CircleShape)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = "App Logo",
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    ShimmerTitle(
                        text = "PDFchemy Tools",
                        style = MaterialTheme.typography.headlineLarge,
                        baseColor = MaterialTheme.colorScheme.primary,
                        isDarkTheme = isDarkTheme
                    )
                    Text(
                        text = "FIX DOCUMENTS LOCALLY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 48.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.graphicsLayer {
                        alpha = cardsAlpha.value
                        translationY = cardsOffsetY.value  // slide up from below
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CategoryCard("Compress", "Make PDFs smaller, safely.", Icons.Rounded.Compress, { onNavigate(Screen.CompressCategory) }, Modifier.weight(1f))
                        CategoryCard("Create", "Turn text into useful files.", Icons.Rounded.AddCircleOutline, { onNavigate(Screen.CreateCategory) }, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CategoryCard("Organize", "Merge, split, rename.", Icons.Rounded.FolderOpen, { onNavigate(Screen.OrganizeCategory) }, Modifier.weight(1f))
                        CategoryCard("Check", "Inspect before sending.", Icons.Rounded.FactCheck, { onNavigate(Screen.CheckCategory) }, Modifier.weight(1f))
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleAnim"
    )
    val view = androidx.compose.ui.platform.LocalView.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(isPressed) {
        if (isPressed) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    val cardModifier = if (isLandscape) {
        modifier.scale(scale)
    } else {
        modifier.height(160.dp).scale(scale)
    }

    Card(
        onClick = onClick,
        modifier = cardModifier,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1.5f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    Scaffold(containerColor = Color.Transparent, 
        topBar = {
            TopAppBar(
                title = { Text("Create") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent), navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToolCard(
                title = "Text to PDF",
                subtitle = "Convert your notes or .txt files to PDF",
                icon = Icons.Rounded.Description,
                onClick = { onNavigate(Screen.TextToPdf) }
            )
            ToolCard(
                title = "Text Format Converter",
                subtitle = "Convert between TXT, MD, CSV, JSON, and more",
                icon = Icons.Rounded.SyncAlt,
                onClick = { onNavigate(Screen.TextConverter) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressCategoryScreen(onNavigate: (Screen) -> Unit, onBack: () -> Unit) {
    BackHandler { onBack() }
    Scaffold(containerColor = Color.Transparent, 
        topBar = {
            TopAppBar(
                title = { Text("Compress") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent), navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ToolCard(
                title = "Compress PDF",
                subtitle = "Make PDFs smaller, safely.",
                icon = Icons.Rounded.Compress,
                onClick = { onNavigate(Screen.CompressPdf) }
            )
            ToolCard(
                title = "Batch Compression",
                subtitle = "Select a folder and compress all PDFs inside",
                icon = Icons.Rounded.LibraryBooks,
                onClick = { onNavigate(Screen.BatchCompressPdf) } // Routes to same tool where tab exists
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderCategoryScreen(title: String, onBack: () -> Unit) {
    BackHandler { onBack() }
    Scaffold(containerColor = Color.Transparent, 
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent), navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Construction, contentDescription = "Coming Soon", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Coming Soon", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scaleAnim"
    )
    val view = androidx.compose.ui.platform.LocalView.current

    Card(
        onClick = { 
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onClick() 
        },
        modifier = Modifier.fillMaxWidth().scale(scale),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            ).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CompressPdfScreen(viewModel: MainViewModel, initialTab: Int = 0, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val isHaptic by viewModel.isHapticEnabled.collectAsState()
    val isSfx by viewModel.isSfxEnabled.collectAsState()
    val currentQuality by viewModel.compressionQuality.collectAsState()
    val originalSize by viewModel.selectedFileSize.collectAsState()
    val pdfAnalysis by viewModel.pdfAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val useGrayscale by viewModel.useGrayscale.collectAsState()
    val useLossless by viewModel.useLossless.collectAsState()
    val stripMetadata by viewModel.stripMetadata.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    
    var showOfficialDocWarning by remember { mutableStateOf(false) }

    LaunchedEffect(pdfAnalysis) {
        if (pdfAnalysis?.scenario == com.example.shrinkpdf.logic.PdfScenario.SIGNED_OFFICIAL) {
            showOfficialDocWarning = true
        }
    }
    
    val selectedFiles by viewModel.selectedFiles.collectAsState()

    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = pagerState.currentPage

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var sourceName by remember { mutableStateOf<String?>(null) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            sourceUri = uri
            sourceName = com.example.shrinkpdf.utils.FileUtils.getFileName(context, uri) ?: "Selected PDF"
            viewModel.onFileSelected(context, uri)
        }
    }

    val pickMultiplePdfsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onFilesSelected(context, uris)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && sourceUri != null) {
            viewModel.compressPdf(context, sourceUri!!, uri)
        }
    }

    val selectDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.compressBatch(context, uri)
        }
    }

    BackHandler { onBack() }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(containerColor = Color.Transparent, 
        topBar = { PremiumTopAppBar("Compress PDF", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth(), containerColor = androidx.compose.ui.graphics.Color.Transparent, divider = {}) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { playFeedback(view, isHaptic, isSfx); coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text("Single File") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { playFeedback(view, isHaptic, isSfx); coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text("Batch Compression") }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (isLandscape) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            LeftPanel(
                                selectedTab = page,
                                sourceUri = sourceUri,
                                sourceName = sourceName,
                                originalSize = originalSize,
                                selectedFiles = selectedFiles,
                                isAnalyzing = isAnalyzing,
                                pdfAnalysis = pdfAnalysis,
                                currentQuality = currentQuality,
                                useGrayscale = useGrayscale,
                                useLossless = useLossless,
                                viewModel = viewModel,
                                onPickSingle = { pickPdfLauncher.launch(arrayOf("application/pdf", "application/x-pdf")) },
                                onPickMultiple = { pickMultiplePdfsLauncher.launch(arrayOf("application/pdf", "application/x-pdf")) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            RightPanel(
                                selectedTab = page,
                                sourceUri = sourceUri,
                                sourceName = sourceName,
                                originalSize = originalSize,
                                selectedFiles = selectedFiles,
                                currentQuality = currentQuality,
                                recQuality = pdfAnalysis?.recommendedQuality,
                                useGrayscale = useGrayscale,
                                useLossless = useLossless,
                                stripMetadata = stripMetadata,
                                pdfAnalysis = pdfAnalysis,
                                viewModel = viewModel,
                                onSaveSingle = { savePdfLauncher.launch("compressed_${sourceName ?: "file.pdf"}") },
                                onSaveBatch = { selectDirectoryLauncher.launch(null) }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LeftPanel(
                            selectedTab = page,
                            sourceUri = sourceUri,
                            sourceName = sourceName,
                            originalSize = originalSize,
                            selectedFiles = selectedFiles,
                            isAnalyzing = isAnalyzing,
                            pdfAnalysis = pdfAnalysis,
                            currentQuality = currentQuality,
                            useGrayscale = useGrayscale,
                            useLossless = useLossless,
                            viewModel = viewModel,
                            onPickSingle = { pickPdfLauncher.launch(arrayOf("application/pdf", "application/x-pdf")) },
                            onPickMultiple = { pickMultiplePdfsLauncher.launch(arrayOf("application/pdf", "application/x-pdf")) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        RightPanel(
                            selectedTab = page,
                            sourceUri = sourceUri,
                            sourceName = sourceName,
                            originalSize = originalSize,
                            selectedFiles = selectedFiles,
                            currentQuality = currentQuality,
                            recQuality = pdfAnalysis?.recommendedQuality,
                            useGrayscale = useGrayscale,
                            useLossless = useLossless,
                            stripMetadata = stripMetadata,
                            pdfAnalysis = pdfAnalysis,
                            viewModel = viewModel,
                            onSaveSingle = { 
                                com.example.shrinkpdf.ads.AdManager.showAd(context as android.app.Activity, isPremium) {
                                    savePdfLauncher.launch("compressed_${sourceName ?: "file.pdf"}") 
                                }
                            },
                            onSaveBatch = { 
                                com.example.shrinkpdf.ads.AdManager.showAd(context as android.app.Activity, isPremium) {
                                    selectDirectoryLauncher.launch(null) 
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showOfficialDocWarning) {
        AlertDialog(
            onDismissRequest = { showOfficialDocWarning = false },
            title = { Text("Quality & Legal Warning") },
            text = { Text("Compression reduces quality. This may invalidate official documents, medical records, or digital signatures. Always keep your original file.") },
            confirmButton = {
                TextButton(onClick = { showOfficialDocWarning = false }) {
                    Text("I Understand")
                }
            }
        )
    }
}

@Composable
fun LeftPanel(
    selectedTab: Int,
    sourceUri: Uri?,
    sourceName: String?,
    originalSize: Long,
    selectedFiles: List<MainViewModel.SelectedFile>,
    isAnalyzing: Boolean,
    pdfAnalysis: PdfAnalysis?,
    currentQuality: Float,
    useGrayscale: Boolean,
    useLossless: Boolean,
    viewModel: MainViewModel,
    onPickSingle: () -> Unit,
    onPickMultiple: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (selectedTab == 0) "Single PDF Optimization" else "Batch PDF Optimization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { if (selectedTab == 0) onPickSingle() else onPickMultiple() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedTab == 0) {
                        if (sourceUri == null) "Select PDF File" else "Change File"
                    } else {
                        if (selectedFiles.isEmpty()) "Select PDF Files" else "Change Files (${selectedFiles.size} selected)"
                    }
                )
            }
        }
    }

    if (selectedTab == 0) {
        sourceUri?.let {
            val estCompressedSize = if (originalSize > 0) {
                viewModel.estimateCompressedSize(
                    originalSize = originalSize,
                    quality = currentQuality,
                    useLossless = useLossless,
                    useGrayscale = useGrayscale,
                    scenario = pdfAnalysis?.scenario
                )
            } else 0L

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "File Name: $sourceName", style = MaterialTheme.typography.bodyLarge)
                    if (originalSize > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAnalyzing) "Original Size: ${viewModel.formatSize(originalSize)} (Analyzing...)" else "Original Size: ${viewModel.formatSize(originalSize)}  →  ${viewModel.formatSize(estCompressedSize)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = if (!isAnalyzing) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    } else {
        if (selectedFiles.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selected Batch Files:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    selectedFiles.forEach { file ->
                        val estCompressedSize = if (file.size > 0) {
                            viewModel.estimateCompressedSize(
                                originalSize = file.size,
                                quality = currentQuality,
                                useLossless = useLossless,
                                useGrayscale = useGrayscale,
                                scenario = null // Batch estimation uses generic ratio since analysis is per-file
                            )
                        } else 0L
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (file.isAnalyzing) "Analyzing..." else viewModel.formatSize(file.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!file.isAnalyzing) {
                                    Text(
                                        text = "→ ${viewModel.formatSize(estCompressedSize)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isAnalyzing,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Analyzing document structure...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }

    AnimatedVisibility(
        visible = pdfAnalysis != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        pdfAnalysis?.let { analysis ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Recommendation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Detected Type: ${analysis.scenario.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Pages: ${analysis.pageCount} | Images: ${analysis.imageCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = analysis.recommendationReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun RightPanel(
    selectedTab: Int,
    sourceUri: Uri?,
    sourceName: String?,
    originalSize: Long,
    selectedFiles: List<MainViewModel.SelectedFile>,
    currentQuality: Float,
    recQuality: Float?,
    useGrayscale: Boolean,
    useLossless: Boolean,
    stripMetadata: Boolean,
    pdfAnalysis: PdfAnalysis?,
    viewModel: MainViewModel,
    onSaveSingle: () -> Unit,
    onSaveBatch: () -> Unit
) {
    val totalOriginalSize = if (selectedTab == 0) originalSize else selectedFiles.sumOf { it.size }
    val hasSelection = if (selectedTab == 0) sourceUri != null else selectedFiles.isNotEmpty()

    if (!hasSelection) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.SettingsSuggest, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Optimization options will appear here once you select a file.", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Compression Preset:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompressionPresetButton("Smallest", 0.25f, currentQuality, recQuality == 0.25f) { viewModel.setQuality(0.25f) }
                CompressionPresetButton("Balanced", 0.50f, currentQuality, recQuality == 0.50f) { viewModel.setQuality(0.50f) }
                CompressionPresetButton("Best", 0.75f, currentQuality, recQuality == 0.75f) { viewModel.setQuality(0.75f) }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Compression options", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grayscale Conversion", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("Convert color images to black & white", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 28.dp))
                    if (useGrayscale && pdfAnalysis?.scenario != com.example.shrinkpdf.logic.PdfScenario.SCANNED_IMAGE_HEAVY) {
                        Text(
                            text = "⚠️ Discards all color elements & diagrams.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp, start = 28.dp)
                        )
                    }
                }
                Switch(checked = useGrayscale, onCheckedChange = { viewModel.setUseGrayscale(it) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FolderZip, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lossless ZIP Compression", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("Skip JPEG lossy compression (no artifacts)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 28.dp))
                    if (useLossless && pdfAnalysis?.scenario == com.example.shrinkpdf.logic.PdfScenario.SCANNED_IMAGE_HEAVY) {
                        Text(
                            text = "⚠️ Lossless on scans can significantly increase size.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp, start = 28.dp)
                        )
                    }
                }
                Switch(checked = useLossless, onCheckedChange = { viewModel.setUseLossless(it) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Tag, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove Metadata", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("Strip author, creator & editor tags", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 28.dp))
                    if (stripMetadata && pdfAnalysis?.scenario == com.example.shrinkpdf.logic.PdfScenario.SIGNED_OFFICIAL) {
                        Text(
                            text = "⚠️ May invalidate digital signatures on official files.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp, start = 28.dp)
                        )
                    }
                }
                Switch(checked = stripMetadata, onCheckedChange = { viewModel.setStripMetadata(it) })
            }
        }
    }

    val estCompressedSize = if (totalOriginalSize > 0) {
        when {
            useLossless -> (totalOriginalSize * 1.05).toLong()
            else -> {
                var ratio = when (currentQuality) {
                    0.25f -> 0.30f
                    0.50f -> 0.50f
                    else -> 0.75f
                }
                
                // If it's mostly text, compression won't be as effective, but we still apply a small ratio
                // so the user gets visual feedback that the algorithm was applied.
                if (pdfAnalysis?.scenario == com.example.shrinkpdf.logic.PdfScenario.TEXT_VECTOR) {
                    ratio = when (currentQuality) {
                        0.25f -> 0.85f
                        0.50f -> 0.90f
                        else -> 0.95f
                    }
                }
                
                val grayscaleDiscount = if (useGrayscale) 0.85f else 1.0f
                (totalOriginalSize * ratio * grayscaleDiscount).toLong()
            }
        }
    } else 0L

    val estCompressedSizeStr = viewModel.formatSize(estCompressedSize)
    val estSavingsPercent = if (totalOriginalSize > 0) {
        val diff = totalOriginalSize - estCompressedSize
        if (diff > 0) ((diff.toFloat() / totalOriginalSize) * 100).toInt() else 0
    } else 0

    AnimatedVisibility(
        visible = true,
        enter = expandVertically() + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Estimated size after compression",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(targetState = estCompressedSizeStr, label = "size_anim") { targetSize ->
                        Text(
                            text = if (totalOriginalSize > 0) "$targetSize (~$estSavingsPercent% smaller)" else "Select files to see projection",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = "Projected Size info",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    Button(
        onClick = { if (selectedTab == 0) onSaveSingle() else onSaveBatch() },
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Compress Icon"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (selectedTab == 0) "Optimize & Save PDF" else "Select Output Folder & Compress",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToPdfScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val isHaptic by viewModel.isHapticEnabled.collectAsState()
    val isSfx by viewModel.isSfxEnabled.collectAsState()
    val inputText by viewModel.inputText.collectAsState()

    val pickTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadTextFromFile(context, uri)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            viewModel.convertTextToPdf(context, uri)
        }
    }

    BackHandler { onBack() }

    Scaffold(containerColor = Color.Transparent, 
        topBar = { PremiumTopAppBar("Text to PDF", onBack) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.setInputText(it) },
                    label = { Text("Type or paste text here") },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    placeholder = { Text("Enter the content of your PDF...") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { pickTextLauncher.launch(arrayOf("text/plain")) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import .txt")
                }

                Button(
                    onClick = { 
                        if (inputText.isBlank()) {
                            Toast.makeText(context, "Please enter some text.", Toast.LENGTH_SHORT).show()
                        } else {
                            savePdfLauncher.launch("converted_text.pdf")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save as PDF")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressionPresetButton(
    label: String,
    quality: Float,
    currentQuality: Float,
    isRecommended: Boolean,
    onClick: () -> Unit
) {
    val isSelected = quality == currentQuality
    ElevatedFilterChip(
        selected = isSelected,
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
        label = {
            if (isRecommended) {
                Text("$label ★")
            } else {
                Text(label)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()
    val isSfxEnabled by viewModel.isSfxEnabled.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumTopAppBar("Settings", onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    Text("App Preferences", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Haptic Feedback", style = MaterialTheme.typography.bodyLarge)
                                    Text("Vibrate on interactions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isHapticEnabled,
                                    onCheckedChange = { viewModel.setHapticEnabled(it) }
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Sound Effects", style = MaterialTheme.typography.bodyLarge)
                                    Text("Play audio on button clicks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isSfxEnabled,
                                    onCheckedChange = { viewModel.setSfxEnabled(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}







