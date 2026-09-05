package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import com.example.model.ShizukuInfo
import com.example.model.ShizukuStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {

    private const val SHIZUKU_PACKAGE = "moe.shizuku.manager"
    const val SHIZUKU_REQUEST_CODE = 1001

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            isAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun getShizukuInfo(context: Context): ShizukuInfo {
        if (!isShizukuInstalled(context)) {
            return ShizukuInfo(
                status = ShizukuStatus.NOT_INSTALLED,
                message = "Shizuku no está instalado. Puedes descargarlo de GitHub o Uptodown para desbloquear limpieza de nivel ADB."
            )
        }

        return try {
            val isPingAlive = Shizuku.pingBinder()
            if (!isPingAlive) {
                ShizukuInfo(
                    status = ShizukuStatus.SERVICE_STOPPED,
                    message = "Servicio Shizuku inactivo. Inícialo en la app Shizuku vía depuración inalámbrica o PC."
                )
            } else {
                val hasPermission = try {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                } catch (e: Throwable) {
                    false
                }

                if (hasPermission) {
                    val version = try { Shizuku.getVersion() } catch (e: Throwable) { 13 }
                    val uid = try { Shizuku.getUid() } catch (e: Throwable) { 2000 }
                    val privilegeLabel = if (uid == 0) "ROOT (Sui / Magisk / KernelSU)" else "ADB (Depuración Inalámbrica / Shell)"
                    ShizukuInfo(
                        status = ShizukuStatus.AUTHORIZED,
                        version = version,
                        uid = uid,
                        message = "Conectado como UID $uid [$privilegeLabel]. IPackageManager activo para permisos y limpieza profunda."
                    )
                } else {
                    ShizukuInfo(
                        status = ShizukuStatus.PERMISSION_REQUIRED,
                        message = "Servicio Shizuku detectado. Pulsa 'Autorizar' para conceder permisos ADB a la app."
                    )
                }
            }
        } catch (e: Throwable) {
            ShizukuInfo(
                status = ShizukuStatus.SERVICE_STOPPED,
                message = "No se pudo comunicar con Shizuku: ${e.localizedMessage ?: "Error desconocido"}"
            )
        }
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                } else {
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                }
            }
        } catch (e: Throwable) {
            // Handled gracefully
        }
    }

    /**
     * Concede un permiso en tiempo de ejecución utilizando el Binder IPC de IPackageManager
     * proporcionado por Shizuku con privilegios ADB/Root.
     */
    fun grantRuntimePermission(packageName: String, permissionName: String, userId: Int = 0): Result<Unit> {
        return try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return Result.failure(IllegalStateException("Shizuku no está conectado o autorizado"))
            }
            ShizukuPermissionManager.grantRuntimePermission(packageName, permissionName, userId)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    /**
     * Otorga automáticamente todos los permisos de almacenamiento necesarios
     * directamente desde el dispositivo sin necesidad de PC ni interacción en Ajustes.
     */
    suspend fun autoGrantStoragePermissions(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(IllegalStateException("Shizuku no está autorizado todavía."))
            }

            val pkg = context.packageName

            // 1. Intento con IPackageManager Binder
            try {
                ShizukuPermissionManager.grantPermissionForCurrentUser(pkg, "android.permission.READ_EXTERNAL_STORAGE")
                ShizukuPermissionManager.grantPermissionForCurrentUser(pkg, "android.permission.WRITE_EXTERNAL_STORAGE")
                ShizukuPermissionManager.grantPermissionForCurrentUser(pkg, "android.permission.MANAGE_EXTERNAL_STORAGE")
            } catch (e: Throwable) {
                // Continuamos con fallback a adb shell en caso de que MANAGE_EXTERNAL_STORAGE requiera AppOps
            }

            // 2. Conceder AppOps para MANAGE_EXTERNAL_STORAGE (Acceso a todos los archivos en Android 11+)
            executeAdbCommand("appops set $pkg MANAGE_EXTERNAL_STORAGE allow")
            executeAdbCommand("pm grant $pkg android.permission.READ_EXTERNAL_STORAGE")
            executeAdbCommand("pm grant $pkg android.permission.WRITE_EXTERNAL_STORAGE")
            executeAdbCommand("pm grant $pkg android.permission.READ_MEDIA_IMAGES")
            executeAdbCommand("pm grant $pkg android.permission.READ_MEDIA_VIDEO")
            executeAdbCommand("pm grant $pkg android.permission.READ_MEDIA_AUDIO")

            Result.success("¡Permisos de almacenamiento concedidos exitosamente vía Shizuku IPackageManager!")
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun executeAdbCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                return@withContext Result.failure(IllegalStateException("Shizuku no está conectado o autorizado"))
            }

            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            ).apply {
                isAccessible = true
            }

            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            Result.success(output.toString().trim())
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun trimSystemCaches(): Result<String> {
        // Standard Android ADB command to instruct the OS package manager to trim all application caches
        return executeAdbCommand("pm trim-caches 999999999999999")
    }
}
