package com.example.shizuku;

import android.content.pm.IPackageManager;
import android.os.Process;
import android.os.RemoteException;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.SystemServiceHelper;

/**
 * Gestor de permisos y llamadas al sistema Android utilizando Shizuku.
 * Permite otorgar permisos en tiempo de ejecución a nivel ADB (UID 2000) o ROOT (UID 0)
 * sin requerir interacción manual del usuario en la pantalla de Ajustes.
 */
public class ShizukuPermissionManager {

    private static IPackageManager packageManagerInstance = null;

    private static synchronized IPackageManager getPackageManager() {
        if (packageManagerInstance == null) {
            packageManagerInstance = IPackageManager.Stub.asInterface(
                new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            );
        }
        return packageManagerInstance;
    }

    /**
     * Código oficial para otorgar permisos en tiempo de ejecución a través del Binder de Shizuku.
     */
    public static void grantRuntimePermission(String packageName, String permissionName, int userId) {
        try {
            getPackageManager().grantRuntimePermission(packageName, permissionName, userId);
        } catch (RemoteException tr) {
            throw new RuntimeException(tr.getMessage(), tr);
        }
    }

    /**
     * Otorga un permiso en tiempo de ejecución para el usuario actual (User 0 / MyUser).
     */
    public static void grantPermissionForCurrentUser(String packageName, String permissionName) {
        int myUserId = Process.myUid() / 100000;
        grantRuntimePermission(packageName, permissionName, myUserId);
    }
}
