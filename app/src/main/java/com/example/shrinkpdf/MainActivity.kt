package com.example.shrinkpdf

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.example.shrinkpdf.R
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shrinkpdf.utils.AppLogger
import com.example.shrinkpdf.ui.MainViewModel
import com.example.shrinkpdf.logic.PdfAnalysis
import com.example.shrinkpdf.ui.OrganizeCategoryScreen
import com.example.shrinkpdf.ui.MergePdfScreen
import com.example.shrinkpdf.ui.SplitPdfScreen
import com.example.shrinkpdf.ui.DeletePagesScreen
import com.example.shrinkpdf.ui.ExtractImagesScreen
import com.example.shrinkpdf.ui.CheckCategoryScreen
import com.example.shrinkpdf.ui.InspectMetadataScreen
import com.example.shrinkpdf.ui.StripMetadataScreen
import com.example.shrinkpdf.ui.TextCleanerScreen
import com.example.shrinkpdf.ui.ImagesToPdfScreen
import com.example.shrinkpdf.ui.RotatePdfScreen
import com.example.shrinkpdf.ui.ExtractTextScreen
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
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.example.shrinkpdf.billing.AdManager

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

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        MobileAds.initialize(this) { }
        // Do not preload ads on launch while debugging the home UI.
        // On some emulator/WebView setups, ad preload can create a blank black surface over Compose.
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            var isDarkTheme by remember { mutableStateOf(false) }
            val useDark = isDarkTheme
            
            ShrinkPdfTheme(useDarkTheme = useDark) {
                MainApp(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
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
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
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

    val isPremium by viewModel.isPremium.collectAsState()
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
            

        when (currentScreen) {
            Screen.Home -> HomeScreen(
                windowWidthSizeClass = windowWidthSizeClass,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onNavigate = { screen -> currentScreen = screen },
                viewModel = viewModel
            )
            Screen.CompressCategory -> CompressCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.CompressPdf -> CompressPdfScreen(viewModel, 0) { currentScreen = Screen.CompressCategory }
            Screen.BatchCompressPdf -> CompressPdfScreen(viewModel, 1) { currentScreen = Screen.CompressCategory }
            Screen.TextToPdf -> TextToPdfScreen(viewModel) { currentScreen = Screen.CreateCategory }
            Screen.ImagesToPdf -> ImagesToPdfScreen(viewModel) { currentScreen = Screen.CreateCategory }
            Screen.TextConverter -> TextConverterScreen(textConverterViewModel) { currentScreen = Screen.CreateCategory }
            Screen.Settings -> SettingsScreen(viewModel) { currentScreen = Screen.Home }
            Screen.CreateCategory -> CreateCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.OrganizeCategory -> OrganizeCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.MergePdf -> MergePdfScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.SplitPdf -> SplitPdfScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.DeletePages -> DeletePagesScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.ExtractImages -> ExtractImagesScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.CheckCategory -> CheckCategoryScreen(onNavigate = { screen -> currentScreen = screen }, onBack = { currentScreen = Screen.Home })
            Screen.InspectMetadata -> InspectMetadataScreen(viewModel) { currentScreen = Screen.CheckCategory }
            Screen.StripMetadata -> StripMetadataScreen(viewModel) { currentScreen = Screen.CheckCategory }
            Screen.TextCleaner -> TextCleanerScreen { currentScreen = Screen.CheckCategory }
            Screen.RotatePdf -> RotatePdfScreen(viewModel) { currentScreen = Screen.OrganizeCategory }
            Screen.ExtractText -> ExtractTextScreen(viewModel) { currentScreen = Screen.CheckCategory }
            Screen.Premium -> PremiumUpgradeScreen(viewModel) { currentScreen = Screen.Home }
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
                        Text(stringResource(R.string.compressing_pdf), color = Color.White)
                    }
                }
            }
        }
        // Dynamic Result Dialog
        when (val state = uiState) {
            is MainViewModel.UiState.Success -> {
                val isPremium by viewModel.isPremium.collectAsState()
                val activity = context as? android.app.Activity
                AlertDialog(
                    onDismissRequest = {
                        if (activity != null) {
                            AdManager.showInterstitialIfReady(activity, isPremium) { viewModel.resetState() }
                        } else {
                            viewModel.resetState()
                        }
                    },
                    title = { Text(state.title) },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = {
                            if (activity != null) {
                                AdManager.showInterstitialIfReady(activity, isPremium) { viewModel.resetState() }
                            } else {
                                viewModel.resetState()
                            }
                        }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        if (state.outputUris.isNotEmpty()) {
                            TextButton(onClick = { 
                                com.example.shrinkpdf.logic.ShareUtil.shareFiles(context, state.outputUris, "Share Output")
                            }) {
                                Text(stringResource(R.string.share))
                            }
                        }
                    }
                )
            }
            is MainViewModel.UiState.Warning -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = { Text(state.title, color = MaterialTheme.colorScheme.error) },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text(stringResource(R.string.ok))
                        }
                    },
                    dismissButton = {
                        if (state.outputUris.isNotEmpty()) {
                            TextButton(onClick = { 
                                com.example.shrinkpdf.logic.ShareUtil.shareFiles(context, state.outputUris, "Share Output")
                            }) {
                                Text(stringResource(R.string.share))
                            }
                        }
                    }
                )
            }
            is MainViewModel.UiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    title = { Text(stringResource(R.string.error)) },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.resetState() }) {
                            Text(stringResource(R.string.ok))
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
            BannerAd(isPremium = isPremium)
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
    object DeletePages : Screen()
    object ExtractImages : Screen()
    object CheckCategory : Screen()
    object InspectMetadata : Screen()
    object StripMetadata : Screen()
    object TextCleaner : Screen()
    object ImagesToPdf : Screen()
    object RotatePdf : Screen()
    object ExtractText : Screen()
    object Premium : Screen()
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
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, baseColor.copy(alpha = 0.6f), Color.White, baseColor.copy(alpha = 0.6f), baseColor),
        start = Offset(shimmerOffset * 800f, 0f),
        end = Offset(shimmerOffset * 800f + 600f, 0f)
    )

    val textShadow = if (isDarkTheme) {
        androidx.compose.ui.graphics.Shadow(
            color = c1.copy(alpha = 0.55f),
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
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigate: (Screen) -> Unit,
    viewModel: MainViewModel
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

        val isPremium by viewModel.isPremium.collectAsState()

        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        val isWideScreen = windowWidthSizeClass != WindowWidthSizeClass.Compact

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (!isPremium) {
                IconButton(onClick = { onNavigate(Screen.Premium) }) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = "Go Premium", tint = Color(0xFFFFD700))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
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

        Box(modifier = Modifier.weight(1f)) {
            if (isWideScreen) {
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
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = CircleShape)
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
                        text = stringResource(R.string.app_name) + " Tools",
                        style = MaterialTheme.typography.headlineMedium,
                        baseColor = MaterialTheme.colorScheme.primary,
                        isDarkTheme = isDarkTheme
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
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
                        CategoryCard(stringResource(R.string.cat_compress), stringResource(R.string.cat_compress_desc), Icons.Rounded.Compress, { onNavigate(Screen.CompressCategory) }, Modifier.weight(1f).fillMaxHeight())
                        CategoryCard(stringResource(R.string.cat_create), stringResource(R.string.cat_create_desc), Icons.Rounded.AddCircleOutline, { onNavigate(Screen.CreateCategory) }, Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryCard(stringResource(R.string.cat_organize), stringResource(R.string.cat_organize_desc), Icons.Rounded.FolderOpen, { onNavigate(Screen.OrganizeCategory) }, Modifier.weight(1f).fillMaxHeight())
                        CategoryCard(stringResource(R.string.cat_check), stringResource(R.string.cat_check_desc), Icons.Rounded.FactCheck, { onNavigate(Screen.CheckCategory) }, Modifier.weight(1f).fillMaxHeight())
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
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), shape = CircleShape)
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
                  Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(stringResource(R.string.cat_compress), stringResource(R.string.cat_compress_desc), Icons.Rounded.Compress, { onNavigate(Screen.CompressCategory) }, Modifier.weight(1f).fillMaxHeight())
                    CategoryCard(stringResource(R.string.cat_create), stringResource(R.string.cat_create_desc), Icons.Rounded.AddCircleOutline, { onNavigate(Screen.CreateCategory) }, Modifier.weight(1f).fillMaxHeight())
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard(stringResource(R.string.cat_organize), stringResource(R.string.cat_organize_desc), Icons.Rounded.FolderOpen, { onNavigate(Screen.OrganizeCategory) }, Modifier.weight(1f).fillMaxHeight())
                    CategoryCard(stringResource(R.string.cat_check), stringResource(R.string.cat_check_desc), Icons.Rounded.FactCheck, { onNavigate(Screen.CheckCategory) }, Modifier.weight(1f).fillMaxHeight())
                }
                    Spacer(modifier = Modifier.height(16.dp))
                    RecentFilesSection(viewModel)
                }
            }
        }
        } // End of Box weight(1f)
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
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var scannedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val saveScannedPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri != null && scannedPdfUri != null) {
            try {
                val uri = scannedPdfUri ?: return@rememberLauncherForActivityResult
                context.contentResolver.openInputStream(uri)?.use { input ->
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "Scanned PDF exported!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                AppLogger.e("Exception caught in MainActivity", e)
                Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val scannerLauncherReal = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanningResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pdf?.uri?.let { pdfUri ->
                scannedPdfUri = pdfUri
                coroutineScope.launch {
                    try {
                        val scansDir = java.io.File(context.filesDir, "scans")
                        if (!scansDir.exists()) scansDir.mkdirs()
                        
                        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                        val fileName = "Scan_$timeStamp.pdf"
                        val destFile = java.io.File(scansDir, fileName)
                        
                        context.contentResolver.openInputStream(pdfUri)?.use { input ->
                            java.io.FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        val internalUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            destFile
                        )
                        
                        val historyRepo = com.example.shrinkpdf.logic.HistoryRepository(context)
                        historyRepo.addHistoryItem(internalUri, fileName, "scanned")
                        
                        // Show snackbar with Export option
                        val snackbarResult = snackbarHostState.showSnackbar(
                            message = "Scan saved to Repository",
                            actionLabel = "Export",
                            duration = SnackbarDuration.Short
                        )
                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                            saveScannedPdfLauncher.launch(com.example.shrinkpdf.logic.FileUtil.generateSuggestedName(null, "scanned_document"))
                        }
                    } catch (e: Exception) {
                        AppLogger.e("Exception caught in MainActivity", e)
                        snackbarHostState.showSnackbar("Failed to process scan")
                    }
                }
            }
        }
    }

    fun launchScanner() {
        if (activity == null) return
        val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setResultFormats(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        val scanner = com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
        scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
            scannerLauncherReal.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
        }.addOnFailureListener {
            Toast.makeText(context, "Scanner not available.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent, 
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent), navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ToolCard(
                    title = "Scan to PDF",
                    subtitle = "Use the native document scanner to create a PDF",
                    icon = Icons.Rounded.DocumentScanner,
                    onClick = { launchScanner() }
                )
            }
            item {
                ToolCard(
                    title = "Images to PDF",
                    subtitle = "Convert JPG, PNG, and other images to a single PDF",
                    icon = Icons.Rounded.PictureAsPdf,
                    onClick = { onNavigate(Screen.ImagesToPdf) }
                )
            }
            item {
                ToolCard(
                    title = "Text to PDF",
                    subtitle = "Convert your notes or .txt files to PDF",
                    icon = Icons.Rounded.Description,
                    onClick = { onNavigate(Screen.TextToPdf) }
                )
            }
            item {
                ToolCard(
                    title = "Text Format Converter",
                    subtitle = "Convert between TXT, MD, CSV, JSON, and more",
                    icon = Icons.Rounded.SyncAlt,
                    onClick = { onNavigate(Screen.TextConverter) }
                )
            }
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
                title = { Text(stringResource(R.string.compress)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent, scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent), navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ToolCard(
                    title = "Compress PDF",
                    subtitle = "Make PDFs smaller, safely.",
                    icon = Icons.Rounded.Compress,
                    onClick = { onNavigate(Screen.CompressPdf) }
                )
            }
            item {
                ToolCard(
                    title = "Batch Compression",
                    subtitle = "Select a folder and compress all PDFs inside",
                    icon = Icons.Rounded.LibraryBooks,
                    onClick = { onNavigate(Screen.BatchCompressPdf) } // Routes to same tool where tab exists
                )
            }
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
                Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.headlineMedium)
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
        if (uri != null) {
            val src = sourceUri ?: return@rememberLauncherForActivityResult
            viewModel.compressPdf(context, src, uri)
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
                    text = { Text(stringResource(R.string.compress_single)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { playFeedback(view, isHaptic, isSfx); coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(R.string.compress_batch)) }
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
                                    onSaveSingle = { savePdfLauncher.launch(com.example.shrinkpdf.logic.FileUtil.generateSuggestedName(sourceUri, "compressed")) },
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
                                    savePdfLauncher.launch(com.example.shrinkpdf.logic.FileUtil.generateSuggestedName(sourceUri, "compressed")) 
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
            title = { Text(stringResource(R.string.warning_quality_title)) },
            text = { Text(stringResource(R.string.warning_quality_desc)) },
            confirmButton = {
                TextButton(onClick = { showOfficialDocWarning = false }) {
                    Text(stringResource(R.string.warning_understand))
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
                        if (selectedFiles.isEmpty()) "Select PDF Files" else "Add More Files (${selectedFiles.size} selected)"
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
            val targetMb by viewModel.targetMb.collectAsState()
            var isListExpanded by remember { mutableStateOf(selectedFiles.size < 2) }
            
            // Calculate totals
            val totalOriginalSize = selectedFiles.sumOf { it.size }
            val totalEstimatedSize = if (targetMb != null) {
                val tm = targetMb
                if (tm != null) {
                    (tm * 1024 * 1024).toLong() * selectedFiles.size
                } else {
                    0L
                }
            } else {
                selectedFiles.sumOf { file ->
                    if (file.size > 0) {
                        viewModel.estimateCompressedSize(
                            originalSize = file.size,
                            quality = currentQuality,
                            useLossless = useLossless,
                            useGrayscale = useGrayscale,
                            scenario = null
                        )
                    } else 0L
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected Batch Files:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { viewModel.clearSelectedFiles() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(stringResource(R.string.clear_all))
                        }
                    }
                    
                    if (selectedFiles.size >= 2) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Summary (${selectedFiles.size} files)", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = viewModel.formatSize(totalOriginalSize),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(stringResource(R.string.empty_str), style = MaterialTheme.typography.bodySmall)
                                    val estimatedText = if (targetMb != null) {
                                    val tm = targetMb
                                    if (tm != null) {
                                        "Target: ${tm * selectedFiles.size} MB"
                                    } else {
                                        "Target: Unknown"
                                    }
                                    } else {
                                        "~${viewModel.formatSize(totalEstimatedSize)}"
                                    }
                                    Text(
                                        text = estimatedText,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Button(
                            onClick = { isListExpanded = !isListExpanded },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                if (isListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isListExpanded) "Hide Individual Files" else "View Individual Files")
                        }
                    }
                    
                    AnimatedVisibility(visible = isListExpanded || selectedFiles.size < 2) {
                        Column {
                            selectedFiles.forEach { file ->
                                val estCompressedSize = if (file.size > 0) {
                                    viewModel.estimateCompressedSize(
                                        originalSize = file.size,
                                        quality = currentQuality,
                                        useLossless = useLossless,
                                        useGrayscale = useGrayscale,
                                        scenario = null
                                    )
                                } else 0L
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.PictureAsPdf, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (file.isAnalyzing) "Analyzing..." else viewModel.formatSize(file.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!file.isAnalyzing) {
                                                Text(
                                                    text = "  →  ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                val estimatedText = if (targetMb != null) {
                                                    "Target: $targetMb MB"
                                                } else {
                                                    "~${viewModel.formatSize(estCompressedSize)}"
                                                }
                                                Text(
                                                    text = estimatedText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.removeSelectedFile(file.uri) }, 
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close, 
                                            contentDescription = "Remove file", 
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
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
                Text(stringResource(R.string.compress_options_empty), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val targetMb by viewModel.targetMb.collectAsState()
    var isTargetSizeEnabled by remember { mutableStateOf(targetMb != null) }
    var selectedTargetPreset by remember { mutableStateOf<String?>(if (targetMb != null) "Custom" else null) }
    var customTargetValue by remember { mutableStateOf(targetMb?.toString() ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.compress_target_size), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isTargetSizeEnabled,
                    onCheckedChange = { 
                        isTargetSizeEnabled = it 
                        if (!it) {
                            viewModel.setTargetMb(null)
                        } else {
                            if (selectedTargetPreset != "Custom" && selectedTargetPreset != null) {
                                viewModel.setTargetMb(selectedTargetPreset?.removeSuffix(" MB")?.toFloatOrNull())
                            }
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            if (!isTargetSizeEnabled) {
                Text(stringResource(R.string.compress_preset), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompressionPresetButton("Smallest", 0.25f, currentQuality, recQuality == 0.25f) { viewModel.setQuality(0.25f) }
                    CompressionPresetButton("Balanced", 0.50f, currentQuality, recQuality == 0.50f) { viewModel.setQuality(0.50f) }
                    CompressionPresetButton("Best", 0.75f, currentQuality, recQuality == 0.75f) { viewModel.setQuality(0.75f) }
                }
            } else {
                Text(stringResource(R.string.select_target_size_mb), style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Presets row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetPresetChip("2 MB", selectedTargetPreset == "2 MB", Modifier.weight(1f)) { 
                        selectedTargetPreset = "2 MB"
                        viewModel.setTargetMb(2f)
                    }
                    TargetPresetChip("5 MB", selectedTargetPreset == "5 MB", Modifier.weight(1f)) { 
                        selectedTargetPreset = "5 MB"
                        viewModel.setTargetMb(5f)
                    }
                    TargetPresetChip("10 MB", selectedTargetPreset == "10 MB", Modifier.weight(1f)) { 
                        selectedTargetPreset = "10 MB"
                        viewModel.setTargetMb(10f)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Presets row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TargetPresetChip("20 MB", selectedTargetPreset == "20 MB", Modifier.weight(1f)) { 
                        selectedTargetPreset = "20 MB"
                        viewModel.setTargetMb(20f)
                    }
                    TargetPresetChip("Custom", selectedTargetPreset == "Custom", Modifier.weight(1f)) { 
                        selectedTargetPreset = "Custom"
                        viewModel.setTargetMb(customTargetValue.toFloatOrNull())
                    }
                }
                
                if (selectedTargetPreset == "Custom") {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = customTargetValue,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                customTargetValue = newValue
                                viewModel.setTargetMb(newValue.toFloatOrNull())
                            }
                        },
                        label = { Text(stringResource(R.string.exact_mb)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.compression_options), style = MaterialTheme.typography.titleMedium)
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
                        Text(stringResource(R.string.grayscale_conversion), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(stringResource(R.string.convert_color_images_to_black), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 28.dp))
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
                        Text(stringResource(R.string.lossless_zip_compression), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(stringResource(R.string.skip_jpeg_lossy_compression_no), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 28.dp))
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
                        Text(stringResource(R.string.remove_metadata), style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(stringResource(R.string.strip_author_creator_editor_ta), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 28.dp))
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
    Spacer(modifier = Modifier.height(32.dp))
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
                    label = { Text(stringResource(R.string.type_or_paste_text_here)) },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    placeholder = { Text(stringResource(R.string.enter_the_content_of_your_pdf)) }
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
                    Text(stringResource(R.string.import_txt))
                }

                Button(
                    onClick = { 
                        if (inputText.isBlank()) {
                            Toast.makeText(context, "Please enter some text.", Toast.LENGTH_SHORT).show()
                        } else {
                            savePdfLauncher.launch(com.example.shrinkpdf.logic.FileUtil.generateSuggestedName(null, "converted_text"))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.save_as_pdf))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetPresetChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = isSelected,
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        label = { Text(label) }
    )
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
    val isHistoryEnabled by viewModel.isHistoryEnabled.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history)) },
            text = { Text(stringResource(R.string.are_you_sure_you_want_to_clear)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

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
                    
                    Text(stringResource(R.string.app_preferences), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))

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
                                    Text(stringResource(R.string.haptic_feedback), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.vibrate_on_interactions), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    Text(stringResource(R.string.sound_effects), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.play_audio_on_button_clicks), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isSfxEnabled,
                                    onCheckedChange = { viewModel.setSfxEnabled(it) }
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                    Text(stringResource(R.string.settings_history), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.settings_history_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isHistoryEnabled,
                                    onCheckedChange = { viewModel.setHistoryEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            // Language Selector
                            var expanded by remember { mutableStateOf(false) }
                            val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag() ?: "en"
                            val languages = mapOf("en" to "English", "pt" to "Português", "es" to "Español", "fr" to "Français", "de" to "Deutsch", "in" to "Bahasa Indonesia", "ro" to "Română")
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { expanded = true },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
                                    Text(stringResource(R.string.settings_language_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box {
                                    Text(languages[currentLocale] ?: "English", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        languages.forEach { (tag, name) ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(tag))
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            TextButton(
                                onClick = { showClearHistoryDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.settings_clear_history), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}







@Composable
fun RecentFilesSection(viewModel: MainViewModel) {
    val history by viewModel.historyList.collectAsState()
    val context = LocalContext.current

    if (history.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    history.take(5).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val uri = Uri.parse(item.uriString)
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        AppLogger.e("Exception caught in MainActivity", e)
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(item.action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannerAd(isPremium: Boolean, modifier: Modifier = Modifier) {
    if (isPremium) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun PremiumUpgradeScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val isPremium by viewModel.isPremium.collectAsState()
    val price by viewModel.premiumPrice.collectAsState()
    val context = LocalContext.current as android.app.Activity

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PremiumTopAppBar(title = "Premium Upgrade", onBack = onBack)
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                imageVector = Icons.Rounded.WorkspacePremium,
                contentDescription = "Premium",
                modifier = Modifier.size(120.dp),
                tint = Color(0xFFFFD700)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = if (isPremium) "You are a Premium User!" else "Support PDFchemy",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isPremium) {
                Text(
                    text = "Thank you for supporting our mission of privacy-first, offline document tools. All ads have been permanently removed.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Unlock the ultimate offline PDF experience.\n\n• No Banner Ads\n• No Interstitial Ads\n• 100% Offline Privacy\n• One-time lifetime purchase",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.purchasePremium(context) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        text = if (price.isNotEmpty()) "Unlock Lifetime for $price" else "Loading price...",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
}
