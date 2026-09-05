package com.example.scanner

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.model.JunkCategoryGroup
import com.example.model.JunkCategoryType
import com.example.model.JunkFileItem
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class ScanProgress(
    val currentPath: String = "",
    val scannedFilesCount: Int = 0,
    val isFinished: Boolean = false,
    val results: Map<JunkCategoryType, List<JunkFileItem>> = emptyMap()
)

object StorageScanner {

    fun getStorageStats(): StorageStats {
        return try {
            val internalPath = Environment.getDataDirectory()
            val stat = StatFs(internalPath.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val total = totalBlocks * blockSize
            val free = availableBlocks * blockSize
            val used = (total - free).coerceAtLeast(0L)

            StorageStats(
                totalBytes = total,
                freeBytes = free,
                usedBytes = used
            )
        } catch (e: Exception) {
            StorageStats(
                totalBytes = 64L * 1024 * 1024 * 1024,
                freeBytes = 24L * 1024 * 1024 * 1024,
                usedBytes = 40L * 1024 * 1024 * 1024
            )
        }
    }

    fun scanStorage(context: Context): Flow<ScanProgress> = flow {
        val appCacheItems = mutableListOf<JunkFileItem>()
        val tempLogItems = mutableListOf<JunkFileItem>()
        val emptyFolderItems = mutableListOf<JunkFileItem>()
        val thumbnailItems = mutableListOf<JunkFileItem>()
        val obsoleteApkItems = mutableListOf<JunkFileItem>()
        val largeFileItems = mutableListOf<JunkFileItem>()

        var totalScanned = 0

        // 1. Scan App Caches
        val cacheDirs = listOfNotNull(
            context.cacheDir,
            context.codeCacheDir,
            *context.externalCacheDirs.filterNotNull().toTypedArray()
        )

        for (cDir in cacheDirs) {
            if (cDir.exists()) {
                cDir.walkTopDown().maxDepth(4).forEach { file ->
                    totalScanned++
                    if (totalScanned % 15 == 0) {
                        emit(
                            ScanProgress(
                                currentPath = file.name,
                                scannedFilesCount = totalScanned,
                                isFinished = false
                            )
                        )
                    }
                    if (file.isFile && file.length() > 0) {
                        appCacheItems.add(
                            JunkFileItem(
                                name = file.name,
                                path = file.absolutePath,
                                size = file.length(),
                                type = JunkCategoryType.APP_CACHE
                            )
                        )
                    }
                }
            }
        }

        // 2. Scan Storage roots (Internal External Storage & Download/Documents)
        val rootsToScan = mutableListOf<File>()
        try {
            val extStorage = Environment.getExternalStorageDirectory()
            if (extStorage != null && extStorage.exists() && extStorage.canRead()) {
                rootsToScan.add(extStorage)
            }
        } catch (e: Exception) {
            // Handled
        }

        // Common folders where junk accumulates
        val targets = listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES,
            Environment.DIRECTORY_DOCUMENTS,
            Environment.DIRECTORY_MOVIES,
            Environment.DIRECTORY_DCIM
        )

        for (folder in targets) {
            try {
                val dir = Environment.getExternalStoragePublicDirectory(folder)
                if (dir != null && dir.exists() && !rootsToScan.contains(dir)) {
                    rootsToScan.add(dir)
                }
            } catch (e: Exception) {
                // Handled
            }
        }

        val visitedPaths = mutableSetOf<String>()

        for (rootDir in rootsToScan) {
            try {
                rootDir.walkTopDown()
                    .maxDepth(5)
                    .onEnter { dir ->
                        // Skip sensitive or inaccessible system directories
                        val name = dir.name.lowercase()
                        !name.startsWith(".") && name != "android" || name == ".thumbnails"
                    }
                    .forEach { file ->
                        if (!visitedPaths.add(file.absolutePath)) return@forEach

                        totalScanned++
                        if (totalScanned % 20 == 0) {
                            emit(
                                ScanProgress(
                                    currentPath = file.name,
                                    scannedFilesCount = totalScanned,
                                    isFinished = false
                                )
                            )
                        }

                        if (file.isDirectory) {
                            // Check for empty folders
                            val children = file.list()
                            if (children != null && children.isEmpty()) {
                                emptyFolderItems.add(
                                    JunkFileItem(
                                        name = file.name.ifEmpty { "Carpeta vacía" },
                                        path = file.absolutePath,
                                        size = 4096L, // Directory entry size estimate
                                        type = JunkCategoryType.EMPTY_FOLDERS
                                    )
                                )
                            }
                        } else if (file.isFile) {
                            val lowerName = file.name.lowercase()
                            val fileSize = file.length()

                            when {
                                // Thumbnails
                                file.parent?.contains(".thumbnails", ignoreCase = true) == true ||
                                        lowerName.endsWith(".thumb") || lowerName.endsWith(".thumbnails") -> {
                                    thumbnailItems.add(
                                        JunkFileItem(
                                            name = file.name,
                                            path = file.absolutePath,
                                            size = fileSize,
                                            type = JunkCategoryType.THUMBNAILS
                                        )
                                    )
                                }

                                // Obsolete APKs in downloads/storage
                                lowerName.endsWith(".apk") -> {
                                    obsoleteApkItems.add(
                                        JunkFileItem(
                                            name = file.name,
                                            path = file.absolutePath,
                                            size = fileSize,
                                            type = JunkCategoryType.OBSOLETE_APKS
                                        )
                                    )
                                }

                                // Temporaries and Logs
                                lowerName.endsWith(".tmp") || lowerName.endsWith(".temp") ||
                                        lowerName.endsWith(".log") || lowerName.endsWith(".bak") ||
                                        lowerName.endsWith(".old") || lowerName.contains("crash") -> {
                                    tempLogItems.add(
                                        JunkFileItem(
                                            name = file.name,
                                            path = file.absolutePath,
                                            size = fileSize,
                                            type = JunkCategoryType.TEMP_AND_LOGS
                                        )
                                    )
                                }

                                // Large Files (> 50 MB)
                                fileSize >= 50L * 1024 * 1024 -> {
                                    largeFileItems.add(
                                        JunkFileItem(
                                            name = file.name,
                                            path = file.absolutePath,
                                            size = fileSize,
                                            type = JunkCategoryType.LARGE_FILES,
                                            isSelected = false // Large files unchecked by default for safety
                                        )
                                    )
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                // Ignore permission denial on specific subfolders
            }
        }

        // Final emit with complete results
        val resultMap = mapOf(
            JunkCategoryType.APP_CACHE to appCacheItems,
            JunkCategoryType.TEMP_AND_LOGS to tempLogItems,
            JunkCategoryType.EMPTY_FOLDERS to emptyFolderItems,
            JunkCategoryType.THUMBNAILS to thumbnailItems,
            JunkCategoryType.OBSOLETE_APKS to obsoleteApkItems,
            JunkCategoryType.LARGE_FILES to largeFileItems
        )

        emit(
            ScanProgress(
                currentPath = "Escaneo completado",
                scannedFilesCount = totalScanned,
                isFinished = true,
                results = resultMap
            )
        )
    }.flowOn(Dispatchers.IO)
}
