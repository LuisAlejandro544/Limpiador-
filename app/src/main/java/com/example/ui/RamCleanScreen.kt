package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ProcessCategory
import com.example.model.RamProcessItem
import com.example.model.formatStorageSize
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.RedDanger
import com.example.ui.theme.ShizukuViolet
import com.example.ui.theme.TechDarkBackground
import com.example.ui.theme.TechDarkCardBorder
import com.example.ui.theme.TechDarkSurface
import com.example.ui.theme.TechDarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamCleanScreen(
    viewModel: CleanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ramScanResult by viewModel.ramScanResult.collectAsState()
    val isRamTrimming by viewModel.isRamTrimming.collectAsState()
    val ramTrimSummary by viewModel.ramTrimSummary.collectAsState()
    val totalSelectedRamBytes by viewModel.totalSelectedRamBytes.collectAsState()
    val shizukuInfo by viewModel.shizukuInfo.collectAsState()

    var filterCategory by remember { mutableStateOf<ProcessCategory?>(null) }

    LaunchedEffect(Unit) {
        if (ramScanResult.processes.isEmpty()) {
            viewModel.startRamScan(context)
        }
    }

    BackHandler(onBack = onBack)

    val filteredProcesses = remember(ramScanResult.processes, filterCategory) {
        if (filterCategory == null) {
            ramScanResult.processes
        } else {
            ramScanResult.processes.filter { it.category == filterCategory }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = TechDarkBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyanPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sanitizador de RAM",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Fugas de Memoria • Recorte sin Reiniciar",
                                fontSize = 11.sp,
                                color = CyanPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("ram_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startRamScan(context) },
                        enabled = !ramScanResult.isScanning && !isRamTrimming,
                        modifier = Modifier.testTag("refresh_ram_scan_button")
                    ) {
                        if (ramScanResult.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CyanPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Escanear RAM",
                                tint = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TechDarkBackground)
            )
        },
        bottomBar = {
            Surface(
                color = TechDarkSurface,
                border = BorderStroke(1.dp, TechDarkCardBorder),
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
                            text = "RAM seleccionada para sanear:",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = formatStorageSize(totalSelectedRamBytes),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (totalSelectedRamBytes > 0) CyanPrimary else TextSecondary
                        )
                    }

                    Button(
                        onClick = { viewModel.trimSelectedRam(context) },
                        enabled = totalSelectedRamBytes > 0 && !isRamTrimming && !ramScanResult.isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black,
                            disabledContainerColor = TechDarkSurfaceVariant,
                            disabledContentColor = TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("trim_ram_action_button")
                    ) {
                        if (isRamTrimming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saneando...")
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sanear RAM", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Tarjeta de Estado de RAM del Dispositivo
            item {
                RamOverviewCard(
                    stats = ramScanResult.stats,
                    isScanning = ramScanResult.isScanning
                )
            }

            // 2. Banner de Información Técnica Shizuku vs Fallback
            item {
                ShizukuRamNotice(
                    isShizukuAuthorized = shizukuInfo.status == com.example.model.ShizukuStatus.AUTHORIZED
                )
            }

            // 3. Controles de Filtrado y Selección
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Procesos en Memoria (${filteredProcesses.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.selectAllRamCandidates() },
                                modifier = Modifier.testTag("select_all_ram_button")
                            ) {
                                Text("Marcar todo", fontSize = 11.sp, color = CyanPrimary)
                            }
                            TextButton(
                                onClick = { viewModel.deselectAllRamCandidates() },
                                modifier = Modifier.testTag("deselect_all_ram_button")
                            ) {
                                Text("Desmarcar", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Chips de filtro
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = filterCategory == null,
                                onClick = { filterCategory = null },
                                label = { Text("Todos", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanPrimary,
                                    containerColor = TechDarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterCategory == ProcessCategory.HEAVY_LEAK,
                                onClick = { filterCategory = ProcessCategory.HEAVY_LEAK },
                                label = { Text("Fugas / Críticos", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedDanger.copy(alpha = 0.2f),
                                    selectedLabelColor = RedDanger,
                                    containerColor = TechDarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterCategory == ProcessCategory.CACHED,
                                onClick = { filterCategory = ProcessCategory.CACHED },
                                label = { Text("Caché / Zombis", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberWarning.copy(alpha = 0.2f),
                                    selectedLabelColor = AmberWarning,
                                    containerColor = TechDarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filterCategory == ProcessCategory.BACKGROUND,
                                onClick = { filterCategory = ProcessCategory.BACKGROUND },
                                label = { Text("2do Plano", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ShizukuViolet.copy(alpha = 0.2f),
                                    selectedLabelColor = ShizukuViolet,
                                    containerColor = TechDarkSurface,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // 4. Lista de Procesos en RAM
            if (ramScanResult.isScanning && filteredProcesses.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = ramScanResult.statusMessage.ifEmpty { "Analizando memoria..." },
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else if (filteredProcesses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TechDarkSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No hay procesos en esta categoría", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Tu memoria RAM se encuentra en niveles saludables.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredProcesses, key = { it.packageName + "_" + it.pid }) { proc ->
                    RamProcessCard(
                        item = proc,
                        onToggle = { viewModel.toggleRamProcessSelection(proc.packageName) }
                    )
                }
            }
        }
    }

    // Diálogo de Resumen de Optimización
    ramTrimSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRamSummary() },
            containerColor = TechDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RAM Optimizada con Éxito", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Se ha aplicado el saneamiento de memoria sin necesidad de reiniciar el teléfono:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Surface(
                        color = TechDarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Memoria reclamada aprox.: ${formatStorageSize(summary.freedBytes)}",
                                color = EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Procesos optimizados: ${summary.procsOptimized}",
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text = summary.details,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissRamSummary() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun RamOverviewCard(
    stats: com.example.model.RamStats,
    isScanning: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TechDarkSurface),
        border = BorderStroke(1.dp, TechDarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estado de la Memoria",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                val pct = (stats.usedPercentage * 100).toInt()
                Text(
                    text = "$pct% en uso",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pct > 80) RedDanger else if (pct > 65) AmberWarning else EmeraldSuccess
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { stats.usedPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (stats.usedPercentage > 0.8f) RedDanger else if (stats.usedPercentage > 0.65f) AmberWarning else CyanPrimary,
                trackColor = TechDarkSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total RAM", fontSize = 11.sp, color = TextMuted)
                    Text(stats.formattedTotal, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Column {
                    Text("En Uso", fontSize = 11.sp, color = TextMuted)
                    Text(stats.formattedUsed, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AmberWarning)
                }
                Column {
                    Text("Disponible", fontSize = 11.sp, color = TextMuted)
                    Text(stats.formattedAvailable, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                }
                if (stats.zramBytes > 0) {
                    Column {
                        Text("ZRAM / Swap", fontSize = 11.sp, color = TextMuted)
                        Text(stats.formattedZram, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ShizukuViolet)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShizukuRamNotice(isShizukuAuthorized: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isShizukuAuthorized) ShizukuViolet.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.12f)
        ),
        border = BorderStroke(
            1.dp,
            if (isShizukuAuthorized) ShizukuViolet.copy(alpha = 0.3f) else AmberWarning.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isShizukuAuthorized) Icons.Default.AutoAwesome else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isShizukuAuthorized) ShizukuViolet else AmberWarning,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isShizukuAuthorized) "Recorte Quirúrgico Shizuku Activo" else "Modo de Compatibilidad sin Shizuku",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isShizukuAuthorized) {
                        "Ejecuta 'am trim-memory COMPLETE' para forzar a las apps a liberar bitmaps y cachés sin provocar cierres forzosos."
                    } else {
                        "Sin Shizuku solo se cierran procesos de fondo permitidos por el sistema. Inicia Shizuku para máxima eficacia."
                    },
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RamProcessCard(
    item: RamProcessItem,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TechDarkSurface),
        border = BorderStroke(
            1.dp,
            if (item.isSelected) CyanPrimary.copy(alpha = 0.4f) else TechDarkCardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanPrimary,
                    checkmarkColor = Color.Black,
                    uncheckedColor = TextMuted
                ),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    val (badgeBg, badgeFg) = when (item.category) {
                        ProcessCategory.HEAVY_LEAK -> RedDanger.copy(alpha = 0.2f) to RedDanger
                        ProcessCategory.CACHED -> AmberWarning.copy(alpha = 0.2f) to AmberWarning
                        ProcessCategory.SYSTEM -> TextMuted.copy(alpha = 0.2f) to TextMuted
                        else -> ShizukuViolet.copy(alpha = 0.2f) to ShizukuViolet
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = item.category.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.packageName,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = item.formattedPss,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isLeakCandidate) RedDanger else CyanPrimary
                )
                Text(
                    text = "PSS en RAM",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
