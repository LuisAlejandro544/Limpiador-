package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.formatStorageSize
import com.example.ui.components.CleanSummaryDialog
import com.example.ui.components.CleaningProgressDialog
import com.example.ui.components.JunkCategoryItem
import com.example.ui.components.OtherStorageSection
import com.example.ui.components.ScanningProgressDialog
import com.example.ui.components.ShizukuStatusCard
import com.example.ui.components.StorageCircularGauge
import com.example.ui.components.StoragePermissionBanner
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.TechDarkBackground
import com.example.ui.theme.TechDarkCardBorder
import com.example.ui.theme.TechDarkSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanScreen(
    viewModel: CleanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageStats by viewModel.storageStats.collectAsState()
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val cleanProgress by viewModel.cleanProgress.collectAsState()
    val categoryGroups by viewModel.categoryGroups.collectAsState()
    val totalSelectedBytes by viewModel.totalSelectedBytes.collectAsState()
    val totalJunkBytes by viewModel.totalJunkFoundBytes.collectAsState()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsState()
    val cleanSummary by viewModel.cleanSummary.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val otherItems by viewModel.otherItems.collectAsState()
    val isScanningOther by viewModel.isScanningOther.collectAsState()
    val otherStatusMessage by viewModel.otherStatusMessage.collectAsState()
    val totalOtherSafeBytes by viewModel.totalOtherSafeBytes.collectAsState()
    val totalOtherSelectedBytes by viewModel.totalOtherSelectedBytes.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TechDarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Limpieza Profunda",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Prototipo Shizuku",
                                fontSize = 11.sp,
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.refreshStorageAndShizuku(context)
                            viewModel.startScan(context)
                        },
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar y escanear",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechDarkBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = TechDarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, TechDarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Total a liberar:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = formatStorageSize(totalSelectedBytes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSelectedBytes > 0) EmeraldSuccess else TextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.startCleaning(context) },
                        enabled = totalSelectedBytes > 0 && !isCleaning && !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clean_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (totalSelectedBytes > 0) Color.Black else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Limpiar Ahora",
                            color = if (totalSelectedBytes > 0) Color.Black else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Storage Circular Gauge
            item {
                StorageCircularGauge(
                    stats = storageStats,
                    junkFoundBytes = totalJunkBytes
                )
            }

            // Shizuku Integration Card
            item {
                ShizukuStatusCard(
                    shizukuInfo = shizukuInfo,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                    onRefresh = { viewModel.refreshStorageAndShizuku(context) },
                    onAutoGrantStorage = { viewModel.autoGrantStoragePermissionsWithShizuku(context) },
                    onTrimSystemCaches = { viewModel.trimSystemCachesWithShizuku(context) }
                )
            }

            // Permission Warning Banner if not granted
            if (!hasStoragePermission) {
                item {
                    StoragePermissionBanner(
                        onDismiss = { viewModel.checkStoragePermission() },
                        isShizukuAuthorized = shizukuInfo.status == com.example.model.ShizukuStatus.AUTHORIZED,
                        onGrantWithShizuku = { viewModel.autoGrantStoragePermissionsWithShizuku(context) }
                    )
                }
            }

            // Apartado de Análisis Profundo de "Otros" (Scripts Shell + Shizuku)
            item {
                OtherStorageSection(
                    items = otherItems,
                    isScanning = isScanningOther,
                    statusMessage = otherStatusMessage,
                    totalSafeBytes = totalOtherSafeBytes,
                    totalSelectedBytes = totalOtherSelectedBytes,
                    onStartScan = { viewModel.startOtherStorageScan(context) },
                    onToggleItem = { viewModel.toggleOtherItemSelection(it) },
                    onSelectAllSafe = { viewModel.selectAllSafeOtherItems() },
                    onDeselectAll = { viewModel.deselectAllOtherItems() },
                    onCleanSelected = { viewModel.cleanSelectedOtherItems(context) }
                )
            }

            // Category list header with rescan button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Categorías de Limpieza",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Selecciona los elementos a eliminar",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.startScan(context) },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier.height(36.dp).testTag("rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escanear", color = CyanPrimary, fontSize = 12.sp)
                    }
                }
            }

            // Category items
            items(
                items = categoryGroups,
                key = { it.type.name }
            ) { group ->
                JunkCategoryItem(
                    group = group,
                    onToggleSelect = { viewModel.toggleCategorySelection(group.type) },
                    onToggleExpand = { viewModel.toggleCategoryExpanded(group.type) },
                    onToggleFileSelect = { filePath -> viewModel.toggleItemSelection(filePath) }
                )
            }
        }
    }

    // Active Scanning Dialog
    if (isScanning) {
        ScanningProgressDialog(progress = scanProgress)
    }

    // Active Cleaning Dialog
    if (isCleaning) {
        CleaningProgressDialog(progress = cleanProgress)
    }

    // Finished Clean Summary Dialog
    cleanSummary?.let { summary ->
        CleanSummaryDialog(
            summary = summary,
            onDismiss = { viewModel.dismissSummary() }
        )
    }
}
