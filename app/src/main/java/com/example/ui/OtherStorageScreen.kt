package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OtherStorageItem
import com.example.model.SafetyLevel
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
fun OtherStorageScreen(
    viewModel: CleanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.otherItems.collectAsState()
    val isScanning by viewModel.isScanningOther.collectAsState()
    val statusMessage by viewModel.otherStatusMessage.collectAsState()
    val totalSafeBytes by viewModel.totalOtherSafeBytes.collectAsState()
    val totalSelectedBytes by viewModel.totalOtherSelectedBytes.collectAsState()

    var selectedSafetyFilter by remember { mutableStateOf<SafetyLevel?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }

    // Interceptar el botón físico o gesto de volver atrás del teléfono
    BackHandler(onBack = onBack)

    val availableCategories = remember(items) {
        items.map { it.category }.distinct()
    }

    val filteredItems = remember(items, selectedSafetyFilter, selectedCategoryFilter) {
        items.filter { item ->
            val matchSafety = selectedSafetyFilter == null || item.safety == selectedSafetyFilter
            val matchCategory = selectedCategoryFilter == null || item.category == selectedCategoryFilter
            matchSafety && matchCategory
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
                                .background(ShizukuViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ShizukuViolet,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Auditoría de «Otros»",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Inspección Profunda de Archivos Ocultos",
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
                        modifier = Modifier.testTag("other_storage_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al Dashboard",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startOtherStorageScan(context) },
                        enabled = !isScanning,
                        modifier = Modifier.testTag("other_storage_rescan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = CyanPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Volver a escanear",
                                tint = TextSecondary
                            )
                        }
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
                            text = "A liberar de «Otros»:",
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
                        onClick = { showConfirmDeleteDialog = true },
                        enabled = totalSelectedBytes > 0 && !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clean_selected_other_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (totalSelectedBytes > 0) Color.Black else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Limpiar (${formatStorageSize(totalSelectedBytes)})",
                            color = if (totalSelectedBytes > 0) Color.Black else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
            // Tarjeta de Diagnóstico / Métricas
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TechDarkSurface,
                    border = BorderStroke(1.dp, TechDarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OtherMetricCard(
                                title = "Seguro de borrar",
                                value = formatStorageSize(totalSafeBytes),
                                accentColor = EmeraldSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            OtherMetricCard(
                                title = "Seleccionado",
                                value = formatStorageSize(totalSelectedBytes),
                                accentColor = CyanPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            OtherMetricCard(
                                title = "Elementos",
                                value = "${items.size}",
                                accentColor = ShizukuViolet,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isScanning) {
                            Spacer(modifier = Modifier.height(14.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanPrimary,
                                trackColor = TechDarkSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = statusMessage.ifEmpty { "Ejecutando script Shell y analizando almacenamiento..." },
                                color = CyanPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Estado inicial si aún no se ha escaneado
            if (items.isEmpty() && !isScanning) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = TechDarkSurface,
                        border = BorderStroke(1.dp, TechDarkCardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CyanPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Auditoría Profunda de «Otros»",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Escanea en busca de miniaturas huérfanas (.thumbnails), fragmentos SQLite (.db-wal, .db-shm), bases de datos de apps desinstaladas y cachés no indexadas que inflan la memoria.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.startOtherStorageScan(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("start_deep_other_scan_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Iniciar Análisis Profundo",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            if (items.isNotEmpty()) {
                // Filtros de Nivel de Seguridad
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Filtrar por nivel de seguridad:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSafetyFilter == null,
                                onClick = { selectedSafetyFilter = null },
                                label = { Text("Todos (${items.size})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TechDarkSurfaceVariant,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = selectedSafetyFilter == SafetyLevel.SAFE,
                                onClick = { selectedSafetyFilter = if (selectedSafetyFilter == SafetyLevel.SAFE) null else SafetyLevel.SAFE },
                                label = { Text("Seguros", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSuccess.copy(alpha = 0.25f),
                                    selectedLabelColor = EmeraldSuccess
                                )
                            )
                            FilterChip(
                                selected = selectedSafetyFilter == SafetyLevel.CAUTION,
                                onClick = { selectedSafetyFilter = if (selectedSafetyFilter == SafetyLevel.CAUTION) null else SafetyLevel.CAUTION },
                                label = { Text("Precaución", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberWarning.copy(alpha = 0.25f),
                                    selectedLabelColor = AmberWarning
                                )
                            )
                            FilterChip(
                                selected = selectedSafetyFilter == SafetyLevel.KEEP,
                                onClick = { selectedSafetyFilter = if (selectedSafetyFilter == SafetyLevel.KEEP) null else SafetyLevel.KEEP },
                                label = { Text("Vital", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RedDanger.copy(alpha = 0.25f),
                                    selectedLabelColor = RedDanger
                                )
                            )
                        }
                    }
                }

                // Filtros por Categoría si hay más de una
                if (availableCategories.size > 1) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Categorías detectadas:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == null,
                                        onClick = { selectedCategoryFilter = null },
                                        label = { Text("Todas", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TechDarkSurfaceVariant,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                                items(availableCategories) { cat ->
                                    FilterChip(
                                        selected = selectedCategoryFilter == cat,
                                        onClick = { selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat },
                                        label = { Text(cat, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                            selectedLabelColor = CyanPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Acciones rápidas de selección
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            Text(
                                text = "Marcar solo seguros",
                                color = CyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { viewModel.selectAllSafeOtherItems() }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Desmarcar todos",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { viewModel.deselectAllOtherItems() }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }

                        Text(
                            text = "${filteredItems.size} resultados",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Lista de elementos de «Otros»
                items(filteredItems, key = { it.id }) { item ->
                    OtherItemDedicatedCard(
                        item = item,
                        onToggle = { viewModel.toggleOtherItemSelection(item.id) }
                    )
                }
            }
        }
    }

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            containerColor = TechDarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Confirmar Limpieza",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Text(
                    text = "¿Deseas eliminar permanentemente los elementos seleccionados de «Otros»? Se liberarán aproximadamente ${formatStorageSize(totalSelectedBytes)} de almacenamiento.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteDialog = false
                        viewModel.cleanSelectedOtherItems(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Limpiar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun OtherMetricCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = TechDarkSurfaceVariant,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, color = TextSecondary, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OtherItemDedicatedCard(
    item: OtherStorageItem,
    onToggle: () -> Unit
) {
    val isKeep = item.safety == SafetyLevel.KEEP
    val (badgeBg, badgeText, badgeIcon) = when (item.safety) {
        SafetyLevel.SAFE -> Triple(
            EmeraldSuccess.copy(alpha = 0.15f),
            EmeraldSuccess,
            Icons.Default.CheckCircle
        )
        SafetyLevel.CAUTION -> Triple(
            AmberWarning.copy(alpha = 0.15f),
            AmberWarning,
            Icons.Default.Warning
        )
        SafetyLevel.KEEP -> Triple(
            RedDanger.copy(alpha = 0.15f),
            RedDanger,
            Icons.Default.Lock
        )
    }

    val categoryIcon: ImageVector = when {
        item.category.contains("Bases de Datos", ignoreCase = true) -> Icons.Default.Storage
        item.category.contains("Miniaturas", ignoreCase = true) -> Icons.Default.Image
        item.category.contains("Caché", ignoreCase = true) -> Icons.Default.FolderZip
        item.category.contains("OBB", ignoreCase = true) -> Icons.Default.Lock
        else -> Icons.Default.FolderOpen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isKeep) { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TechDarkSurface),
        border = BorderStroke(1.dp, if (item.isSelected) CyanPrimary.copy(alpha = 0.5f) else TechDarkCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { if (!isKeep) onToggle() },
                enabled = !isKeep,
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanPrimary,
                    uncheckedColor = TextMuted,
                    checkmarkColor = Color.Black,
                    disabledCheckedColor = Color.Gray,
                    disabledUncheckedColor = Color(0xFF334155)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.path,
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeText,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.safety.label,
                                color = badgeText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TechDarkSurfaceVariant
                    ) {
                        Text(
                            text = item.category,
                            color = TextSecondary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = formatStorageSize(item.sizeBytes),
                color = if (item.safety == SafetyLevel.SAFE) EmeraldSuccess else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
