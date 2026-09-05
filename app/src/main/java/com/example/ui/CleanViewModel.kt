package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.JunkCategoryGroup
import com.example.model.JunkCategoryType
import com.example.model.JunkFileItem
import com.example.model.OtherStorageItem
import com.example.model.SafetyLevel
import com.example.model.ShizukuInfo
import com.example.model.ShizukuStatus
import com.example.model.StorageStats
import com.example.scanner.CleanProgress
import com.example.scanner.CleanSummary
import com.example.scanner.OtherStorageScanner
import com.example.scanner.ScanProgress
import com.example.scanner.StorageCleaner
import com.example.scanner.StorageScanner
import com.example.shizuku.ShizukuHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.RamProcessItem
import com.example.model.RamScanResult
import com.example.model.RamStats
import com.example.model.RamTrimResult
import com.example.scanner.RamCleaner
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppScreen {
    DASHBOARD,
    OTHER_STORAGE,
    RAM_CLEANER,
    DEBUG_CONSOLE
}

class CleanViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateToOtherStorage() {
        _currentScreen.value = AppScreen.OTHER_STORAGE
    }

    fun navigateToRamCleaner(context: Context? = null) {
        _currentScreen.value = AppScreen.RAM_CLEANER
        if (context != null) {
            startRamScan(context)
        }
    }

    fun navigateToDashboard() {
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun navigateToDebugConsole() {
        _currentScreen.value = AppScreen.DEBUG_CONSOLE
        refreshLogcat()
    }

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _shizukuInfo = MutableStateFlow(ShizukuInfo())
    val shizukuInfo: StateFlow<ShizukuInfo> = _shizukuInfo.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    private val _cleanProgress = MutableStateFlow(CleanProgress())
    val cleanProgress: StateFlow<CleanProgress> = _cleanProgress.asStateFlow()

    private val _categoryGroups = MutableStateFlow<List<JunkCategoryGroup>>(emptyList())
    val categoryGroups: StateFlow<List<JunkCategoryGroup>> = _categoryGroups.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(false)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _cleanSummary = MutableStateFlow<CleanSummary?>(null)
    val cleanSummary: StateFlow<CleanSummary?> = _cleanSummary.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Estados para el Analizador de "Otros"
    private val _isScanningOther = MutableStateFlow(false)
    val isScanningOther: StateFlow<Boolean> = _isScanningOther.asStateFlow()

    private val _otherStatusMessage = MutableStateFlow("")
    val otherStatusMessage: StateFlow<String> = _otherStatusMessage.asStateFlow()

    private val _otherItems = MutableStateFlow<List<OtherStorageItem>>(emptyList())
    val otherItems: StateFlow<List<OtherStorageItem>> = _otherItems.asStateFlow()

    val totalOtherSafeBytes: StateFlow<Long> = _otherItems.map { list ->
        list.filter { it.safety == SafetyLevel.SAFE }.sumOf { it.sizeBytes }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalOtherSelectedBytes: StateFlow<Long> = _otherItems.map { list ->
        list.filter { it.isSelected }.sumOf { it.sizeBytes }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalSelectedBytes: StateFlow<Long> = _categoryGroups.map { groups ->
        groups.filter { it.isSelected }.sumOf { group ->
            group.items.filter { it.isSelected }.sumOf { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val totalJunkFoundBytes: StateFlow<Long> = _categoryGroups.map { groups ->
        groups.sumOf { it.totalSize }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    init {
        refreshStorageAndShizuku(application)
        checkStoragePermission()
        // Initialize default empty category groups
        _categoryGroups.value = JunkCategoryType.entries.map { type ->
            JunkCategoryGroup(type = type, items = emptyList())
        }
        // Auto-run initial light scan
        startScan(application)
    }

    fun checkStoragePermission() {
        _hasStoragePermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Read/write permissions handled in manifest for Android <= 10
        }
    }

    fun refreshStorageAndShizuku(context: Context) {
        viewModelScope.launch {
            _storageStats.value = StorageScanner.getStorageStats()
            _shizukuInfo.value = ShizukuHelper.getShizukuInfo(context)
            checkStoragePermission()
        }
    }

    fun requestShizukuPermission(context: Context? = null) {
        ShizukuHelper.requestPermission(context)
    }

    fun openShizuku(context: Context) {
        ShizukuHelper.openShizukuApp(context)
    }

    fun autoGrantStoragePermissionsWithShizuku(context: Context) {
        viewModelScope.launch {
            val result = ShizukuHelper.autoGrantStoragePermissions(context)
            if (result.isSuccess) {
                _actionMessage.value = "¡Permisos de almacenamiento concedidos exitosamente mediante Shizuku IPackageManager!"
                checkStoragePermission()
                startScan(context)
            } else {
                _actionMessage.value = "Error al auto-conceder permisos: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun trimSystemCachesWithShizuku(context: Context) {
        viewModelScope.launch {
            val result = ShizukuHelper.trimSystemCaches()
            if (result.isSuccess) {
                _actionMessage.value = "Cachés del sistema optimizadas y recortadas exitosamente (pm trim-caches)."
                refreshStorageAndShizuku(context)
                startScan(context)
            } else {
                _actionMessage.value = "Error al recortar cachés del sistema: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun startScan(context: Context) {
        if (_isScanning.value || _isCleaning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            StorageScanner.scanStorage(context).collect { progress ->
                _scanProgress.value = progress
                if (progress.isFinished) {
                    val newGroups = JunkCategoryType.entries.map { type ->
                        val items = progress.results[type] ?: emptyList()
                        JunkCategoryGroup(
                            type = type,
                            items = items,
                            isSelected = type.isSafeToDeleteByDefault && items.isNotEmpty()
                        )
                    }
                    _categoryGroups.value = newGroups

                    // Update storage stats with junk size
                    val junkSum = newGroups.sumOf { it.totalSize }
                    _storageStats.value = _storageStats.value.copy(junkFoundBytes = junkSum)
                    _isScanning.value = false
                }
            }
        }
    }

    fun toggleCategorySelection(type: JunkCategoryType) {
        _categoryGroups.value = _categoryGroups.value.map { group ->
            if (group.type == type) {
                val newSelect = !group.isSelected
                group.copy(
                    isSelected = newSelect,
                    items = group.items.map { it.copy(isSelected = newSelect) }
                )
            } else {
                group
            }
        }
    }

    fun toggleCategoryExpanded(type: JunkCategoryType) {
        _categoryGroups.value = _categoryGroups.value.map { group ->
            if (group.type == type) {
                group.copy(isExpanded = !group.isExpanded)
            } else {
                group
            }
        }
    }

    fun toggleItemSelection(itemPath: String) {
        _categoryGroups.value = _categoryGroups.value.map { group ->
            val hasItem = group.items.any { it.path == itemPath }
            if (hasItem) {
                val updatedItems = group.items.map { item ->
                    if (item.path == itemPath) item.copy(isSelected = !item.isSelected) else item
                }
                val anySelected = updatedItems.any { it.isSelected }
                group.copy(items = updatedItems, isSelected = anySelected)
            } else {
                group
            }
        }
    }

    fun startCleaning(context: Context) {
        if (_isCleaning.value || _isScanning.value) return

        val itemsToClean = _categoryGroups.value
            .filter { it.isSelected }
            .flatMap { group -> group.items.filter { it.isSelected } }

        val isShizukuReady = _shizukuInfo.value.status == ShizukuStatus.AUTHORIZED

        if (itemsToClean.isEmpty() && !isShizukuReady) return

        viewModelScope.launch {
            _isCleaning.value = true
            StorageCleaner.cleanSelectedItems(
                itemsToClean = itemsToClean,
                useShizukuIfAvailable = isShizukuReady
            ).collect { progress ->
                _cleanProgress.value = progress
                if (progress.isFinished) {
                    _cleanSummary.value = CleanSummary(
                        totalFreedBytes = progress.freedBytes,
                        totalDeletedFiles = progress.deletedCount,
                        shizukuTrimExecuted = isShizukuReady,
                        shizukuMessage = if (isShizukuReady) "Cachés del sistema optimizados con Shizuku (pm trim-caches)" else ""
                    )
                    _isCleaning.value = false
                    // Re-scan to update remaining files and current storage stats
                    refreshStorageAndShizuku(context)
                    startScan(context)
                }
            }
        }
    }

    fun dismissSummary() {
        _cleanSummary.value = null
    }

    /**
     * Inicia el escaneo del almacenamiento "Otros" en segundo plano (Coroutines + Dispatchers.IO)
     */
    fun startOtherStorageScan(context: Context) {
        if (_isScanningOther.value) return

        viewModelScope.launch {
            _isScanningOther.value = true
            _otherStatusMessage.value = "Iniciando análisis..."
            OtherStorageScanner.scanOtherStorageFlow(context).collect { progress ->
                _otherStatusMessage.value = progress.statusMessage
                if (progress.itemsFound.isNotEmpty()) {
                    _otherItems.value = progress.itemsFound
                }
                if (progress.isComplete) {
                    _isScanningOther.value = false
                }
            }
        }
    }

    fun toggleOtherItemSelection(id: String) {
        _otherItems.value = _otherItems.value.map { item ->
            if (item.id == id) item.copy(isSelected = !item.isSelected) else item
        }
    }

    fun selectAllSafeOtherItems() {
        _otherItems.value = _otherItems.value.map { item ->
            if (item.safety == SafetyLevel.SAFE) item.copy(isSelected = true) else item
        }
    }

    fun deselectAllOtherItems() {
        _otherItems.value = _otherItems.value.map { it.copy(isSelected = false) }
    }

    /**
     * Limpia en segundo plano los ítems seleccionados de "Otros"
     */
    fun cleanSelectedOtherItems(context: Context) {
        val selected = _otherItems.value.filter { it.isSelected && it.safety != SafetyLevel.KEEP }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _otherStatusMessage.value = "Eliminando ${selected.size} elementos seleccionados..."
            val result = OtherStorageScanner.deleteSelectedItems(selected)
            if (result.isSuccess) {
                val freed = result.getOrDefault(0L)
                _actionMessage.value = "¡Liberados ${com.example.model.formatStorageSize(freed)} de la categoría 'Otros'!"
                refreshStorageAndShizuku(context)
                // Volver a escanear en segundo plano para refrescar la lista
                startOtherStorageScan(context)
            } else {
                _actionMessage.value = "Error al limpiar elementos: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    // ==============================================================================
    // HERRAMIENTAS DE DEBUG: VISOR DE LOGCAT Y CONSOLA EN VIVO
    // ==============================================================================

    private val _logcatEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logcatEntries: StateFlow<List<LogEntry>> = _logcatEntries.asStateFlow()

    private val _isLogcatLoading = MutableStateFlow(false)
    val isLogcatLoading: StateFlow<Boolean> = _isLogcatLoading.asStateFlow()

    private val _terminalOutput = MutableStateFlow("Consola lista. Ingresa un comando o pulsa un acceso rápido.\n")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _isTerminalExecuting = MutableStateFlow(false)
    val isTerminalExecuting: StateFlow<Boolean> = _isTerminalExecuting.asStateFlow()

    fun refreshLogcat(filterText: String = "", minLevel: LogLevel = LogLevel.ALL) {
        viewModelScope.launch {
            _isLogcatLoading.value = true
            val logs = withContext(Dispatchers.IO) {
                try {
                    val process = Runtime.getRuntime().exec("logcat -d -v time -t 600")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val list = mutableListOf<LogEntry>()
                    var lineId = 0L
                    var line = reader.readLine()
                    while (line != null) {
                        val level = when {
                            line.contains(" E ") || line.startsWith("E/") -> LogLevel.ERROR
                            line.contains(" W ") || line.startsWith("W/") -> LogLevel.WARN
                            line.contains(" I ") || line.startsWith("I/") -> LogLevel.INFO
                            line.contains(" D ") || line.startsWith("D/") -> LogLevel.DEBUG
                            line.contains(" V ") || line.startsWith("V/") -> LogLevel.VERBOSE
                            else -> LogLevel.DEBUG
                        }

                        val matchesLevel = minLevel == LogLevel.ALL || level == minLevel
                        val matchesText = filterText.isBlank() || line.contains(filterText, ignoreCase = true)

                        if (matchesLevel && matchesText) {
                            list.add(
                                LogEntry(
                                    id = ++lineId,
                                    raw = line,
                                    level = level,
                                    tag = line.substringBefore(":").takeLast(20).trim(),
                                    message = line
                                )
                            )
                        }
                        line = reader.readLine()
                    }
                    reader.close()
                    process.destroy()
                    list
                } catch (e: Exception) {
                    listOf(
                        LogEntry(
                            id = 1L,
                            raw = "Error al leer logcat: ${e.message}",
                            level = LogLevel.ERROR,
                            tag = "LogcatError",
                            message = e.localizedMessage ?: "Fallo de lectura"
                        )
                    )
                }
            }
            _logcatEntries.value = logs
            _isLogcatLoading.value = false
        }
    }

    fun clearLogcat() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    Runtime.getRuntime().exec("logcat -c").waitFor()
                } catch (_: Exception) {}
            }
            _logcatEntries.value = emptyList()
        }
    }

    fun executeTerminalCommand(command: String) {
        if (command.isBlank()) return
        viewModelScope.launch {
            _isTerminalExecuting.value = true
            _terminalOutput.value += "\n$ $command\n"
            val output = withContext(Dispatchers.IO) {
                try {
                    // Si Shizuku está disponible y autorizado, ejecutamos vía Shizuku, o runtime local
                    if (ShizukuHelper.hasPermission()) {
                        val result = ShizukuHelper.executeAdbCommand(command)
                        result.getOrElse { "Error Shizuku: ${it.message}" }
                    } else {
                        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                        val stdout = process.inputStream.bufferedReader().readText()
                        val stderr = process.errorStream.bufferedReader().readText()
                        process.waitFor()
                        val full = (stdout + if (stderr.isNotBlank()) "\n[stderr]: $stderr" else "").trim()
                        if (full.isEmpty()) "(Sin salida retornada)" else full
                    }
                } catch (e: Exception) {
                    "Error al ejecutar: ${e.localizedMessage}"
                }
            }
            _terminalOutput.value += "$output\n"
            _isTerminalExecuting.value = false
        }
    }

    fun clearTerminalOutput() {
        _terminalOutput.value = "Consola limpiada.\n"
    }

    // ==============================================================================
    // GESTIÓN DE MEMORIA RAM Y DETECCIÓN DE FUGAS
    // ==============================================================================

    private val _ramScanResult = MutableStateFlow(RamScanResult())
    val ramScanResult: StateFlow<RamScanResult> = _ramScanResult.asStateFlow()

    private val _isRamTrimming = MutableStateFlow(false)
    val isRamTrimming: StateFlow<Boolean> = _isRamTrimming.asStateFlow()

    private val _ramTrimSummary = MutableStateFlow<RamTrimResult?>(null)
    val ramTrimSummary: StateFlow<RamTrimResult?> = _ramTrimSummary.asStateFlow()

    val totalSelectedRamBytes: StateFlow<Long> = _ramScanResult.map { res ->
        res.processes.filter { it.isSelected }.sumOf { it.pssBytes }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    fun startRamScan(context: Context) {
        if (_ramScanResult.value.isScanning || _isRamTrimming.value) return

        viewModelScope.launch {
            RamCleaner.scanRamFlow(context).collect { result ->
                _ramScanResult.value = result
            }
        }
    }

    fun toggleRamProcessSelection(packageName: String) {
        val current = _ramScanResult.value
        val updatedList = current.processes.map { proc ->
            if (proc.packageName == packageName) proc.copy(isSelected = !proc.isSelected) else proc
        }
        _ramScanResult.value = current.copy(processes = updatedList)
    }

    fun selectAllRamCandidates() {
        val current = _ramScanResult.value
        val updatedList = current.processes.map { it.copy(isSelected = true) }
        _ramScanResult.value = current.copy(processes = updatedList)
    }

    fun deselectAllRamCandidates() {
        val current = _ramScanResult.value
        val updatedList = current.processes.map { it.copy(isSelected = false) }
        _ramScanResult.value = current.copy(processes = updatedList)
    }

    fun trimSelectedRam(context: Context) {
        val selected = _ramScanResult.value.processes.filter { it.isSelected }
        if (selected.isEmpty() || _isRamTrimming.value) return

        viewModelScope.launch {
            _isRamTrimming.value = true
            val initialAvail = _ramScanResult.value.stats.availableBytes
            val pkgs = selected.map { it.packageName }
            val result = RamCleaner.trimSelectedProcesses(context, pkgs, initialAvail)
            if (result.isSuccess) {
                _ramTrimSummary.value = result.getOrNull()
                _actionMessage.value = "¡Memoria RAM optimizada exitosamente sin reiniciar!"
                // Re-escanear para actualizar datos en pantalla
                startRamScan(context)
            } else {
                _actionMessage.value = "Error al optimizar RAM: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isRamTrimming.value = false
        }
    }

    fun dismissRamSummary() {
        _ramTrimSummary.value = null
    }

    // ==============================================================================
    // ACCIONES DIRECTAS DE LEAKCANARY
    // ==============================================================================

    fun forceLeakCanaryDump(context: Context) {
        try {
            leakcanary.LeakCanary.dumpHeap()
            _actionMessage.value = "Volcado de Heap solicitado. LeakCanary está analizando la memoria en segundo plano."
        } catch (e: Throwable) {
            _actionMessage.value = "No se pudo solicitar el volcado: ${e.localizedMessage}"
        }
    }

    private val leakedObjectsHolder = mutableListOf<Any>()

    fun triggerTestLeak() {
        val dummyLeaked = object {
            val description = "Fuga simulada intencional creada el ${System.currentTimeMillis()}"
            val payload = ByteArray(512 * 1024) // 512 KB
        }
        leakedObjectsHolder.add(dummyLeaked)
        try {
            leakcanary.AppWatcher.objectWatcher.watch(
                dummyLeaked,
                "Fuga de prueba intencional para verificar LeakCanary en el dispositivo"
            )
            _actionMessage.value = "Fuga de prueba registrada en AppWatcher. Genera un volcado para ver la notificación."
        } catch (e: Throwable) {
            _actionMessage.value = "Error al registrar en AppWatcher: ${e.localizedMessage}"
        }
    }

    fun openLeaksApp(context: Context) {
        try {
            val intent = android.content.Intent().apply {
                setClassName(context.packageName, "leakcanary.internal.activity.LeakActivity")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            _actionMessage.value = "No se pudo abrir la app de Leaks: ${e.localizedMessage}"
        }
    }
}
