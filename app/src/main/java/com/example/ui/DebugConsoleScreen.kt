package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
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
fun DebugConsoleScreen(
    viewModel: CleanViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var filterText by remember { mutableStateOf("") }
    var selectedLogLevel by remember { mutableStateOf(LogLevel.ALL) }
    var terminalInput by remember { mutableStateOf("") }

    val logEntries by viewModel.logcatEntries.collectAsState()
    val isLogcatLoading by viewModel.isLogcatLoading.collectAsState()
    val terminalOutput by viewModel.terminalOutput.collectAsState()
    val isTerminalExecuting by viewModel.isTerminalExecuting.collectAsState()

    BackHandler(onBack = onBack)

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
                                .background(AmberWarning.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Herramientas de Debug",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Logcat en Vivo • Consola Shell • LeakCanary",
                                fontSize = 11.sp,
                                color = AmberWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("debug_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = { viewModel.refreshLogcat(filterText, selectedLogLevel) },
                            enabled = !isLogcatLoading,
                            modifier = Modifier.testTag("refresh_logcat_button")
                        ) {
                            if (isLogcatLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = AmberWarning,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refrescar Logs",
                                    tint = TextPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.clearLogcat() },
                            modifier = Modifier.testTag("clear_logcat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Limpiar Logcat",
                                tint = RedDanger
                            )
                        }
                    } else if (selectedTab == 1) {
                        IconButton(
                            onClick = { viewModel.clearTerminalOutput() },
                            modifier = Modifier.testTag("clear_terminal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar Consola",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TechDarkBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Pestañas Superiores
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TechDarkSurface,
                contentColor = CyanPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = when (selectedTab) {
                            0 -> AmberWarning
                            1 -> CyanPrimary
                            else -> Color(0xFFFDE047)
                        }
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logcat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = AmberWarning,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shell", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = CyanPrimary,
                    unselectedContentColor = TextSecondary
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LeakCanary", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    selectedContentColor = Color(0xFFFDE047),
                    unselectedContentColor = TextSecondary
                )
            }

            // Banner interactivo de LeakCanary en el APK
            Surface(
                color = Color(0xFF1E1B4B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedTab = 2 }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🐤 LeakCanary 2.14:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE047)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rastreo activo. Toca para ver controles y diagnóstico.",
                        fontSize = 10.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (selectedTab == 0) {
                // ==================== LOGCAT TAB ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Barra de búsqueda rápida
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = {
                            filterText = it
                            viewModel.refreshLogcat(it, selectedLogLevel)
                        },
                        placeholder = { Text("Filtrar logs por texto o tag...", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (filterText.isNotEmpty()) {
                                IconButton(onClick = {
                                    filterText = ""
                                    viewModel.refreshLogcat("", selectedLogLevel)
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar filtro", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("logcat_filter_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TechDarkSurface,
                            unfocusedContainerColor = TechDarkSurface,
                            focusedBorderColor = AmberWarning,
                            unfocusedBorderColor = TechDarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chips de niveles de Logcat
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LogLevel.values()) { level ->
                            FilterChip(
                                selected = selectedLogLevel == level,
                                onClick = {
                                    selectedLogLevel = level
                                    viewModel.refreshLogcat(filterText, level)
                                },
                                label = { Text(level.name, fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (level) {
                                        LogLevel.ERROR -> RedDanger.copy(alpha = 0.3f)
                                        LogLevel.WARN -> AmberWarning.copy(alpha = 0.3f)
                                        LogLevel.INFO -> EmeraldSuccess.copy(alpha = 0.3f)
                                        LogLevel.DEBUG -> CyanPrimary.copy(alpha = 0.3f)
                                        else -> TechDarkSurfaceVariant
                                    },
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val listState = rememberLazyListState()

                    LaunchedEffect(logEntries.size) {
                        if (logEntries.isNotEmpty()) {
                            listState.scrollToItem(logEntries.size - 1)
                        }
                    }

                    Surface(
                        color = Color(0xFF070B14),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, TechDarkCardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (logEntries.isEmpty() && !isLogcatLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No se encontraron logs coincidentes. Toca 'Refrescar'.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(logEntries, key = { it.id }) { entry ->
                                    val logColor = when (entry.level) {
                                        LogLevel.ERROR -> RedDanger
                                        LogLevel.WARN -> AmberWarning
                                        LogLevel.INFO -> EmeraldSuccess
                                        LogLevel.DEBUG -> CyanPrimary
                                        LogLevel.VERBOSE -> TextSecondary
                                        LogLevel.ALL -> TextMuted
                                    }

                                    Text(
                                        text = entry.raw,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = logColor,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // ==================== TERMINAL SHELL TAB ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Comandos Rápidos:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val quickCommands = listOf(
                        "id",
                        "whoami",
                        "pm list packages | head -n 15",
                        "ls -la /sdcard",
                        "ls -la /sdcard/Android/data",
                        "df -h /data /sdcard",
                        "sh /data/local/tmp/scan_other_storage.sh"
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickCommands) { cmd ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TechDarkSurfaceVariant,
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.testTag("quick_cmd_${cmd.take(5)}")
                            ) {
                                Button(
                                    onClick = {
                                        terminalInput = cmd
                                        viewModel.executeTerminalCommand(cmd)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = cmd, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Pantalla de terminal
                    val terminalScrollState = rememberScrollState()
                    LaunchedEffect(terminalOutput) {
                        terminalScrollState.scrollTo(terminalScrollState.maxValue)
                    }

                    Surface(
                        color = Color(0xFF030712),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp)
                                .verticalScroll(terminalScrollState)
                        ) {
                            Text(
                                text = terminalOutput,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF4ADE80), // Verde consola
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Input de comando personalizado
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = terminalInput,
                            onValueChange = { terminalInput = it },
                            placeholder = { Text("Escribe comando shell (ej: pm list packages)...", fontSize = 12.sp, color = TextMuted) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("terminal_command_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = TechDarkSurface,
                                unfocusedContainerColor = TechDarkSurface,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = TechDarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (terminalInput.isNotBlank()) {
                                    viewModel.executeTerminalCommand(terminalInput)
                                    terminalInput = ""
                                }
                            },
                            enabled = !isTerminalExecuting && terminalInput.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("run_terminal_command_button")
                        ) {
                            if (isTerminalExecuting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Send, contentDescription = "Ejecutar", tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                // ==================== LEAKCANARY TAB ====================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TechDarkSurface),
                        border = BorderStroke(1.dp, Color(0xFFFDE047).copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🐤", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "LeakCanary 2.14 Activo",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFDE047)
                                    )
                                    Text(
                                        text = "Rastreador de fugas de memoria en tiempo real",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "• Estado: Iniciado automáticamente en CleanerApp con umbral sensible (1 objeto retenido).\n" +
                                       "• Notificaciones: Con el permiso POST_NOTIFICATIONS concedido, verás el aviso sonoro y visual en la barra superior al detectar fugas.\n" +
                                       "• App Leaks: Se instala automáticamente en tu cajón de aplicaciones de Android con un icono de patito amarillo.",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Text(
                        text = "Acciones de Diagnóstico",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { viewModel.openLeaksApp(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_leaks_app_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFDE047),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir Aplicación «Leaks»", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.forceLeakCanaryDump(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("force_heap_dump_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Forzar Volcado de Heap y Notificar", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.triggerTestLeak() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("trigger_test_leak_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning),
                        border = BorderStroke(1.dp, AmberWarning),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simular Fuga de Prueba (512 KB)")
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TechDarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "¿Cómo probar que funciona ahora mismo?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. Toca 'Simular Fuga de Prueba'.\n2. Toca 'Forzar Volcado de Heap y Notificar'.\n3. Verás la notificación inmediata de LeakCanary en la barra superior de Android con el análisis completo.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
