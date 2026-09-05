# Contexto de IA (`AI_context.md`)

Este documento sirve como contexto de conocimiento estructurado para cualquier modelo de Inteligencia Artificial o asistente que colabore en el mantenimiento y evolución de esta base de código.

---

## 🎯 Propósito del Proyecto

El proyecto es una aplicación nativa de Android desarrollada en **Kotlin** y **Jetpack Compose** orientada a la **auditoría profunda, diagnóstico y limpieza del almacenamiento interno del teléfono**, con un enfoque primordial en la categoría conocida como **«Otros»** (que suele acumular entre 10 GB y 40 GB de archivos residuales que el sistema operativo no desglosa al usuario común).

---

## 📱 Perfil del Usuario y Restricciones de Entorno

1. **Plataforma del Usuario**: El usuario opera exclusivamente desde un **teléfono móvil Android** (no dispone de PC para comandos ADB por cable). Por ello:
   - Toda la gestión privilegiada de permisos y ejecución de comandos debe funcionar mediante **Shizuku** (que se activa en el móvil vía Depuración Inalámbrica de Android).
   - Siempre debe existir un modo de respaldo nativo si Shizuku no está presente o activo.
2. **Canal de Distribución**: La aplicación se compilará como APK independiente y se distribuirá en plataformas abiertas como **Uptodown** o repositorios de APKs de terceros, **NO en Google Play**.
   - Por tanto, no aplican restricciones artificiales de Google Play sobre permisos de almacenamiento (`MANAGE_EXTERNAL_STORAGE` o uso de `IPackageManager`).
   - Se prioriza la máxima potencia y funcionalidad real sobre políticas restrictivas de tiendas comerciales.
3. **Tamaño del APK**: Al usuario **no le preocupa el peso final del APK**, siempre y cuando las dependencias sean 100% funcionales, estables y robustas. Se prefieren bibliotecas probadas sobre soluciones artesanales sin dependencias.
4. **Propiedad Intelectual**: Está estrictamente prohibido nombrar archivos, paquetes o clases con marcas registradas o protegidas por derechos de autor que puedan comprometer al usuario.
5. **Comportamiento del Sistema**: En caso de implementar características de aceleración o modo juego en el futuro, **NUNCA** modificar ni usar propiedades del tipo `persist.sys.*`.

---

## 🔍 ¿Qué es la categoría «Otros» en Android y por qué crece tanto?

En los ajustes de almacenamiento de Android (MIUI, One UI, ColorOS, Pixel OS, etc.), el sistema clasifica fotos, videos, audio y aplicaciones. Todo archivo que no encaja en esas cuatro definiciones MIME estándar cae en **«Otros»**:

1. **Miniaturas de Galería (`.thumbnails`)**: Cada imagen o video genera miniaturas en `/sdcard/DCIM/.thumbnails/`. Con el tiempo, este directorio puede contener cientos de miles de miniaturas huérfanas de fotos ya borradas, alcanzando entre 3 GB y 10 GB.
2. **Cachés de Apps de Mensajería**: Clientes como Telegram y WhatsApp guardan audios, stickers cacheados y fragmentos multimedia en sus propios directorios (`Telegram/Telegram Documents`, `Android/media/com.whatsapp/`).
3. **Residuos de Aplicaciones Desinstaladas**: Cuando el usuario desinstala un juego o app pesada, las carpetas con datos adicionales en `/sdcard/Android/data/` o `/sdcard/Android/obb/` muchas veces no son purgadas por el sistema.
4. **Archivos Temporales y Descargas Huérfanas**: Archivos `.tmp`, `.apk` viejos en la carpeta de descargas, logs del sistema (`.log`, `.dump`, `tombstones`).

---

## ⚙️ Arquitectura Técnica

### 1. Suite de Scripts Shell Autónomos (`app/src/main/assets/scripts/`)
- **`scan_other_storage.sh`**:
  - Se extrae al directorio de caché interno de la app al momento del escaneo.
  - Se ejecuta mediante `ShizukuHelper.executeAdbCommand("sh ...")` si Shizuku está disponible, o mediante el runtime de Java como fallback.
  - Emite líneas estructuradas: `ITEM|<tamano_bytes>|<ruta_absoluta>|<categoria>|<nivel_seguridad>|<descripcion>`.
  - Niveles de seguridad asignados:
    - `SAFE`: Eliminar sin riesgo alguno (miniaturas, `.tmp`, `.log`, cachés de descarga).
    - `CAUTION`: Analizar antes de borrar (carpetas de apps desinstaladas, descargas antiguas).
    - `KEEP`: Archivos protegidos que no deben borrarse jamás (`.obb`, bases de datos SQLite `.db`).
- **`clean_orphaned_packages.sh`**:
  - Obtiene la lista activa de paquetes (`pm list packages`) y rastrea `/sdcard/Android/data` y `/sdcard/Android/obb`.
  - Reporta y/o purga carpetas residuales de aplicaciones desinstaladas que Android no eliminó.
- **`purge_system_logs_and_dumps.sh`**:
  - Limpia archivos de depuración en `/data/local/tmp`, volcados de caída (*tombstones*, *dropbox*) y registros `.log`/`.dmp`.
- **`trim_art_cache.sh`**:
  - Lanza `pm trim-caches 999999999999999` para que el framework libere espacio de caché sin tocar propiedades `persist.sys.*`.

### 2. Capa de Integración Shizuku (`ShizukuHelper.kt`)
- Utiliza la API oficial de Shizuku para comunicarse con el proceso privilegiado (UID 2000 / UID 0).
- Obtiene la interfaz `IPackageManager` del sistema mediante `ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))`.
- Permite invocar:
  - `grantRuntimePermission(...)`: Otorga permisos de almacenamiento a la propia app sin interacción manual.
  - `pm trim-caches 999999999999999`: Solicita al sistema operativo recortar el almacenamiento de caché de todas las aplicaciones instaladas.
  - `newProcess(...)`: Ejecuta comandos shell como usuario `shell`/`adb`.

### 3. Pipeline de Automatización y Compilación en la Nube (`build-debug-apk.yml`)
- Workflow de GitHub Actions ubicado en `.github/workflows/build-debug-apk.yml`.
- Configurado intencionadamente **sin caché** (`cache: ''`, `--no-build-cache`, `--no-daemon`) para asegurar compilaciones limpias desde cero.
- Auto-genera el keystore de depuración estándar de Android en el entorno runner (`keytool -genkey ... ~/.android/debug.keystore`).
- Genera el APK Debug como artefacto descargable directamente desde el móvil a través del navegador o la app de GitHub.

### 3. Hilos y Reactividad (Concurrencia)
- **Regla de Oro**: Ninguna operación de lectura de disco, cálculo de tamaño o ejecución de scripts debe correr en el hilo principal (`Main Thread`).
- Todo el escaneo se ejecuta en `Dispatchers.IO` dentro de corrutinas gestionadas por `viewModelScope`.
- Se usa `Kotlin Flow` para emitir el progreso de forma reactiva, permitiendo que la UI muestre en tiempo real el directorio que se está escaneando sin tartamudeos ni congelamientos.
