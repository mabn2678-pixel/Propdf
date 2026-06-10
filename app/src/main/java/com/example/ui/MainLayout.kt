package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import android.print.PrintManager
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.os.ParcelFileDescriptor
import android.os.Bundle
import java.io.FileOutputStream
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.data.PdfFile
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Simple App Entry Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: PdfViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val themeSetting by viewModel.themeSetting.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val permissionGranted by viewModel.hasPermissionGranted.collectAsState()
    val selectedPdf by viewModel.selectedPdf.collectAsState()
    
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Helper: evaluate system dark theme setting vs manual overrides
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = remember(themeSetting, isSystemDark) {
        when (themeSetting) {
            "dark" -> true
            "light" -> false
            else -> isSystemDark
        }
    }

    // Helper: Determine RTL Direction dynamically based on in-app selected Language
    val layoutDirection = if (appLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

    // Unified Permission Verification & Auto-Onboarding Trigger
    fun checkAndSyncPermission() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        viewModel.setPermissionState(granted)
        if (!granted) {
            viewModel.setShowPermissionExplanatory(true)
        } else {
            viewModel.scanForPdfFiles(context)
        }
    }

    // Run permission check on every start
    LaunchedEffect(Unit) {
        checkAndSyncPermission()
    }

    MyApplicationTheme(darkTheme = useDarkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (selectedPdf != null) {
                    // Immersive Fullscreen PDF Reader View takes priority, hiding general Navigation Shell interfaces
                    PdfReaderScreen(
                        pdfFile = selectedPdf!!,
                        viewModel = viewModel,
                        useDarkTheme = useDarkTheme,
                        lang = appLanguage
                    )
                } else {
                    // Side Navigation Drawer
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(300.dp),
                                drawerContainerColor = MaterialTheme.colorScheme.surface
                            ) {
                                DrawerHeaderMenu(appLanguage)
                                DrawerItemRow(
                                    icon = androidx.compose.material.icons.Icons.Default.School,
                                    label = if (appLanguage == "ar") "المفردات والدراسة" else "Vocabulary & Study",
                                    selected = currentTab == 4,
                                    onClick = {
                                        viewModel.selectTab(4)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                DrawerItemRow(
                                    icon = Icons.Default.Folder,
                                    label = LocaleHelper.getString("folders_header", appLanguage),
                                    selected = currentTab == 0,
                                    onClick = {
                                        viewModel.selectTab(0)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                DrawerItemRow(
                                    icon = Icons.Default.Star,
                                    label = LocaleHelper.getString("tab_bookmarks", appLanguage),
                                    selected = currentTab == 1,
                                    onClick = {
                                        viewModel.selectTab(1)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                DrawerItemRow(
                                    icon = Icons.Default.Search,
                                    label = LocaleHelper.getString("tab_search", appLanguage),
                                    selected = currentTab == 2,
                                    onClick = {
                                        viewModel.selectTab(2)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                DrawerItemRow(
                                    icon = Icons.Default.Settings,
                                    label = LocaleHelper.getString("tab_settings", appLanguage),
                                    selected = currentTab == 3,
                                    onClick = {
                                        viewModel.selectTab(3)
                                        scope.launch { drawerState.close() }
                                    }
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "v1.0.0 Pro • WPS Engine",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    ) {
                        var showCreateDialog by remember { mutableStateOf(false) }

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = LocaleHelper.getString("app_name", appLanguage),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Color.White
                                        )
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(
                                                imageVector = Icons.Default.Menu,
                                                contentDescription = "Drawer Menu",
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { showCreateDialog = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Create Simulated Document",
                                                tint = Color.White
                                            )
                                        }
                                        IconButton(onClick = { viewModel.selectTab(2) }) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Quick Search",
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = WPSBlue
                                    ),
                                    modifier = Modifier.shadow(4.dp)
                                )
                            },
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 6.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentTab == 0,
                                        onClick = { viewModel.selectTab(0) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == 0) Icons.Filled.Description else Icons.Outlined.Description,
                                                contentDescription = "Recent"
                                            )
                                        },
                                        label = {
                                            Text(
                                                LocaleHelper.getString("tab_recent", appLanguage),
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.testTag("tab_recent")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 1,
                                        onClick = { viewModel.selectTab(1) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == 1) Icons.Filled.Star else Icons.Outlined.Star,
                                                contentDescription = "Bookmarks"
                                            )
                                        },
                                        label = {
                                            Text(
                                                LocaleHelper.getString("tab_bookmarks", appLanguage),
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.testTag("tab_bookmarks")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 2,
                                        onClick = { viewModel.selectTab(2) },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search"
                                            )
                                        },
                                        label = {
                                            Text(
                                                LocaleHelper.getString("tab_search", appLanguage),
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.testTag("tab_search")
                                    )
                                    NavigationBarItem(
                                        selected = currentTab == 3,
                                        onClick = { viewModel.selectTab(3) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                contentDescription = "Settings"
                                            )
                                        },
                                        label = {
                                            Text(
                                                LocaleHelper.getString("tab_settings", appLanguage),
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.testTag("tab_settings")
                                     )
                                     NavigationBarItem(
                                         selected = currentTab == 4,
                                         onClick = { viewModel.selectTab(4) },
                                         icon = {
                                             Icon(
                                                 imageVector = if (currentTab == 4) androidx.compose.material.icons.Icons.Filled.School else androidx.compose.material.icons.Icons.Outlined.School,
                                                 contentDescription = "Vocabulary"
                                             )
                                         },
                                         label = {
                                             Text(
                                                 if (appLanguage == "ar") "المفردات" else "Vocab",
                                                 fontSize = 11.sp
                                             )
                                         },
                                         modifier = Modifier.testTag("tab_vocab")
                                    )
                                }
                            },
                            floatingActionButton = {
                                FloatingActionButton(
                                    onClick = { showCreateDialog = true },
                                    containerColor = WPSBlue,
                                    contentColor = Color.White
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "New PDF")
                                }
                            }
                        ) { paddingValues ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues)
                            ) {
                                when (currentTab) {
                                    0 -> RecentFilesScreen(viewModel, appLanguage)
                                    1 -> BookmarksScreen(viewModel, appLanguage)
                                    2 -> SearchScreen(viewModel, appLanguage)
                                    3 -> SettingsScreen(
                                        viewModel = viewModel,
                                        lang = appLanguage,
                                        onCheckPerm = { checkAndSyncPermission() }
                                    )
                                    4 -> VocabularyManagerScreen(viewModel)
                                }

                                // Interactive Dialog to create custom mock PDFs inside custom folders
                                if (showCreateDialog) {
                                    CreatePdfDialog(
                                        lang = appLanguage,
                                        onDismiss = { showCreateDialog = false },
                                        onConfirm = { name, folder ->
                                            viewModel.addNewSamplePdf(name, folder)
                                            showCreateDialog = false
                                        }
                                    )
                                }

                                // Step-by-Step Files Access explanatory onboarding gate
                                val showExplain by viewModel.showPermissionExplanatoryDialog.collectAsState()
                                if (showExplain) {
                                    PermissionExplanatoryGate(
                                        viewModel = viewModel,
                                        lang = appLanguage,
                                        onCheckPermissionNow = { checkAndSyncPermission() }
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

// Side Drawer logo card
@Composable
fun DrawerHeaderMenu(lang: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(WPSBlue, WPSBlueDark)
                )
            )
            .padding(vertical = 40.dp, horizontal = 16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "WPS Logo",
                        tint = PDFRed,
                        modifier = Modifier.size(35.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = LocaleHelper.getString("nav_drawer_title", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Office Suite Engine v1.0",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerItemRow(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) WPSBlue.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) WPSBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) WPSBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ------------------- SCREEN 1: CLIENT FILE LIST (RECENT) -------------------
fun formatLastOpened(timestamp: Long, lang: String): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60000 -> {
            if (lang == "ar") "الآن" else if (lang == "de") "Gerade eben" else "Just now"
        }
        diff < 3600000 -> {
            val minutes = diff / 60000
            if (lang == "ar") "منذ $minutes دقيقة" else if (lang == "de") "Vor $minutes Min." else "$minutes m ago"
        }
        diff < 86400000 -> {
            val hours = diff / 3600000
            if (lang == "ar") "منذ $hours ساعة" else if (lang == "de") "Vor $hours Std." else "$hours h ago"
        }
        else -> {
            val days = diff / 86400000
            if (lang == "ar") "منذ $days يوم" else if (lang == "de") "Vor $days Tagen" else "$days d ago"
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentFilesScreen(viewModel: PdfViewModel, lang: String) {
    val recents by viewModel.recentFiles.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var activeFolderFilter by remember { mutableStateOf<String?>(null) }
    val filteredFiles = remember(recents, activeFolderFilter) {
        if (activeFolderFilter == null) recents else recents.filter { it.folderName == activeFolderFilter }
    }

    var isGridView by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // WPS Classic Tools Row (PDF to DOC, Merger, compressor)
        WpsToolsDashboard(viewModel, lang)

        // Folder organization row (tap folder to filter)
        if (folders.isNotEmpty()) {
            Column {
                Text(
                    text = LocaleHelper.getString("folders_header", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All files tag
                    Surface(
                        onClick = { activeFolderFilter = null },
                        color = if (activeFolderFilter == null) WPSBlue else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (lang == "ar") "جميع الملفات" else if (lang == "de") "Alle Dateien" else "All Files",
                            fontSize = 13.sp,
                            color = if (activeFolderFilter == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    for (folder in folders) {
                        val isSelected = activeFolderFilter == folder
                        Surface(
                            onClick = { activeFolderFilter = folder },
                            color = if (isSelected) WPSBlue else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = if (isSelected) Color.White else WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = folder,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modern File Grid Header with orientation/layout switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocaleHelper.getString("recent_header", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeFolderFilter != null) {
                    TextButton(onClick = { activeFolderFilter = null }) {
                        Text(
                            text = if (lang == "ar") "عرض الكل" else if (lang == "de") "Alle zeigen" else "Show All",
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(
                    onClick = { isGridView = !isGridView },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "Switch Layout View Mode",
                        tint = WPSBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (filteredFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Empty PDFs",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = LocaleHelper.getString("no_recents", lang),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredFiles,
                        key = { it.id }
                    ) { pdf ->
                        PdfGridCard(
                            pdfFile = pdf,
                            lang = lang,
                            onClick = { viewModel.openPdf(pdf) },
                            onBookmarkToggle = { viewModel.toggleBookmark(pdf) },
                            onDelete = { viewModel.deleteFile(pdf) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredFiles,
                        key = { it.id }
                    ) { pdf ->
                        PdfFileCard(
                            pdfFile = pdf,
                            lang = lang,
                            onClick = { viewModel.openPdf(pdf) },
                            onBookmarkToggle = { viewModel.toggleBookmark(pdf) },
                            onDelete = { viewModel.deleteFile(pdf) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfGridCard(
    pdfFile: PdfFile,
    lang: String,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val sizeKb = pdfFile.sizeBytes / 1024L
    val sizeStr = if (sizeKb > 1024) "${"%.1f".format(sizeKb.toFloat() / 1024f)} MB" else "$sizeKb KB"
    val progressPercent = ((pdfFile.currentReadingPage.toFloat() / pdfFile.pageCount.toFloat()) * 100).toInt()
    val formattedTime = remember(pdfFile.lastOpened, lang) {
        formatLastOpened(pdfFile.lastOpened, lang)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Document Thumbnail with beautifully-styled custom WPS gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PDFRed,
                                PDFRed.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background visual accents to avoid solid flat colors (Design principle)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw stylish translucent curved wave shape or diagonal lines
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, size.height * 0.7f)
                        quadraticTo(
                            size.width * 0.5f, size.height * 0.5f,
                            size.width, size.height * 0.8f
                        )
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.15f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF Document",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PDF DOCUMENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                }

                // Decorative corner folder folding effect
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopStart)
                        .drawBehind {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color.White.copy(alpha = 0.25f)
                            )
                        }
                )

                // Bookmarked Star button on the Top-Right Corner
                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (pdfFile.isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Bookmark Toggle",
                        tint = if (pdfFile.isBookmarked) WarningAmber else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Page numbers badge in bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "ص ${pdfFile.pageCount}" else "${pdfFile.pageCount} pgs",
                        fontSize = 8.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = pdfFile.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Row with size labels & folder (if present)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // File size badge
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sizeStr,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Progress badge or folder
                    if (pdfFile.folderName != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    WarningAmber.copy(alpha = 0.12f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pdfFile.folderName,
                                fontSize = 9.sp,
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 50.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${progressPercent}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Last-opened timestamp & quick actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = "Last Opened",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete File",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(LocaleHelper.getString("delete_confirm", lang)) },
            text = { Text(LocaleHelper.getString("delete_desc", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PDFRed)
                ) {
                    Text(LocaleHelper.getString("delete", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(LocaleHelper.getString("cancel", lang))
                }
            }
        )
    }
}

// WPS-style quick tool panels
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WpsToolsDashboard(viewModel: PdfViewModel? = null, lang: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Fetch recent files from ViewModel if present, else fallback to standard mockups
    val recentListState = if (viewModel != null) {
        viewModel.recentFiles.collectAsState()
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val filesList = recentListState.value.ifEmpty {
        listOf(
            PdfFile(id = 101, filePath = "secured_report.pdf", title = "Financial Locked Secured Report 2026.pdf", author = "WPS Lead", sizeBytes = 1800000, pageCount = 14, folderName = "Sealed Letters"),
            PdfFile(id = 102, filePath = "invoice_scan.pdf", title = "Handwritten Tax Invoice.pdf", author = "System Scanner", sizeBytes = 2200000, pageCount = 3, folderName = "Invoices"),
            PdfFile(id = 103, filePath = "compose_guide.pdf", title = "Jetpack Compose Reference Docs.pdf", author = "Google UI Team", sizeBytes = 3500000, pageCount = 42, folderName = "Manuals")
        )
    }

    var activeTool by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (lang == "ar") "أدوات ميزات المكتبة الذكية (WPS Premium Toolkit)" else if (lang == "de") "Professionelle PDF-Werkzeuge" else "Ultimate PDF Specialist Tools",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grid containing the 8 robust tools
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val tools = listOf(
                    Triple("decrypt", if (lang == "ar") "فك تشفير PDF" else "Decrypt PDF", Icons.Default.LockOpen),
                    Triple("merge", if (lang == "ar") "دمج ملفات" else "Merge PDFs", Icons.Default.Merge),
                    Triple("split", if (lang == "ar") "تقسيم النطاق" else "Split PDF", Icons.Default.CallSplit),
                    Triple("ocr", if (lang == "ar") "التعرف OCR" else "OCR Text Scan", Icons.Default.CenterFocusWeak),
                    Triple("pdf2img", if (lang == "ar") "تحويل لصور" else "PDF to Images", Icons.Default.Image),
                    Triple("img2pdf", if (lang == "ar") "صور إلى PDF" else "Images to PDF", Icons.Default.PictureAsPdf),
                    Triple("pdf2txt", if (lang == "ar") "تحويل لنص" else "PDF to Word/TXT", Icons.Default.TextSnippet),
                    Triple("print", if (lang == "ar") "طباعة مباشرة" else "Print PDF", Icons.Default.Print)
                )

                for ((id, title, icon) in tools) {
                    val color = when (id) {
                        "decrypt" -> PDFRed
                        "merge" -> WarningAmber
                        "split" -> SuccessGreen
                        "ocr" -> WPSBlue
                        "pdf2img" -> Color(0xFF9C27B0)
                        "img2pdf" -> Color(0xFF00BCD4)
                        "pdf2txt" -> Color(0xFFE91E63)
                        else -> WPSBlueDark
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clickable { activeTool = id }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = color,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    // Interactive Dialogs for each Tool
    when (activeTool) {
        "decrypt" -> {
            var selectedFile by remember { mutableStateOf(filesList.firstOrNull()) }
            var passwordEntered by remember { mutableStateOf("") }
            var isDecrypted by remember { mutableStateOf(false) }
            var errorMsg by remember { mutableStateOf("") }
            var decryptingState by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "فك تشفير ملف PDF محمي" else "Decrypt Protected PDF") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (lang == "ar") "اختر المستند المشفر المراد إلغائه وحذف الحماية:" else "Select the encrypted document to decrypt:")
                        
                        // Select Dropdown / Horizontal list of files
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filesList) { file ->
                                val isSelected = file.id == selectedFile?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFile = file; isDecrypted = false; errorMsg = "" },
                                    label = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) }
                                )
                            }
                        }

                        if (selectedFile != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = PDFRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(selectedFile!!.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(if (lang == "ar") "مستوى الأمان: تشفير AES-256 بت" else "Security: AES-256 Bit Encryption", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                            
                            if (isDecrypted) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = SuccessGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (lang == "ar") "✓ تم فك تشفير المستند بنجاح! تم حفظ نسخة غير محمية." else "✓ Decrypted successfully! Unprotected copy saved.", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = passwordEntered,
                                    onValueChange = { passwordEntered = it; errorMsg = "" },
                                    label = { Text(if (lang == "ar") "كلمة مرور المستند (تجربة: 1234)" else "Document Password (Try: 1234)") },
                                    singleLine = true,
                                    isError = errorMsg.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (errorMsg.isNotEmpty()) {
                                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isDecrypted) {
                            Button(
                                onClick = {
                                    if (selectedFile == null) return@Button
                                    if (passwordEntered == "1234") {
                                        decryptingState = true
                                        scope.launch {
                                            delay(1500)
                                            decryptingState = false
                                            isDecrypted = true
                                            // Write decrypted item to room db
                                            viewModel?.addNewSamplePdf(
                                                title = selectedFile!!.title.replace(".pdf", " (Unencrypted).pdf"),
                                                folderName = selectedFile!!.folderName ?: "Unlocked PDFs"
                                            )
                                        }
                                    } else {
                                        errorMsg = if (lang == "ar") "كلمة المرور غير صحيحة! جرب '1234'" else "Invalid password entered! Try '1234'"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PDFRed),
                                enabled = !decryptingState
                            ) {
                                if (decryptingState) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text(if (lang == "ar") "فك التشفير" else "Decrypt Now")
                                }
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "merge" -> {
            val checkedIds = remember { mutableStateListOf<Int>() }
            var customMergeTitle by remember { mutableStateOf("") }
            var mergingInProgress by remember { mutableStateOf(false) }
            var mergedSuccess by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "دمج ملفات PDF متعددة" else "Merge PDF Documents") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (lang == "ar") "تحديد مستندين أو أكثر لتجميعهم في مستند واحد بالتوالي:" else "Select 2 or more files to combine into a unified document:")
                        
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filesList) { file ->
                                val isChecked = checkedIds.contains(file.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isChecked) checkedIds.remove(file.id) else checkedIds.add(file.id)
                                        }
                                        .background(if (isChecked) WPSBlue.copy(alpha = 0.08f) else Color.Transparent, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            if (isChecked) checkedIds.remove(file.id) else checkedIds.add(file.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(file.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${file.pageCount} ${if (lang == "ar") "صفحات" else "pages"}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        if (checkedIds.size >= 2) {
                            OutlinedTextField(
                                value = customMergeTitle,
                                onValueChange = { customMergeTitle = it },
                                label = { Text(if (lang == "ar") "اسم الملف المدمج الناتج" else "Resulting Merged Name") },
                                placeholder = { Text("Merged_Document") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(if (lang == "ar") "يرجى تحديد مستندين على الأقل للبدء." else "Please select at least 2 documents.", fontSize = 11.sp, color = PDFRed, fontWeight = FontWeight.Bold)
                        }

                        if (mergedSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (lang == "ar") "✓ تم دمج الملفات وحفظ الكتاب بنجاح!" else "✓ Files merged & saved successfully!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (checkedIds.size >= 2 && !mergedSuccess) {
                            Button(
                                onClick = {
                                    mergingInProgress = true
                                    scope.launch {
                                        delay(1800)
                                        mergingInProgress = false
                                        mergedSuccess = true
                                        val sumPages = filesList.filter { checkedIds.contains(it.id) }.sumOf { it.pageCount }
                                        val outputTitle = if (customMergeTitle.isNotBlank()) customMergeTitle else "Combined_PdfBook"
                                        viewModel?.addNewSamplePdf(
                                            title = "$outputTitle.pdf",
                                            folderName = "Merged Suite"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                enabled = !mergingInProgress
                            ) {
                                if (mergingInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text(if (lang == "ar") "دمج المستندات" else "Merge Now")
                                }
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "split" -> {
            var selectedFile by remember { mutableStateOf(filesList.firstOrNull()) }
            var startPage by remember { mutableStateOf("1") }
            var endPage by remember { mutableStateOf("3") }
            var isSplittingState by remember { mutableStateOf(false) }
            var splitSuccess by remember { mutableStateOf(false) }
            var errorText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "تقسيم ملف PDF مخصص" else "Split PDF Document") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (lang == "ar") "اختر الملف وحدد نطاق الصفحات المراد استخراجه:" else "Select file & input page ranges to slice:")
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filesList) { file ->
                                val selected = file.id == selectedFile?.id
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedFile = file; splitSuccess = false; errorText = "" },
                                    label = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) }
                                )
                            }
                        }

                        if (selectedFile != null) {
                            Text(if (lang == "ar") "إجمالي صفحات الملف: ${selectedFile!!.pageCount}" else "Total file pages: ${selectedFile!!.pageCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WPSBlue)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = startPage,
                                    onValueChange = { startPage = it; errorText = "" },
                                    label = { Text(if (lang == "ar") "من صفحة" else "From Page") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endPage,
                                    onValueChange = { endPage = it; errorText = "" },
                                    label = { Text(if (lang == "ar") "إلى صفحة" else "To Page") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (errorText.isNotEmpty()) {
                                Text(errorText, color = PDFRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (splitSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (lang == "ar") "✓ تم الاقتصاص وتوليد الجزء بنجاح كملف جديد!" else "✓ PDF split into new sliced copy successfully!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!splitSuccess && selectedFile != null) {
                            Button(
                                onClick = {
                                    val start = startPage.toIntOrNull() ?: 1
                                    val end = endPage.toIntOrNull() ?: 1
                                    if (start < 1 || end > selectedFile!!.pageCount || start > end) {
                                        errorText = if (lang == "ar") "نطاق صفحات غير صالح!" else "Invalid page range for this file!"
                                    } else {
                                        isSplittingState = true
                                        scope.launch {
                                            delay(1500)
                                            isSplittingState = false
                                            splitSuccess = true
                                            viewModel?.addNewSamplePdf(
                                                title = selectedFile!!.title.replace(".pdf", " (Clipped_Pages_${start}_${end}).pdf"),
                                                folderName = "Clipped Outputs"
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                enabled = !isSplittingState
                            ) {
                                if (isSplittingState) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text(if (lang == "ar") "تقسيم الآن" else "Split Pages")
                                }
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "ocr" -> {
            var selectedMockIndex by remember { mutableStateOf(0) }
            val mockScans = listOf(
                Pair("Commercial Taxes Invoice.jpg", "INVOICE #94819\nTax Date: 2026-05-12\nSeller: WPS Global Hub Inc.\nTotal Balance Due: $412.50 USD\nNotes: Thank you for your business!"),
                Pair("Arabic handwritten statement.jpg", "بيان الميزانية السنوية لمبيعات ٢٠٢٦\nالشركة: وكالة الحلول المكتبية المتكاملة\nإجمالي الإيرادات: ٩٥٠ ألف ريال سعودي\nالتقرير سري وجاهز للطباعة والترحيل المباشر."),
                Pair("Legal Agreement Contract.png", "WPS GENERAL SYSTEM PARTNERSHIP AGREEMENT\nDate: June 10, 2026\nTerms of Use: This sandbox simulates legal OCR indexing.\nAuthorized: Core SDK Modules.")
            )

            var isScanning by remember { mutableStateOf(false) }
            var extractionProgress by remember { mutableStateOf(0f) }
            var extractedResultText by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "التعرف الضوئي OCR من الصور" else "Scanned OCR Reader") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (lang == "ar") "اختر صورة المستند الممسوحة لتشغيل مسح التعرف ومستخرج الكلمات الذكي للملف القابل للنسخ:" else "Select scanned page templates to run blue-laser OCR text extractor:")
                        
                        ScrollableTabRow(selectedTabIndex = selectedMockIndex) {
                            mockScans.forEachIndexed { index, pair ->
                                Tab(
                                    selected = selectedMockIndex == index,
                                    onClick = { selectedMockIndex = index; extractedResultText = "" },
                                    text = { Text(pair.first, fontSize = 11.sp) }
                                )
                            }
                        }

                        if (isScanning) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(progress = { extractionProgress }, color = WPSBlue, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${(extractionProgress * 100).toInt()}% " + (if (lang == "ar") "مسح الأسطر ضوئياً واستخراج الفقرات..." else "Sweeping laser scanner, building glyph trees..."), fontSize = 11.sp, color = WPSBlue, fontWeight = FontWeight.Bold)
                            }
                        } else if (extractedResultText.isNotEmpty()) {
                            Text(if (lang == "ar") "الكلمات والنصوص المستخرجة بنجاح (قابلة للتعديل والنسخ):" else "Extracted text result (editable & copyable):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            OutlinedTextField(
                                value = extractedResultText,
                                onValueChange = { extractedResultText = it },
                                modifier = Modifier.fillMaxWidth().height(140.dp),
                                label = { Text("OCR Editor Panel") }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Camera Scan", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(mockScans[selectedMockIndex].first, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (extractedResultText.isEmpty() && !isScanning) {
                            Button(
                                onClick = {
                                    isScanning = true
                                    extractionProgress = 0f
                                    scope.launch {
                                        while (extractionProgress < 1.0f) {
                                            delay(150)
                                            extractionProgress += 0.1f
                                        }
                                        isScanning = false
                                        extractedResultText = mockScans[selectedMockIndex].second
                                        Toast.makeText(context, if (lang == "ar") "تم الانتهاء من استخراج النص!" else "Text extraction completed!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WPSBlue)
                            ) {
                                Text(if (lang == "ar") "بدأ التعرف على النص" else "Run Scanner OCR")
                            }
                        } else if (extractedResultText.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("WPS OCR", extractedResultText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (lang == "ar") "تم نسخ النص إلى الحافظة" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text(if (lang == "ar") "نسخ النص الناتج" else "Copy Text")
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "pdf2img" -> {
            var selectedFile by remember { mutableStateOf(filesList.firstOrNull()) }
            var imageFormat by remember { mutableStateOf("PNG") }
            var imageDpi by remember { mutableStateOf(150f) }
            var convertedSuccess by remember { mutableStateOf(false) }
            var convertingInProgress by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "تحويل PDF إلى صور مخصصة" else "Convert PDF to Image Slides") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (lang == "ar") "اختر المستند ونوع الجودة المرغوبة (DPI):" else "Select the doc & adjust target DPI rendering quality:")
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filesList) { file ->
                                val active = file.id == selectedFile?.id
                                FilterChip(
                                    selected = active,
                                    onClick = { selectedFile = file; convertedSuccess = false },
                                    label = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) }
                                )
                            }
                        }

                        if (selectedFile != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Format:", fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(selected = imageFormat == "PNG", onClick = { imageFormat = "PNG" }, label = { Text("PNG") })
                                    FilterChip(selected = imageFormat == "JPG", onClick = { imageFormat = "JPG" }, label = { Text("JPG") })
                                }
                            }

                            Text("Quality: ${imageDpi.toInt()} DPI (${if (imageDpi < 100) "Normal Draft" else if (imageDpi < 200) "Standard HD" else "Ultra Fine (Print Ready)"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Slider(
                                value = imageDpi,
                                onValueChange = { imageDpi = it },
                                valueRange = 72f..300f
                            )
                        }

                        if (convertedSuccess) {
                            Text(if (lang == "ar") "تم توليد لقطات الشرائح بنجاح! صور PNG جاهزة:" else "Successfully generated slide prints! PNG Files Ready:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(4) { idx ->
                                    Card(
                                        modifier = Modifier.size(70.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                                Text("Page ${idx + 1}.$imageFormat", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!convertedSuccess && selectedFile != null) {
                            Button(
                                onClick = {
                                    convertingInProgress = true
                                    scope.launch {
                                        delay(1600)
                                        convertingInProgress = false
                                        convertedSuccess = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                                enabled = !convertingInProgress
                            ) {
                                if (convertingInProgress) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text(if (lang == "ar") "تصدير الصور الآن" else "Export Images")
                                }
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "img2pdf" -> {
            var inputPdfName by remember { mutableStateOf("") }
            val selectedPhotos = remember { mutableStateListOf(0, 1, 2) } // indexes of mockup items checked
            var convertingImgState by remember { mutableStateOf(false) }
            var conversionImgSuccess by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "تحويل الصور إلى مستند PDF" else "Convert Images to PDF Book") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (lang == "ar") "منظم تجميع ملفات ومسح صور الكاميرا:" else "Compile selected photos into a standard document format:")
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(3) { idx ->
                                val active = selectedPhotos.contains(idx)
                                Card(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clickable { if (active) selectedPhotos.remove(idx) else selectedPhotos.add(idx) }
                                        .border(2.dp, if (active) WPSBlue else Color.Transparent, RoundedCornerShape(8.dp)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Photo, contentDescription = null, tint = if (active) WPSBlue else Color.Gray)
                                            Text("Photo_${idx + 1}.jpg", fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedPhotos.isNotEmpty()) {
                            OutlinedTextField(
                                value = inputPdfName,
                                onValueChange = { inputPdfName = it },
                                label = { Text(if (lang == "ar") "اسم ملف PDF الناتج" else "Resulting PDF Name") },
                                placeholder = { Text("Scanner_Output_1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(if (lang == "ar") "يرجى اختيار صورة واحدة على الأقل." else "Please choose at least 1 image.", color = PDFRed, fontSize = 11.sp)
                        }

                        if (conversionImgSuccess) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SuccessGreen.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (lang == "ar") "✓ تم تحويل الصور وبناء المستند الجديد بنجاح!" else "✓ Compiled photos to PDF successfully!", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedPhotos.isNotEmpty() && !conversionImgSuccess) {
                            Button(
                                onClick = {
                                    convertingImgState = true
                                    scope.launch {
                                        delay(1500)
                                        convertingImgState = false
                                        conversionImgSuccess = true
                                        val saveName = if (inputPdfName.isNotBlank()) inputPdfName else "ImageBook"
                                        viewModel?.addNewSamplePdf(
                                            title = "$saveName.pdf",
                                            folderName = "Compiled Photos"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                                enabled = !convertingImgState
                            ) {
                                if (convertingImgState) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text(if (lang == "ar") "تحويل وحفظ كـ PDF" else "Compile to PDF")
                                }
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "pdf2txt" -> {
            var selectedFile by remember { mutableStateOf(filesList.firstOrNull()) }
            var exportInTxt by remember { mutableStateOf(true) }
            var conversionSuccess by remember { mutableStateOf(false) }
            var contentExtracted by remember { mutableStateOf("") }
            var loadingConvertState by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "تحويل PDF إلى نص/Word" else "Convert PDF to Word/TXT") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (lang == "ar") "اختر المستند لتفريغ الحروف والنصوص:" else "Select PDF to extract structured text contents:")
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filesList) { file ->
                                val active = file.id == selectedFile?.id
                                FilterChip(
                                    selected = active,
                                    onClick = { selectedFile = file; conversionSuccess = false },
                                    label = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) }
                                )
                            }
                        }

                        if (selectedFile != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Format:", fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(selected = exportInTxt, onClick = { exportInTxt = true }, label = { Text("Text Only (.txt)") })
                                    FilterChip(selected = !exportInTxt, onClick = { exportInTxt = false }, label = { Text("Word document (.docx)") })
                                }
                            }
                        }

                        if (loadingConvertState) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(color = WPSBlue)
                                Text(if (lang == "ar") "جاري جرد فقرات المستند وفك الترميز..." else "De-tokenizing PDF font streams, extracting paragraphs...", fontSize = 11.sp, color = Color.Gray)
                            }
                        } else if (conversionSuccess) {
                            Text(if (lang == "ar") "محتوى المستند المستخرج بنجاح فريد:" else "Extracted text content from document (editable):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            OutlinedTextField(
                                value = contentExtracted,
                                onValueChange = { contentExtracted = it },
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                label = { Text("Text Editor") }
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!conversionSuccess && selectedFile != null && !loadingConvertState) {
                            Button(
                                onClick = {
                                    loadingConvertState = true
                                    scope.launch {
                                        delay(1500)
                                        loadingConvertState = false
                                        conversionSuccess = true
                                        contentExtracted = if (lang == "ar") {
                                            "هذا النص مستخرج بالكامل من ملف: ${selectedFile!!.title}\nتم إنشاؤه وتنسيقه بواسطة خوارزميات WPS Office Global PDF Sandbox."
                                        } else {
                                            "WPS OFFICE EXTRACTED TEXT - REPORT EXPORT\nFile: ${selectedFile!!.title}\nThis simulated sandbox reads directly from document node models in real-time."
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                            ) {
                                Text(if (lang == "ar") "بدأ التحويل اللفظي" else "Convert Now")
                            }
                        } else if (conversionSuccess) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Extracted TXT", contentExtracted)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, if (lang == "ar") "تم نسخ النص الناتج!" else "Extracted Text Copied!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text(if (lang == "ar") "نسخ النص" else "Copy Extracted")
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }

        "print" -> {
            var selectedFile by remember { mutableStateOf(filesList.firstOrNull()) }

            AlertDialog(
                onDismissRequest = { activeTool = null },
                title = { Text(if (lang == "ar") "طباعة المستند مباشرة (System Print)" else "Android Print Engine") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(if (lang == "ar") "اتصال مباشر مع PrintManager الخاص بـ Android. حدد المستند المراد إطلاقه للطباعة:" else "Select the document to send directly to the system printer services:")
                        
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filesList) { file ->
                                val active = file.id == selectedFile?.id
                                FilterChip(
                                    selected = active,
                                    onClick = { selectedFile = file },
                                    label = { Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp)) }
                                )
                            }
                        }

                        if (selectedFile != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🖨 " + selectedFile!!.title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("${selectedFile!!.pageCount} pages • Print Job ready.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (selectedFile != null) {
                            Button(
                                onClick = {
                                    activeTool = null
                                    triggerSystemPrint(context, selectedFile!!.title)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WPSBlueDark)
                            ) {
                                Text(if (lang == "ar") "إرسال للطابعة" else "Submit Print Job")
                            }
                        }
                        TextButton(onClick = { activeTool = null }) {
                            Text(if (lang == "ar") "إغلاق" else "Close")
                        }
                    }
                }
            )
        }
    }
}

// Global System Print Function leveraging live Android PrintManager API
fun triggerSystemPrint(context: Context, title: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
        Toast.makeText(context, "Printing is not supported on this Android system build.", Toast.LENGTH_SHORT).show()
        return
    }
    val jobName = "${title.replace(".pdf", "")}_WPS_Office_Print"
    
    try {
        printManager.print(jobName, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                try {
                    val output = FileOutputStream(destination?.fileDescriptor)
                    val dummyData = "WPS Office Android Print Service\nDocument Title: $title\nPages printed via Android PrintManager successfully."
                    output.write(dummyData.toByteArray())
                    output.close()
                    callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.toString())
                }
            }
        }, null)
    } catch (e: Exception) {
        Toast.makeText(context, "Print failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// Standard file design card
@Composable
fun PdfFileCard(
    pdfFile: PdfFile,
    lang: String,
    onClick: () -> Unit,
    onBookmarkToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.0.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual PDF image thumbnail sheet or red booklet placeholder
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(PDFRed, PDFRed.copy(alpha = 0.8f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "PDF Document",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "PDF",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = pdfFile.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pdfFile.folderName != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = pdfFile.folderName,
                                fontSize = 9.sp,
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val progressPercent = ((pdfFile.currentReadingPage.toFloat() / pdfFile.pageCount.toFloat()) * 100).toInt()
                    Text(
                        text = if (lang == "ar") "صفحة ${pdfFile.currentReadingPage}/${pdfFile.pageCount} (${progressPercent}%)"
                        else if (lang == "de") "Seite ${pdfFile.currentReadingPage}/${pdfFile.pageCount} (${progressPercent}%)"
                        else "Page ${pdfFile.currentReadingPage}/${pdfFile.pageCount} (${progressPercent}%)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // File metadata
                val sizeKb = pdfFile.sizeBytes / 1024L
                val sizeStr = if (sizeKb > 1024) "${"%.1f".format(sizeKb.toFloat() / 1024f)} MB" else "$sizeKb KB"
                Text(
                    text = "$sizeStr • ${pdfFile.author ?: "WPS Engine"}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // Quick interactions
            IconButton(onClick = onBookmarkToggle) {
                Icon(
                    imageVector = if (pdfFile.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarRate,
                    contentDescription = "Bookmark Toggle",
                    tint = if (pdfFile.isBookmarked) WarningAmber else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            IconButton(onClick = { showDeleteConfirmDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete File",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(LocaleHelper.getString("delete_confirm", lang)) },
            text = { Text(LocaleHelper.getString("delete_desc", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PDFRed)
                ) {
                    Text(LocaleHelper.getString("delete", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(LocaleHelper.getString("cancel", lang))
                }
            }
        )
    }
}

// ------------------- SCREEN 2: BOOKMARKS SCREEN -------------------
@Composable
fun BookmarksScreen(viewModel: PdfViewModel, lang: String) {
    val bookmarks by viewModel.bookmarkedFiles.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = LocaleHelper.getString("bookmarks_header", lang),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = "No bookmarks",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = LocaleHelper.getString("no_bookmarks", lang),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(bookmarks) { pdf ->
                    PdfFileCard(
                        pdfFile = pdf,
                        lang = lang,
                        onClick = { viewModel.openPdf(pdf) },
                        onBookmarkToggle = { viewModel.toggleBookmark(pdf) },
                        onDelete = { viewModel.deleteFile(pdf) }
                    )
                }
            }
        }
    }
}

// ------------------- SCREEN 3: SEARCH SCREEN -------------------
@Composable
fun SearchScreen(viewModel: PdfViewModel, lang: String) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input text field
        TextField(
            value = query,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text(LocaleHelper.getString("search_hint", lang)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Query Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FindInPage,
                        contentDescription = "No results",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = LocaleHelper.getString("search_empty", lang),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(searchResults) { pdf ->
                    PdfFileCard(
                        pdfFile = pdf,
                        lang = lang,
                        onClick = { viewModel.openPdf(pdf) },
                        onBookmarkToggle = { viewModel.toggleBookmark(pdf) },
                        onDelete = { viewModel.deleteFile(pdf) }
                    )
                }
            }
        }
    }
}

// ------------------- SCREEN 4: SETTINGS SCREEN -------------------
@Composable
fun SettingsScreen(
    viewModel: PdfViewModel,
    lang: String,
    onCheckPerm: () -> Unit
) {
    val themeSetting by viewModel.themeSetting.collectAsState()
    val continuousScroll by viewModel.continuousScroll.collectAsState()
    val screenBrightness by viewModel.screenBrightness.collectAsState()
    val filesPermOk by viewModel.hasPermissionGranted.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Language Picker Switcher (Supports in-app LTR <-> RTL instantaneous switching)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocaleHelper.getString("settings_language", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LanguageSelectorOption(
                            label = LocaleHelper.getString("lang_ar", lang),
                            isSelected = lang == "ar",
                            onClick = { viewModel.setAppLanguage("ar") },
                            modifier = Modifier.weight(1f)
                        )
                        LanguageSelectorOption(
                            label = LocaleHelper.getString("lang_en", lang),
                            isSelected = lang == "en",
                            onClick = { viewModel.setAppLanguage("en") },
                            modifier = Modifier.weight(1f)
                        )
                        LanguageSelectorOption(
                            label = LocaleHelper.getString("lang_de", lang),
                            isSelected = lang == "de",
                            onClick = { viewModel.setAppLanguage("de") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Night Mode & Visual Customizations
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LocaleHelper.getString("settings_general", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = LocaleHelper.getString("settings_theme", lang) + ":",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val optTextSystem = LocaleHelper.getString("settings_sys_default", lang)
                        ThemeSelectorChip("dark", "Dark (داكن)", themeSetting == "dark", onClick = { viewModel.setThemeSetting("dark") }, modifier = Modifier.weight(1f))
                        ThemeSelectorChip("light", "Light (فاتح)", themeSetting == "light", onClick = { viewModel.setThemeSetting("light") }, modifier = Modifier.weight(1f))
                        ThemeSelectorChip("system", optTextSystem, themeSetting == "system", onClick = { viewModel.setThemeSetting("system") }, modifier = Modifier.weight(1f))
                    }

                    // Simulated Brightness slider
                    Text(
                        text = "${LocaleHelper.getString("settings_brightness", lang)} (${screenBrightness.toInt()}%):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Slider(
                        value = screenBrightness,
                        onValueChange = { viewModel.setScreenBrightness(it) },
                        valueRange = 10f..100f,
                        colors = SliderDefaults.colors(thumbColor = WPSBlue, activeTrackColor = WPSBlue)
                    )
                }
            }
        }

        // GRANULAR FILES PERMISSIONS REGION
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filesPermOk) SuccessGreen.copy(alpha = 0.08f) else WarningAmber.copy(alpha = 0.08f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (filesPermOk) SuccessGreen.copy(alpha = 0.3f) else WarningAmber.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LocaleHelper.getString("settings_perm_status", lang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (filesPermOk) LocaleHelper.getString("settings_perm_status_ok", lang)
                                else LocaleHelper.getString("settings_perm_status_no", lang),
                                fontSize = 13.sp,
                                color = if (filesPermOk) SuccessGreen else PDFRed,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Icon indicating granted vs denied
                        Icon(
                            imageVector = if (filesPermOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Permission Status",
                            tint = if (filesPermOk) SuccessGreen else WarningAmber,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    if (!filesPermOk) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.setShowPermissionExplanatory(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = WPSBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(LocaleHelper.getString("permissions_grant", lang))
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lang == "ar") "✓ التطبيق مفعل بالكامل وجاهز لمسح جميع ملفات المستندات في الخلفية."
                            else if (lang == "de") "✓ Die App ist voll funktionsfähig und scannt den lokalen Speicher."
                            else "✓ Fully authorized to discover PDF and Office documents.",
                            fontSize = 11.sp,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }

        // Help Feedback & App Info
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = LocaleHelper.getString("settings_about", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = LocaleHelper.getString("settings_about_desc", lang),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "WPS Office PDF Version", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text(text = "1.0.0 (Ultimate Build)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // GERMAN STUDY SETTINGS INTEGRATION CARD
        item {
            val studyModeEnabled by viewModel.studyModeEnabled.collectAsState()
            val highlightType by viewModel.highlightType.collectAsState()
            val pronunciationSpeed by viewModel.pronunciationSpeed.collectAsState()
            val preferredDict by viewModel.preferredWebDictionary.collectAsState()
            val showSpeakerIcon by viewModel.showSpeakerIconBesideWord.collectAsState()

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (lang == "ar") "إعدادات دراسة الألمانية (German Study Settings)" else "German Study & Interaction Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // 1. Toggle study overlay
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("تفعيل طبقة التفاعل (Study Overlay)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("يجعل كل كلمة ألمانية بالـ PDF قابلة للنقر والنطق الفوري", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = studyModeEnabled,
                            onCheckedChange = { viewModel.setStudyModeEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Playback Pronunciation speed selector
                    Text("سرعة نطق الكلمات (TTS Speed):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.5f to "بطيء (0.5x)", 1.0f to "طبيعي (1.0x)", 1.5f to "سريع (1.5x)").forEach { (value, label) ->
                            Button(
                                onClick = { viewModel.setPronunciationSpeed(value) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pronunciationSpeed == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = if (pronunciationSpeed == value) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Highlight Style Choose
                    Text("نوع التمييز البصري (Highlight Style):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("luminous" to "مضيء (Luminous)", "hidden" to "مخفي (Hidden)", "test" to "اختبار (Test)").forEach { (key, label) ->
                            Button(
                                onClick = { viewModel.setHighlightType(key) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (highlightType == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(label, fontSize = 10.sp, color = if (highlightType == key) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4. Default External Dictionary DB
                    Text("القاموس الخارجي الافتراضي:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("DWDS", "Arabdict", "Google Translate").forEach { dict ->
                            Button(
                                onClick = { viewModel.setPreferredWebDictionary(dict) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (preferredDict == dict) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(dict, fontSize = 10.sp, color = if (preferredDict == dict) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Toggle speaker icon beside the words
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("إيقونة مكبر الصوت بجانب المفردة", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("عرض إيقونة 🔊 لتسهيل النطق المباشر", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = showSpeakerIcon,
                            onCheckedChange = { viewModel.setShowSpeakerIcon(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectorOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) WPSBlue else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ThemeSelectorChip(
    id: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) WPSBlue else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ------------------- IMMERSIVE SANDBOX PDF READER SCREEN -------------------
@Composable
fun PdfReaderScreen(
    pdfFile: PdfFile,
    viewModel: PdfViewModel,
    useDarkTheme: Boolean,
    lang: String
) {
    val context = LocalContext.current
    val continuousScroll by viewModel.continuousScroll.collectAsState()
    val screenBrightness by viewModel.screenBrightness.collectAsState()

    var activeStudyWordItem by remember { mutableStateOf<com.example.data.VocabItem?>(null) }

    // Defaults to FALSE to start IMMEDIATELY in IMMERSIVE FULLSCREEN MODE as requested!
    var showOverlayControls by remember { mutableStateOf(false) }
    
    // Zoom and pan tracking states for full custom pinch-to-zoom (25% to 500%)
    var zoomScale by remember { mutableStateOf(1.0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Active annotations state trackers
    var activeAnnotationTool by remember { mutableStateOf<String?>(null) } // "ink", "shape", "text", "sticky", null
    var annotationColor by remember { mutableStateOf(Color.Red) }
    var activeShapeType by remember { mutableStateOf("square") } // "square", "circle", "arrow", "line"

    // Toggle overlay on tapping center of the document pages
    fun toggleControls() {
        showOverlayControls = !showOverlayControls
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            // Simulates actual screen brightness reduction using a dark translucent visual cover overlay!
            .drawBehind {
                val alpha = (100f - screenBrightness) / 100f * 0.5f
                drawRect(color = Color.Black.copy(alpha = alpha))
            }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (useDarkTheme) WPSDarkBackground else Color(0xFFE0E0E0))
                // Touch gestures for Pinch-to-Zoom (25% to 500%) and Double-tap (to 150%)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale >= 1.5f) {
                                zoomScale = 1.0f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                zoomScale = 1.50f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                        onTap = {
                            toggleControls()
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (zoomScale * zoom).coerceIn(0.25f, 5.0f)
                        zoomScale = newScale
                        if (zoomScale > 1.0f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        ) {
            // Apply zoom scaling & panning translations to the document container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoomScale,
                        scaleY = zoomScale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            ) {
                // Document Layout Pages
                if (continuousScroll) {
                    // Vertical Continuous Scroll Layout Mode
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pdfFile.pageCount) { pageIdx ->
                            PdfPageRenderer(
                                pdfFile = pdfFile,
                                pageNumber = pageIdx + 1,
                                scale = 1.0f, // Scale is managed by high-performance parent graphicsLayer
                                darkTheme = useDarkTheme,
                                lang = lang,
                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(4.dp)),
                                activeAnnotationTool = activeAnnotationTool,
                                annotationColor = annotationColor,
                                activeShapeType = activeShapeType,
                                viewModel = viewModel,
                                onWordSelected = { activeStudyWordItem = it }
                            )
                        }
                    }
                } else {
                    // Horizontal Swipe Booklet Mode
                    val pagerState = rememberPagerState(initialPage = pdfFile.currentReadingPage - 1) { pdfFile.pageCount }
                    
                    // Keep ViewModel reading progress synchronized dynamically
                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.updateReadingPage(pdfFile, pagerState.currentPage + 1)
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) { page ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            PdfPageRenderer(
                                pdfFile = pdfFile,
                                pageNumber = page + 1,
                                scale = 1.0f,
                                darkTheme = useDarkTheme,
                                lang = lang,
                                modifier = Modifier.shadow(4.dp, RoundedCornerShape(4.dp)),
                                activeAnnotationTool = activeAnnotationTool,
                                annotationColor = annotationColor,
                                activeShapeType = activeShapeType,
                                viewModel = viewModel,
                                onWordSelected = { activeStudyWordItem = it }
                            )
                        }
                    }
                }
            }

            // IMmersive Floating Reading UI Overlays
            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                // Upper booklet status bar toolbar
                Surface(
                    color = WPSBlue,
                    contentColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.closePdf() }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Close PDF", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pdfFile.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Text(
                                text = "${LocaleHelper.getString("pdf_viewer", lang)} • ${pdfFile.author ?: "WPS Engine"}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        // Annotate Toolbar Toggle option icon
                        IconButton(onClick = {
                            activeAnnotationTool = if (activeAnnotationTool == null) "ink" else null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Annotate PDF",
                                tint = if (activeAnnotationTool != null) WarningAmber else Color.White
                            )
                        }

                        IconButton(onClick = { viewModel.toggleBookmark(pdfFile) }) {
                            Icon(
                                imageVector = if (pdfFile.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarRate,
                                contentDescription = "Bookmark",
                                tint = if (pdfFile.isBookmarked) WarningAmber else Color.White
                            )
                        }

                        IconButton(onClick = {
                            val msg = "Sharing simulated link: ${pdfFile.title}"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Document", tint = Color.White)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Bottom booklet settings menu bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Current page display and swipe layout buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == "ar") "الصفحة ${pdfFile.currentReadingPage} / ${pdfFile.pageCount}"
                                else if (lang == "de") "Seite ${pdfFile.currentReadingPage} von ${pdfFile.pageCount}"
                                else "Page ${pdfFile.currentReadingPage} of ${pdfFile.pageCount}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Page Flip Toggle layout buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    onClick = { viewModel.setContinuousScroll(true) },
                                    color = if (continuousScroll) WPSBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Vertical Continuous",
                                        tint = if (continuousScroll) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Surface(
                                    onClick = { viewModel.setContinuousScroll(false) },
                                    color = if (!continuousScroll) WPSBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Horizontal Flip",
                                        tint = if (!continuousScroll) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }

                        // Zoom scaling triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(text = LocaleHelper.getString("pdf_zoom", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (zoomScale > 0.25f) zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.25f) }) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom out")
                            }
                            Text(text = "${(zoomScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { if (zoomScale < 5.0f) zoomScale = (zoomScale + 0.15f).coerceAtMost(5.0f) }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom in")
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Simulated eye protection mode trigger
                            Surface(
                                onClick = {
                                    val nextBrightness = if (screenBrightness < 60f) 85f else 40f
                                    viewModel.setScreenBrightness(nextBrightness)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (screenBrightness < 60f) SuccessGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RemoveRedEye,
                                        contentDescription = "Eye shield",
                                        tint = if (screenBrightness < 60f) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (lang == "ar") "حماية العين" else if (lang == "de") "Augenschutz" else "Eye Shield",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (screenBrightness < 60f) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING CONFIG BAR OVERLAY
            AnimatedVisibility(
                visible = activeAnnotationTool != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lang == "ar") "أدوات الرسم والتعليق" else "Annotation Toolkit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { activeAnnotationTool = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close annotation configuration", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Choice of Annotating Tool: Ink, Shape, Text notation, Sticky pin
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = { activeAnnotationTool = "ink" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (activeAnnotationTool == "ink") WPSBlue else MaterialTheme.colorScheme.surfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Gesture, contentDescription = "Draw", tint = if (activeAnnotationTool == "ink") Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (lang == "ar") "قلم" else "Ink", fontSize = 10.sp, color = if (activeAnnotationTool == "ink") Color.White else MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = { activeAnnotationTool = "shape" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (activeAnnotationTool == "shape") WPSBlue else MaterialTheme.colorScheme.surfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Category, contentDescription = "Shape tool", tint = if (activeAnnotationTool == "shape") Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (lang == "ar") "شكل" else "Shape", fontSize = 10.sp, color = if (activeAnnotationTool == "shape") Color.White else MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = { activeAnnotationTool = "text" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (activeAnnotationTool == "text") WPSBlue else MaterialTheme.colorScheme.surfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.TextFields, contentDescription = "Text tool", tint = if (activeAnnotationTool == "text") Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (lang == "ar") "نص" else "Text", fontSize = 10.sp, color = if (activeAnnotationTool == "text") Color.White else MaterialTheme.colorScheme.onSurface)
                                }

                                Button(
                                    onClick = { activeAnnotationTool = "sticky" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (activeAnnotationTool == "sticky") WPSBlue else MaterialTheme.colorScheme.surfaceVariant),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(28.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.PushPin, contentDescription = "Sticky notes", tint = if (activeAnnotationTool == "sticky") Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(if (lang == "ar") "لاصقة" else "Sticky", fontSize = 10.sp, color = if (activeAnnotationTool == "sticky") Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }

                            // Dynamic Color Chooser for Active Pen Drawing and shapes border outline
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.DarkGray).forEach { clr ->
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(clr)
                                            .border(if (annotationColor == clr) 1.5.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            .clickable { annotationColor = clr }
                                    )
                                }
                            }
                        }

                        // Shapes Choice Secondary Selectable Row (only visible if "shape" tool is active!)
                        if (activeAnnotationTool == "shape") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (lang == "ar") "نوع الشكل:" else "Shape:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                listOf(
                                    "square" to (if (lang == "ar") "مربع" else "Square"),
                                    "circle" to (if (lang == "ar") "دائرة" else "Circle"),
                                    "arrow" to (if (lang == "ar") "سهم" else "Arrow"),
                                    "line" to (if (lang == "ar") "خط" else "Line")
                                ).forEach { (shKey, shLabel) ->
                                    Button(
                                        onClick = { activeShapeType = shKey },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (activeShapeType == shKey) WarningAmber else MaterialTheme.colorScheme.surfaceVariant),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        modifier = Modifier.height(24.dp),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(shLabel, fontSize = 9.sp, color = if (activeShapeType == shKey) Color.Black else MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // -----------------------------------------------------
            // Slide-up Bottom Sheet Overlay for German study words
            // -----------------------------------------------------
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeStudyWordItem != null,
                    enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                ) {
                    activeStudyWordItem?.let { item ->
                        WordInteractionBottomSheet(
                            vocabItem = item,
                            viewModel = viewModel,
                            onDismiss = { activeStudyWordItem = null }
                        )
                    }
                }
            }
        }
    }
}

// Custom document sandbox drawing engine
@Composable
fun PdfPageRenderer(
    pdfFile: PdfFile,
    pageNumber: Int,
    scale: Float,
    darkTheme: Boolean,
    lang: String,
    modifier: Modifier = Modifier,
    activeAnnotationTool: String? = null,
    annotationColor: Color = Color.Red,
    activeShapeType: String = "square",
    viewModel: PdfViewModel,
    onWordSelected: (com.example.data.VocabItem) -> Unit
) {
    val baseWidth = 320.dp
    val baseHeight = 440.dp
    val dynamicWidth = baseWidth * scale
    val dynamicHeight = baseHeight * scale

    val pageCanvasBg = if (darkTheme) WPSDarkSurface else Color.White
    val textBaseColor = if (darkTheme) WPSDarkOnSurface else Color(0xFF263238)

    // Persistent page annotations within this active session
    val pageInkPaths = remember { mutableStateListOf<List<Offset>>() }
    val pageTextAnnotations = remember { mutableStateListOf<Triple<String, Offset, Color>>() } // text, offset, color
    val pageShapeAnnotations = remember { mutableStateListOf<Triple<String, Offset, Color>>() } // type, offset, color
    val pageStickyNotes = remember { mutableStateListOf<Pair<String, Offset>>() } // content, offset

    var activeInteractiveTapOffset by remember { mutableStateOf<Offset?>(null) }
    var showTextAnnotationInput by remember { mutableStateOf(false) }
    var showStickyNoteInput by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(dynamicWidth)
            .height(dynamicHeight)
            .background(pageCanvasBg)
            .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Document Header Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pdfFile.title.take(15) + "...",
                    fontSize = (9 * scale).sp,
                    fontWeight = FontWeight.Medium,
                    color = textBaseColor.copy(alpha = 0.4f)
                )
                Text(
                    text = "Page $pageNumber",
                    fontSize = (9 * scale).sp,
                    color = textBaseColor.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Document Paragraphs (Renders real selectable, interactable text paragraphs!)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Topic Label Header
                val headerText = if (pdfFile.title.contains("Guide")) "Section $pageNumber: Getting Started with WPS"
                else if (pdfFile.title.contains("تقرير") || lang == "ar") "القسم $pageNumber: المراجعة الشاملة للمستندات"
                else "Kapitel $pageNumber: Einführung"

                Text(
                    text = headerText,
                    fontWeight = FontWeight.Bold,
                    fontSize = (13 * scale).sp,
                    color = textBaseColor
                )

                // Simulated booklet separator line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(WPSBlue)
                )

                // Real Selectable Paragraph component satisfying long press text selection & handles
                val paraText = getPageText(pdfFile, pageNumber, lang)
                
                val studyModeEnabled by viewModel.studyModeEnabled.collectAsState()
                
                if (studyModeEnabled) {
                    GermanArabicInteractiveVocabParagraph(
                        text = paraText,
                        scale = scale,
                        textBaseColor = textBaseColor,
                        viewModel = viewModel,
                        onWordSelected = onWordSelected
                    )
                } else {
                    InteractiveParagraph(
                        text = paraText,
                        scale = scale,
                        textBaseColor = textBaseColor,
                        lang = lang
                    )
                }

                // If page is odd, render a beautiful infographic vector or statistics chart block
                if (pageNumber % 2 == 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((70 * scale).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(textBaseColor.copy(alpha = 0.05f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.InsertChart,
                                contentDescription = "Mock Chart",
                                tint = WPSBlue.copy(alpha = 0.7f),
                                modifier = Modifier.size((30 * scale).dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == "ar") "شكل $pageNumber.1: مؤشر نمو المستندات" else "Figure $pageNumber.1: Document Growth Index",
                                fontSize = (8 * scale).sp,
                                color = textBaseColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stylized signatures or footnotes
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width((60 * scale).dp)
                                .height((6 * scale).dp)
                                .background(textBaseColor.copy(alpha = 0.1f))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Box(
                            modifier = Modifier
                                .width((40 * scale).dp)
                                .height((4 * scale).dp)
                                .background(textBaseColor.copy(alpha = 0.1f))
                        )
                    }

                    // Visual simulated signature seal
                    Box(
                        modifier = Modifier
                            .size((28 * scale).dp)
                            .clip(RoundedCornerShape(50))
                            .background(PDFRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "WPS",
                            fontSize = (6 * scale).sp,
                            fontWeight = FontWeight.Bold,
                            color = PDFRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer metadata
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(textBaseColor.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Confidential • WPS Office Global PDF Sandbox • Page $pageNumber / ${pdfFile.pageCount}",
                fontSize = (7 * scale).sp,
                color = textBaseColor.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // -----------------------------------------------------
        // Interactive Custom Freehand Drawing Canvas Overlay
        // -----------------------------------------------------
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeAnnotationTool) {
                    if (activeAnnotationTool == "ink") {
                        detectDragGestures(
                            onDragStart = { offset ->
                                pageInkPaths.add(listOf(offset))
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val lastIndex = pageInkPaths.lastIndex
                                if (lastIndex >= 0) {
                                    val lastPath = pageInkPaths[lastIndex]
                                    pageInkPaths[lastIndex] = lastPath + change.position
                                }
                            }
                        )
                    }
                }
        ) {
            for (pathPoints in pageInkPaths) {
                if (pathPoints.size > 1) {
                    val path = Path()
                    path.moveTo(pathPoints[0].x, pathPoints[0].y)
                    for (i in 1 until pathPoints.size) {
                        path.lineTo(pathPoints[i].x, pathPoints[i].y)
                    }
                    drawPath(
                        path = path,
                        color = annotationColor,
                        style = Stroke(width = 5f, cap = StrokeCap.Round)
                    )
                } else if (pathPoints.size == 1) {
                    drawCircle(
                        color = annotationColor,
                        radius = 3f,
                        center = pathPoints[0]
                    )
                }
            }
        }

        // -----------------------------------------------------
        // Vector Shapes, Sticky notes and Text boxes Gestures Layer
        // -----------------------------------------------------
        val density = androidx.compose.ui.platform.LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeAnnotationTool) {
                    detectTapGestures { offset ->
                        if (activeAnnotationTool == "shape") {
                            pageShapeAnnotations.add(Triple(activeShapeType, offset, annotationColor))
                        } else if (activeAnnotationTool == "text") {
                            activeInteractiveTapOffset = offset
                            showTextAnnotationInput = true
                        } else if (activeAnnotationTool == "sticky") {
                            activeInteractiveTapOffset = offset
                            showStickyNoteInput = true
                        }
                    }
                }
        ) {
            // Shapes Drawing
            for ((type, offset, color) in pageShapeAnnotations) {
                val dx = with(density) { offset.x.toDp() }
                val dy = with(density) { offset.y.toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = dx, y = dy)
                ) {
                    when (type) {
                        "square" -> {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(2.dp, color, RoundedCornerShape(2.dp))
                            )
                        }
                        "circle" -> {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(2.dp, color, CircleShape)
                            )
                        }
                        "arrow" -> {
                            Icon(
                                imageVector = Icons.Default.TrendingFlat,
                                contentDescription = "Arrow",
                                tint = color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        "line" -> {
                            Box(
                                modifier = Modifier
                                    .width(45.dp)
                                    .height(2.5.dp)
                                    .background(color)
                            )
                        }
                    }
                }
            }

            // Custom Text Annotations
            for ((text, offset, color) in pageTextAnnotations) {
                val dx = with(density) { offset.x.toDp() }
                val dy = with(density) { offset.y.toDp() }
                Text(
                    text = text,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .offset(x = dx, y = dy)
                        .background(pageCanvasBg.copy(alpha = 0.85f))
                        .border(0.5.dp, color, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Sticky Notes (📌) with dynamic fold/unfold layout
            for (notePair in pageStickyNotes) {
                val (noteContent, offset) = notePair
                val dx = with(density) { offset.x.toDp() }
                val dy = with(density) { offset.y.toDp() }
                var isNotesExpanded by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .offset(x = dx, y = dy)
                ) {
                    IconButton(
                        onClick = { isNotesExpanded = !isNotesExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned note",
                            tint = WarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (isNotesExpanded) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(top = 22.dp)
                                .widthIn(max = 120.dp)
                                .shadow(2.dp)
                        ) {
                            Text(
                                text = noteContent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Input dialogues
    if (showTextAnnotationInput && activeInteractiveTapOffset != null) {
        var inputTxt by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextAnnotationInput = false },
            title = { Text(if (lang == "ar") "إضافة نص على الصفحة" else "Add Text on PDF Page") },
            text = {
                OutlinedTextField(
                    value = inputTxt,
                    onValueChange = { inputTxt = it },
                    placeholder = { Text(if (lang == "ar") "اكتب النص المراد عرضه..." else "Type annotation text...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputTxt.isNotBlank()) {
                            pageTextAnnotations.add(Triple(inputTxt, activeInteractiveTapOffset!!, annotationColor))
                        }
                        showTextAnnotationInput = false
                    }
                ) {
                    Text(if (lang == "ar") "إضافة" else "Add")
                }
            }
        )
    }

    if (showStickyNoteInput && activeInteractiveTapOffset != null) {
        var typedNote by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showStickyNoteInput = false },
            title = { Text(if (lang == "ar") "تعليق لاصق (Sticky Note)" else "Add Sticky Note Point") },
            text = {
                OutlinedTextField(
                    value = typedNote,
                    onValueChange = { typedNote = it },
                    placeholder = { Text(if (lang == "ar") "اكتب ملحوظة قابلة للطي والفتح..." else "Type sticky comment...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (typedNote.isNotBlank()) {
                            pageStickyNotes.add(Pair(typedNote, activeInteractiveTapOffset!!))
                        }
                        showStickyNoteInput = false
                    }
                ) {
                    Text(if (lang == "ar") "حفظ" else "Save")
                }
            }
        )
    }
}

// ------------------- HIGH QUALITY REAL CHAT INTERACTION & ANNOTATION PARAGRAPHS -------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveParagraph(
    text: String,
    scale: Float,
    textBaseColor: Color,
    lang: String
) {
    var selectStart by remember { mutableStateOf(5) }
    var selectEnd by remember { mutableStateOf(40) }
    var isSelectedMode by remember { mutableStateOf(false) }
    var isHighlighted by remember { mutableStateOf(false) }
    var isUnderlined by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var highlightColor by remember { mutableStateOf(Color(0xFFFFEE58)) } // Default vivid yellow highlight
    var savedNote by remember { mutableStateOf<String?>(null) }
    var translationText by remember { mutableStateOf<String?>(null) }
    var isAddingNote by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current
    
    // Safety boundaries for indices
    val safeText = text.takeIf { it.isNotBlank() } ?: "نص مستند WPS فارغ أو غير متاح."
    val startIdx = selectStart.coerceIn(0, safeText.length - 2)
    val endIdx = selectEnd.coerceIn(startIdx + 1, safeText.length)
    
    val highlightedPart = safeText.substring(startIdx, endIdx)
    val prefixText = safeText.substring(0, startIdx)
    val suffixText = safeText.substring(endIdx)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onLongClick = {
                    isSelectedMode = true
                    showMenu = true
                },
                onClick = {
                    if (isSelectedMode) {
                        showMenu = !showMenu
                    }
                }
            )
    ) {
        // Text Context Menu Overlays
        AnimatedVisibility(visible = isSelectedMode && showMenu) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // COPY with Arabic LTR/RTL layout orientation preservation
                        ContextMenuItem(
                            icon = Icons.Default.ContentCopy,
                            label = if (lang == "ar") "نسخ" else "Copy",
                            color = WPSBlue,
                            scale = scale,
                            onClick = {
                                val rtlPreservedText = if (lang == "ar") "\u200F$highlightedPart" else highlightedPart
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("WPS PDF Select", rtlPreservedText)
                                clipboard.setPrimaryClip(clip)
                                
                                val toastMsg = if (lang == "ar") "تم نسخ النص وتنسيق اتجاه اليمين لليسار (RTL)" else "Text copied with formatting layout preservation"
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                isSelectedMode = false
                            }
                        )

                        // HIGHLIGHT WITH COLOR CHOICES (أصفر، أخضر، أزرق، وردي)
                        ContextMenuItem(
                            icon = Icons.Default.BorderColor,
                            label = if (lang == "ar") "تمييز" else "Highlight",
                            color = WarningAmber,
                            scale = scale,
                            onClick = {
                                isHighlighted = !isHighlighted
                            }
                        )

                        // UNDERLINE (تحته خط)
                        ContextMenuItem(
                            icon = Icons.Default.FormatUnderlined,
                            label = if (lang == "ar") "خط تحت" else "Underline",
                            color = SuccessGreen,
                            scale = scale,
                            onClick = {
                                isUnderlined = !isUnderlined
                                isSelectedMode = false
                            }
                        )

                        // STRIKETHROUGH (يتوسطه خط)
                        ContextMenuItem(
                            icon = Icons.Default.FormatStrikethrough,
                            label = if (lang == "ar") "يتوسطه خط" else "Strikethrough",
                            color = PDFRed,
                            scale = scale,
                            onClick = {
                                isStrikethrough = !isStrikethrough
                                isSelectedMode = false
                            }
                        )

                        // TRANSLATE
                        ContextMenuItem(
                            icon = Icons.Default.Translate,
                            label = if (lang == "ar") "ترجمة" else "Translate",
                            color = SuccessGreen,
                            scale = scale,
                            onClick = {
                                translationText = if (lang == "ar") {
                                    translateToEn(highlightedPart)
                                } else {
                                    translateToAr(highlightedPart)
                                }
                                showMenu = false
                            }
                        )

                        // SEARCH
                        ContextMenuItem(
                            icon = Icons.Default.Search,
                            label = if (lang == "ar") "بحث" else "Search",
                            color = WPSBlueDark,
                            scale = scale,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$highlightedPart"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        )

                        // NOTE
                        ContextMenuItem(
                            icon = Icons.Default.EditNote,
                            label = if (lang == "ar") "ملحوظة" else "Add Note",
                            color = PDFRed,
                            scale = scale,
                            onClick = {
                                isAddingNote = true
                            }
                        )
                    }

                    // Highlight Colors Palette Selection Row (Yellow, Green, Blue, Pink)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (lang == "ar") "اللون:" else "Color:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        listOf(
                            Color(0xFFFFEE58) to "أصفر",
                            Color(0xFF66BB6A) to "أخضر",
                            Color(0xFF42A5F5) to "أزرق",
                            Color(0xFFEC407A) to "وردي"
                        ).forEach { (colorCode, label) ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorCode)
                                    .border(if (highlightColor == colorCode && isHighlighted) 1.5.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clickable {
                                        highlightColor = colorCode
                                        isHighlighted = true
                                    }
                            )
                        }
                    }
                }
            }
        }

        // Translation popup
        if (translationText != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "❖ ${translationText}",
                        fontSize = (11 * scale).sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { translationText = null },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Annotation Notes Label Block
        if (savedNote != null) {
            Surface(
                color = WarningAmber.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, WarningAmber),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.NoteAlt, contentDescription = "Annotated Note", tint = WarningAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = savedNote!!,
                        fontSize = (10 * scale).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { savedNote = null },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Annotation", tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Selected & Highlighted Text Block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            val textDecoration = when {
                isUnderlined && isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.combine(
                    listOf(androidx.compose.ui.text.style.TextDecoration.Underline, androidx.compose.ui.text.style.TextDecoration.LineThrough)
                )
                isUnderlined -> androidx.compose.ui.text.style.TextDecoration.Underline
                isStrikethrough -> androidx.compose.ui.text.style.TextDecoration.LineThrough
                else -> androidx.compose.ui.text.style.TextDecoration.None
            }

            if (isSelectedMode) {
                // Interactive Selection Highlight Area
                androidx.compose.ui.text.buildAnnotatedString {
                    append(prefixText)
                    pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            background = WPSAccent.copy(alpha = 0.3f),
                            color = WPSBlueDark,
                            textDecoration = textDecoration
                        )
                    )
                    append(highlightedPart)
                    pop()
                    append(suffixText)
                }.let { annotatedString ->
                    Text(
                        text = annotatedString,
                        fontSize = (12 * scale).sp,
                        color = textBaseColor,
                        lineHeight = (18 * scale).sp,
                        textAlign = if (lang == "ar") TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                androidx.compose.ui.text.buildAnnotatedString {
                    append(prefixText)
                    pushStyle(
                        androidx.compose.ui.text.SpanStyle(
                            background = if (isHighlighted) highlightColor.copy(alpha = 0.35f) else Color.Transparent,
                            textDecoration = textDecoration
                        )
                    )
                    append(highlightedPart)
                    pop()
                    append(suffixText)
                }.let { annotatedString ->
                    Text(
                        text = annotatedString,
                        fontSize = (12 * scale).sp,
                        color = textBaseColor,
                        lineHeight = (18 * scale).sp,
                        textAlign = if (lang == "ar") TextAlign.Right else TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Draggable Selection Handles (Drag start/end handles to modify selected indexes)
        if (isSelectedMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Drag start thumb controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = (dragAmount.x / 10).toInt()
                                val nextStart = (selectStart + delta).coerceIn(0, selectEnd - 1)
                                selectStart = nextStart
                            }
                        }
                        .background(WPSBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.ArrowLeft, contentDescription = "Drag start selection index", tint = WPSBlue, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (lang == "ar") "البداية: $selectStart" else "Start: $selectStart",
                        fontSize = (8 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = WPSBlue
                    )
                }

                // Confirm Selection Done Check mark
                IconButton(
                    onClick = { isSelectedMode = false },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Confirm index selection", tint = SuccessGreen)
                }

                // Drag end thumb controller
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val delta = (dragAmount.x / 10).toInt()
                                val nextEnd = (selectEnd + delta).coerceIn(selectStart + 1, safeText.length)
                                selectEnd = nextEnd
                            }
                        }
                        .background(WPSBlue.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (lang == "ar") "النهاية: $selectEnd" else "End: $selectEnd",
                        fontSize = (8 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = WPSBlue
                    )
                    Icon(imageVector = Icons.Default.ArrowRight, contentDescription = "Drag end selection index", tint = WPSBlue, modifier = Modifier.size(14.dp))
                }
            }
        }
    }

    // Interactive Annotations Notes Dialog Builder
    if (isAddingNote) {
        var noteContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isAddingNote = false },
            title = {
                Text(
                    text = if (lang == "ar") "كتابة ملحوظة على النص المحدد" else "Add Annotation Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    placeholder = { Text(if (lang == "ar") "أدخل نص الملحوظة الاحترافية هنا..." else "Enter comment note text...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteContent.isNotBlank()) {
                            savedNote = noteContent
                        }
                        isAddingNote = false
                        isSelectedMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WPSBlue)
                ) {
                    Text(if (lang == "ar") "حفظ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNote = false }) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }
}

// Context Menu Mini Items inside the overlay bar
@Composable
fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    color: Color,
    scale: Float,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.08f),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size((14 * scale).dp))
            Text(text = label, fontSize = (9 * scale).sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// Simulated Machine Translation engine translating Arabic text selection directly
private fun translateToEn(arText: String): String {
    return when {
        arText.contains("مرحباً") || arText.contains("ترحيب") || arText.contains("بكم") -> "Welcome to the WPS Office Pro PDF document Guide."
        arText.contains("الأجهزة") || arText.contains("الهواتف") || arText.contains("تصفح") -> "Supports high dynamic scaling and smooth dual navigation."
        arText.contains("حماية") || arText.contains("العين") || arText.contains("التكيف") -> "Intelligent custom eye protection triggers to prevent fatigue."
        arText.contains("المجلدات") || arText.contains("مجلد") || arText.contains("تصنيف") -> "Automatic indexing helps manage local library archives."
        else -> "WPS Translation Active: Selected content processed with precision."
    }
}

private fun translateToAr(enText: String): String {
    return "ترجمة المستند: المحتوى المحدد يعالج بيانات مرجعية ممتازة وسرعة عالية وتزامن دائم وحجم نصوص ذكي."
}

// Dynamic Document Database providing real content passages depending on document and language
@Composable
fun getPageText(pdfFile: PdfFile, pageNumber: Int, lang: String): String {
    val isGerman = pdfFile.title.contains("de", ignoreCase = true) || pdfFile.title.contains("Einführung", ignoreCase = true) || pdfFile.title.contains("deutsch", ignoreCase = true) || lang == "de"
    if (isGerman) {
        return when (pageNumber % 4) {
            1 -> "Willkommen beim professionellen WPS PDF Reader. Bitte die Geduld erlernen. Jedes Wort ist vorteilhaft für Ihren Erfolg."
            2 -> "Wir wollen eine neue Sprache studieren. Bitte nicht aufgeben. Es ist wunderbar und einfach zu verstehen."
            3 -> "Sie können entscheiden wie Sie lernen. Unser System ist sehr begeistert von Ihrem täglichen Fortschritt."
            else -> "Ein kluger Kopf lernt täglich. Geduld bringt den größten Erfolg in Ihrer professionellen Entwicklung."
        }
    }
    
    return if (lang == "ar" || pdfFile.title.contains("تقرير") || pdfFile.title.contains("تجربة")) {
        when (pageNumber % 4) {
            1 -> "مرحباً بكم في الدليل الشامل لقارئ المستندات الاحترافي ٢٠٢٦ من فريق تطوير WPS. نهدف إلى تقديم أفضل تجربة قراءة على الهواتف الذكية مع ميزات فريدة تمكنك من مراجعة كل ملفاتك المحلية بسهولة."
            2 -> "يدعم التطبيق محرك التكبير الفائق مع الحفاظ على وضوح الخطوط وجودتها العالية، وتصفح سلس ثنائي الاتجاه للتوافق مع شاشات الجوال والأجهزة اللوحية ويدعم تدوير المستند ديناميكياً."
            3 -> "تتضمن الميزات الذكية لهذا الإصدار محاذاة العرض التلقائي، وحماية العين التكيفية، والتنقل الفوري بين الفصول باستخدام المعالجة السريعة للمستند للوصول السريع بدون قلق حول البطارية."
            else -> "تعتمد أدوات المكتبة على تصنيف المجلدات لتسهيل الوصول للأبحاث والتقارير اليومية مع توفير التزامن المحلي والذاكرة المؤقتة السريعة التي تضمن لك إيجاد مستنداتك وتصفحها بسلاسة."
        }
    } else {
        when (pageNumber % 4) {
            1 -> "Welcome to the ultimate WPS PDF reader. This document details custom tools, gesture behaviors, double-tap quick-zoom settings, and text selection modes designed for absolute reading comfort."
            2 -> "The layout details a highly efficient rendering workflow. Pinch with two fingers to change magnification from 25% up to 500% instantly, or double-tap to scale straight to 150% and back."
            3 -> "RTL representation is fully preserved during translation, highlights, and custom clipboard copies. Long press on any paragraph to activate Draggable Handles and the advanced Context Menu."
            else -> "Saved notes and annotation layers are preserved securely. Use standard navigation controls or switch continuous vertical scroll on to enjoy smooth page transitions across sessions."
        }
    }
}

// ------------------- FILE CREATION HANDLER DIALOG -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePdfDialog(
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = LocaleHelper.getString("create_pdf_title", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(LocaleHelper.getString("create_pdf_placeholder", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(LocaleHelper.getString("create_pdf_folder", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, folderName) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WPSBlue)
            ) {
                Text(LocaleHelper.getString("create_pdf_confirm", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(LocaleHelper.getString("create_pdf_cancel", lang))
            }
        }
    )
}

// ------------------- FILE PERMISSION EXPLANATORY SCREEN GUIDE -------------------
@Composable
fun PermissionExplanatoryGate(
    viewModel: PdfViewModel,
    lang: String,
    onCheckPermissionNow: () -> Unit
) {
    val context = LocalContext.current
    var rejectionClickCount by remember { mutableStateOf(0) }

    // Launcher for standard runtime permissions (READ_EXTERNAL_STORAGE)
    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setPermissionState(true)
            viewModel.setShowPermissionExplanatory(false)
            onCheckPermissionNow()
        } else {
            // Track rejection clicks to automatically bypass and launch settings when denied!
            rejectionClickCount++
            if (rejectionClickCount >= 1) {
                Toast.makeText(context, LocaleHelper.getString("permissions_settings_toast", lang), Toast.LENGTH_LONG).show()
                // Action: Direct Settings opening flow as requested!
                launchSystemPermissionSettings(context)
            }
        }
    }

    // Modal Sheet or Card panel block for permissions Onboarding step-by-step
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(enabled = false) {}, // consume taps to ensure modal gate
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Onboarding visual graphics icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(50))
                        .background(WarningAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Filer Access Required",
                        tint = WarningAmber,
                        modifier = Modifier.size(35.dp)
                    )
                }

                Text(
                    text = LocaleHelper.getString("permissions_title", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = LocaleHelper.getString("permissions_desc", lang),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 20.sp
                )

                Divider()

                // Step-by-Step UI Tutorial Map
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = LocaleHelper.getString("permissions_step1", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = LocaleHelper.getString("permissions_step2", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Text(
                        text = LocaleHelper.getString("permissions_step3", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Interaction button triggers. First asks standard, if fails redirects settings instantly
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Android 11+ requires MANAGE_EXTERNAL_STORAGE intent trigger directly
                            launchSystemPermissionSettings(context)
                            // Dismiss gate overlay momentarily so they can re-inspect when returning
                            viewModel.setShowPermissionExplanatory(false)
                        } else {
                            // Android 10 and under uses standard manifest runtime request
                            readPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WPSBlue),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = LocaleHelper.getString("permissions_grant", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        // Dismiss gate manually
                        viewModel.setShowPermissionExplanatory(false)
                    }
                ) {
                    Text(text = LocaleHelper.getString("cancel", lang), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
        }
    }
}

// Utility function to launch full system settings directories
private fun launchSystemPermissionSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        // Fallback open general settings panel
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
