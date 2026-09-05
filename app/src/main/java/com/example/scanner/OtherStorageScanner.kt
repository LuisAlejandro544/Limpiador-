package com.example.scanner

import android.content.Context
import android.os.Environment
import com.example.model.OtherStorageItem
import com.example.model.OtherStorageScanResult
import com.example.model.SafetyLevel
import com.example.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

data class OtherScanProgress(
    val statusMessage: String = "",
    val itemsFound: List<OtherStorageItem> = emptyList(),
    val isComplete: Boolean = false
)

object OtherStorageScanner {

    /**
     * Ejecuta el escaneo de almacenamiento "Otros" en un hilo secundario (Dispatchers.IO).
     * Emite actualizaciones en tiempo real para no congelar la interfaz de usuario.
     */
    fun scanOtherStorageFlow(context: Context): Flow<OtherScanProgress> = flow {
        emit(OtherScanProgress(statusMessage = "Iniciando análisis del almacenamiento 'Otros'...", isComplete = false))

        val isShizukuAvailable = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()
        val items = mutableListOf<OtherStorageItem>()

        if (isShizukuAvailable) {
            emit(OtherScanProgress(statusMessage = "Ejecutando script Shell con permisos Shizuku...", isComplete = false))
            val shellItems = scanWithShizukuShell(context)
            if (shellItems.isNotEmpty()) {
                items.addAll(shellItems)
                emit(OtherScanProgress(statusMessage = "Análisis Shell completado", itemsFound = items, isComplete = true))
                return@flow
            }
        }

        // Modo de escaneo directo en Dispatchers.IO (nativo / fallback)
        emit(OtherScanProgress(statusMessage = "Analizando miniaturas y cachés externas...", itemsFound = items))
        scanNativeDirectories(context) { item ->
            items.add(item)
        }

        emit(OtherScanProgress(
            statusMessage = "Escaneo finalizado. ${items.size} elementos clasificados en 'Otros'.",
            itemsFound = items.sortedByDescending { it.sizeBytes },
            isComplete = true
        ))
    }.flowOn(Dispatchers.IO)

