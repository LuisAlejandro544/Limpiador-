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
        // 1. Si el Binder ya responde, está instalado y activo sin lugar a dudas
        if (isAvailable()) return true

        // 2. Verificación directa de paquete por PackageInfo
        try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            return true
        } catch (e: Throwable) {
            // Ignorado para intentar método secundario
        }

        // 3. Verificación mediante Intent de lanzamiento
        try {
            if (context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null) {
                return true
            }
        } catch (e: Throwable) {
            // Ignorado
        }

        // 4. Verificación de ContentProvider o Intent queries
        return try {
            val intent = android.content.Intent("moe.shizuku.manager.action.START").apply {
                setPackage(SHIZUKU_PACKAGE)
            }
            val resolveInfo = context.packageManager.queryIntentActivities(intent, 0)
            resolveInfo.isNotEmpty()
        } catch (e: Throwable) {
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

    fun openShizukuApp(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            false
        }
    }

    fun getShizukuInfo(context: Context): ShizukuInfo {
        // Prioridad 1: Si el Binder de Shizuku/Sui responde activamente
        val isPingAlive = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }

        if (isPingAlive) {
            val hasPermission = try {
                if (Shizuku.isPreV11()) {
                    false
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
            } catch (e: Throwable) {
                false
            }

            return if (hasPermission) {
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
                    message = "Servicio Shizuku activo en el dispositivo. Pulsa 'Autorizar' para conceder permisos ADB a la app."
                )
            }
        }

        // Prioridad 2: Si el Binder no responde, verificar si la app gestora está en el móvil
        val isInstalled = isShizukuInstalled(context)
        if (isInstalled) {
            return ShizukuInfo(
                status = ShizukuStatus.SERVICE_STOPPED,
                message = "Servicio Shizuku inactivo. Abre la app Shizuku e inícialo mediante Depuración Inalámbrica en este móvil o vía ROOT."
            )
        }

        // Prioridad 3: Ni Binder ni app detectados
        return ShizukuInfo(
            status = ShizukuStatus.NOT_INSTALLED,
            message = "Shizuku no está instalado. Puedes descargarlo de GitHub o Uptodown para desbloquear limpieza de nivel ADB."
        )
    }

    fun requestPermission(context: Context? = null) {
        try {
            if (Shizuku.pingBinder()) {
                if (!Shizuku.isPreV11()) {
                    Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                }
            } else if (context != null) {
                openShizukuApp(context)
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
