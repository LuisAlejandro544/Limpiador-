package com.example.scanner

import com.example.model.JunkCategoryType
import com.example.model.JunkFileItem
import com.example.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

data class CleanProgress(
    val currentFileName: String = "",
    val deletedCount: Int = 0,
    val totalToClean: Int = 0,
    val freedBytes: Long = 0L,
    val isFinished: Boolean = false
)

data class CleanSummary(
    val totalFreedBytes: Long,
    val totalDeletedFiles: Int,
    val shizukuTrimExecuted: Boolean,
    val shizukuMessage: String = ""
)

object StorageCleaner {

    fun cleanSelectedItems(
        itemsToClean: List<JunkFileItem>,
        useShizukuIfAvailable: Boolean
    ): Flow<CleanProgress> = flow {
        var deletedCount = 0
        var freedBytes = 0L
        val totalCount = itemsToClean.size

        for ((index, item) in itemsToClean.withIndex()) {
            emit(
                CleanProgress(
                    currentFileName = item.name,
                    deletedCount = deletedCount,
                    totalToClean = totalCount,
                    freedBytes = freedBytes,
                    isFinished = false
                )
            )

            try {
                val file = File(item.path)
                if (file.exists()) {
                    val size = if (file.isFile) file.length() else item.size
                    val deleted = if (file.isDirectory) {
                        file.deleteRecursively()
                    } else {
                        file.delete()
                    }

                    if (deleted) {
                        deletedCount++
                        freedBytes += size
                    }
                }
            } catch (e: Exception) {
                // Ignore individual unremovable files
            }
        }

        // Deep ADB Cache Trim with Shizuku if enabled
        var shizukuSuccess = false
        var shizukuFeedback = ""
        if (useShizukuIfAvailable) {
            emit(
                CleanProgress(
                    currentFileName = "Optimizando caché global con Shizuku (pm trim-caches)...",
                    deletedCount = deletedCount,
                    totalToClean = totalCount,
                    freedBytes = freedBytes,
                    isFinished = false
                )
            )
            val result = ShizukuHelper.trimSystemCaches()
            if (result.isSuccess) {
                shizukuSuccess = true
                shizukuFeedback = "Comando trim-caches ejecutado vía Shizuku."
            }
        }

        emit(
            CleanProgress(
                currentFileName = "Limpieza completada",
                deletedCount = deletedCount,
                totalToClean = totalCount,
                freedBytes = freedBytes,
                isFinished = true
            )
        )
    }.flowOn(Dispatchers.IO)
}
