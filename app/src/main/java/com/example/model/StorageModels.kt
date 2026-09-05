package com.example.model

import java.text.DecimalFormat

enum class JunkCategoryType(
    val title: String,
    val description: String,
    val isSafeToDeleteByDefault: Boolean
) {
    APP_CACHE(
        title = "Caché de Aplicaciones",
        description = "Datos temporales creados por apps para agilizar su carga",
        isSafeToDeleteByDefault = true
    ),
    TEMP_AND_LOGS(
        title = "Temporales y Logs",
        description = "Archivos .tmp, .log, volcados de error y respaldos residuales",
        isSafeToDeleteByDefault = true
    ),
    EMPTY_FOLDERS(
        title = "Carpetas Huérfanas y Vacías",
        description = "Directorios sin contenido dejados por aplicaciones desinstaladas",
        isSafeToDeleteByDefault = true
    ),
    THUMBNAILS(
        title = "Miniaturas de Medios",
        description = "Caché de miniaturas de galería y álbumes del sistema",
        isSafeToDeleteByDefault = true
    ),
    OBSOLETE_APKS(
        title = "Instaladores APK Residuales",
        description = "Archivos .apk descargados que ya fueron instalados o no se usan",
        isSafeToDeleteByDefault = true
    ),
    LARGE_FILES(
        title = "Archivos Grandes (>50 MB)",
        description = "Archivos pesados identificados para revisión manual antes de borrar",
        isSafeToDeleteByDefault = false
    )
}

data class JunkFileItem(
    val name: String,
    val path: String,
    val size: Long,
    val type: JunkCategoryType,
    val isSelected: Boolean = true
)

data class JunkCategoryGroup(
    val type: JunkCategoryType,
    val items: List<JunkFileItem> = emptyList(),
    val isSelected: Boolean = type.isSafeToDeleteByDefault,
    val isExpanded: Boolean = false
) {
    val totalSize: Long get() = items.sumOf { it.size }
    val itemCount: Int get() = items.size
    val selectedSize: Long get() = items.filter { it.isSelected }.sumOf { it.size }
}

enum class ShizukuStatus(val label: String) {
    NOT_INSTALLED("No Instalado"),
    SERVICE_STOPPED("Servicio Detenido"),
    PERMISSION_REQUIRED("Permiso Requerido"),
    AUTHORIZED("Activo (Nivel ADB)")
}

data class ShizukuInfo(
    val status: ShizukuStatus = ShizukuStatus.SERVICE_STOPPED,
    val version: Int = 0,
    val uid: Int = -1,
    val message: String = ""
)

data class StorageStats(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val junkFoundBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) ((usedBytes.toFloat() / totalBytes.toFloat()) * 100f).coerceIn(0f, 100f) else 0f
}

enum class SafetyLevel(
    val label: String,
    val description: String
) {
    SAFE("Seguro de Eliminar", "Basura real, cachés redundantes y temporales que el sistema regenera."),
    CAUTION("Precaución", "Archivos desconocidos o respaldos antiguos. Se recomienda revisar."),
    KEEP("Conservar / Vital", "Datos de juegos y aplicaciones esenciales. NO eliminar.")
}

data class OtherStorageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val category: String,
    val safety: SafetyLevel,
    val description: String,
    val isSelected: Boolean = (safety == SafetyLevel.SAFE)
)

data class OtherStorageScanResult(
    val items: List<OtherStorageItem> = emptyList(),
    val isScanning: Boolean = false,
    val executionLog: String = ""
) {
    val totalSafeBytes: Long get() = items.filter { it.safety == SafetyLevel.SAFE }.sumOf { it.sizeBytes }
    val totalCautionBytes: Long get() = items.filter { it.safety == SafetyLevel.CAUTION }.sumOf { it.sizeBytes }
    val totalKeepBytes: Long get() = items.filter { it.safety == SafetyLevel.KEEP }.sumOf { it.sizeBytes }
    val selectedBytes: Long get() = items.filter { it.isSelected }.sumOf { it.sizeBytes }
}

fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val df = DecimalFormat("#,##0.#")
    return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
