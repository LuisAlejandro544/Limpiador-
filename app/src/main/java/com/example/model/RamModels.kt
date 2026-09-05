package com.example.model

enum class ProcessCategory(val label: String) {
    HEAVY_LEAK("Fuga / Consumo Crítico"),
    CACHED("Proceso en Caché"),
    BACKGROUND("Servicio en 2do Plano"),
    SYSTEM("Sistema / Protegido")
}

data class RamStats(
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val cachedBytes: Long = 0L,
    val zramBytes: Long = 0L
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedTotal: String get() = formatStorageSize(totalBytes)
    val formattedUsed: String get() = formatStorageSize(usedBytes)
    val formattedAvailable: String get() = formatStorageSize(availableBytes)
    val formattedCached: String get() = formatStorageSize(cachedBytes)
    val formattedZram: String get() = formatStorageSize(zramBytes)
}

data class RamProcessItem(
    val pid: Int,
    val packageName: String,
    val appName: String,
    val pssBytes: Long,
    val category: ProcessCategory,
    val isLeakCandidate: Boolean,
    val isSelected: Boolean = false
) {
    val formattedPss: String get() = formatStorageSize(pssBytes)
}

data class RamScanResult(
    val stats: RamStats = RamStats(),
    val processes: List<RamProcessItem> = emptyList(),
    val isScanning: Boolean = false,
    val statusMessage: String = ""
)

data class RamTrimResult(
    val freedBytes: Long,
    val procsOptimized: Int,
    val details: String
)