    /**
     * Ejecuta el script scan_other_storage.sh vía Shizuku process
     */
    private suspend fun scanWithShizukuShell(context: Context): List<OtherStorageItem> = withContext(Dispatchers.IO) {
        val resultList = mutableListOf<OtherStorageItem>()
        try {
            // Leer el script de assets
            val scriptContent = context.assets.open("scripts/scan_other_storage.sh").bufferedReader().use { it.readText() }

            // Crear archivo temporal ejecutable en cache interno
            val tempScriptFile = File(context.cacheDir, "scan_other_storage.sh")
            tempScriptFile.writeText(scriptContent)
            tempScriptFile.setExecutable(true, false)

            // Ejecutar con Shizuku ADB shell
            val executionResult = ShizukuHelper.executeAdbCommand("sh ${tempScriptFile.absolutePath}")
            if (executionResult.isSuccess) {
                val output = executionResult.getOrDefault("")
                for (l in output.lines()) {
                    if (l.startsWith("ITEM|")) {
                        val parts = l.split("|")
                        if (parts.size >= 6) {
                            val size = parts[1].toLongOrNull() ?: 0L
                            val path = parts[2]
                            val category = parts[3]
                            val safetyStr = parts[4]
                            val description = parts[5]

                            val safety = when (safetyStr) {
                                "SAFE" -> SafetyLevel.SAFE
                                "CAUTION" -> SafetyLevel.CAUTION
                                else -> SafetyLevel.KEEP
                            }

                            val file = File(path)
                            resultList.add(
                                OtherStorageItem(
                                    path = path,
                                    name = file.name.ifEmpty { path },
                                    sizeBytes = size,
                                    category = category,
                                    safety = safety,
                                    description = description
                                )
                            )
                        }
                    }
                }
            }
            tempScriptFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        resultList
    }

    /**
     * Escaneo complementario nativo en segundo plano garantizando seguridad de datos
     */
    private fun scanNativeDirectories(context: Context, onItemFound: (OtherStorageItem) -> Unit) {
        val externalStorage = Environment.getExternalStorageDirectory() ?: return

        // 1. Miniaturas en DCIM/.thumbnails
        val thumbnailsDir = File(externalStorage, "DCIM/.thumbnails")
        if (thumbnailsDir.exists() && thumbnailsDir.isDirectory) {
            thumbnailsDir.listFiles()?.forEach { thumbFile ->
                val size = if (thumbFile.isDirectory) getFolderSize(thumbFile) else thumbFile.length()
                if (size > 0) {
                    onItemFound(
                        OtherStorageItem(
                            path = thumbFile.absolutePath,
                            name = thumbFile.name,
                            sizeBytes = size,
                            category = "Miniaturas del Sistema",
                            safety = SafetyLevel.SAFE,
                            description = "Base de datos o caché de miniaturas de la galería. Es 100% seguro de borrar."
                        )
                    )
                }
            }
        }

        // 2. Caché de aplicaciones en Android/data/*/cache
        val dataDir = File(externalStorage, "Android/data")
        if (dataDir.exists() && dataDir.isDirectory) {
            dataDir.listFiles()?.forEach { pkgDir ->
                if (pkgDir.isDirectory) {
                    val cacheFolder = File(pkgDir, "cache")
                    if (cacheFolder.exists() && cacheFolder.isDirectory) {
                        val size = getFolderSize(cacheFolder)
                        if (size > 10 * 1024) {
                            onItemFound(
                                OtherStorageItem(
                                    path = cacheFolder.absolutePath,
                                    name = "${pkgDir.name}/cache",
                                    sizeBytes = size,
                                    category = "Caché de Aplicaciones",
                                    safety = SafetyLevel.SAFE,
                                    description = "Caché temporal no indexada en la memoria externa."
                                )
                            )
                        }
                    }
                }
            }
        }

        // 3. Temporales huérfanos (.tmp, .crdownload, .part, .log)
        val tempExtensions = setOf("tmp", "crdownload", "part", "dmp", "log", "bak")
        scanDirForExtensions(externalStorage, tempExtensions, maxDepth = 4) { tempFile ->
            val size = tempFile.length()
            if (size > 0) {
                onItemFound(
                    OtherStorageItem(
                        path = tempFile.absolutePath,
                        name = tempFile.name,
                        sizeBytes = size,
                        category = "Temporales y Descargas Huérfanas",
                        safety = SafetyLevel.SAFE,
                        description = "Descarga incompleta o archivo de registro temporal."
                    )
                )
            }
        }

        // 4. Copias de seguridad antiguas de mensajería (PRECAUCIÓN)
        val mediaDir = File(externalStorage, "Android/media")
        if (mediaDir.exists()) {
            mediaDir.walkTopDown().maxDepth(5).filter {
                it.isFile && it.name.startsWith("msgstore-") && it.name.contains(".db.crypt")
            }.forEach { backupFile ->
                onItemFound(
                    OtherStorageItem(
                        path = backupFile.absolutePath,
                        name = backupFile.name,
                        sizeBytes = backupFile.length(),
                        category = "Respaldos Históricos",
                        safety = SafetyLevel.CAUTION,
                        description = "Copia de seguridad local antigua de chats. Conserva el más reciente y borra los antiguos si necesitas espacio."
                    )
                )
            }
        }

        // 5. Archivos pesados desconocidos (> 25 MB)
        val downloadDir = File(externalStorage, "Download")
        if (downloadDir.exists()) {
            downloadDir.listFiles()?.filter { it.isFile && it.length() > 25 * 1024 * 1024 }?.forEach { bigFile ->
                val ext = bigFile.extension.lowercase()
                if (ext !in setOf("mp4", "mkv", "mp3", "m4a", "jpg", "png", "pdf", "zip", "apk")) {
                    onItemFound(
                        OtherStorageItem(
                            path = bigFile.absolutePath,
                            name = bigFile.name,
                            sizeBytes = bigFile.length(),
                            category = "Archivos Desconocidos",
                            safety = SafetyLevel.CAUTION,
                            description = "Archivo grande sin extensión multimedia estándar que suma a 'Otros'."
                        )
                    )
                }
            }
        }

        // 6. Datos de Juegos OBB (NO TOCAR / KEEP)
        val obbDir = File(externalStorage, "Android/obb")
        if (obbDir.exists() && obbDir.isDirectory) {
            obbDir.listFiles()?.forEach { obbPkg ->
                if (obbPkg.isDirectory) {
                    val obbSize = getFolderSize(obbPkg)
                    if (obbSize > 0) {
                        onItemFound(
                            OtherStorageItem(
                                path = obbPkg.absolutePath,
                                name = obbPkg.name,
                                sizeBytes = obbSize,
                                category = "Datos de Juegos (OBB)",
                                safety = SafetyLevel.KEEP,
                                description = "Archivos esenciales de juego. NO deben eliminarse para evitar perder datos o descargas."
                            )
                        )
                    }
                }
            }
        }
    }

    private fun scanDirForExtensions(dir: File, exts: Set<String>, maxDepth: Int, onFile: (File) -> Unit) {
        if (!dir.exists() || !dir.canRead()) return
        dir.walkTopDown().maxDepth(maxDepth).filter { file ->
            file.isFile && file.extension.lowercase() in exts
        }.forEach { onFile(it) }
    }

    private fun getFolderSize(folder: File): Long {
        var length = 0L
        val files = folder.listFiles() ?: return 0L
        for (file in files) {
            length += if (file.isFile) file.length() else getFolderSize(file)
        }
        return length
    }

    /**
     * Elimina de forma segura los ítems seleccionados por el usuario
     * en un hilo secundario utilizando Shizuku (rm -rf) o File IO según disponibilidad.
     */
    suspend fun deleteSelectedItems(items: List<OtherStorageItem>): Result<Long> = withContext(Dispatchers.IO) {
        var bytesFreed = 0L
        val isShizukuAvailable = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()

        for (item in items) {
            if (!item.isSelected || item.safety == SafetyLevel.KEEP) {
                continue // Proteger siempre los ítems KEEP
            }

            try {
                if (isShizukuAvailable) {
                    // Eliminación privilegiada vía ADB shell
                    val escapedPath = item.path.replace(" ", "\\ ")
                    ShizukuHelper.executeAdbCommand("rm -rf $escapedPath")
                    bytesFreed += item.sizeBytes
                } else {
                    val file = File(item.path)
                    val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                    if (success) {
                        bytesFreed += item.sizeBytes
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Result.success(bytesFreed)
    }
}
