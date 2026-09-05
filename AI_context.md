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
3. **Residuos de Aplicaciones Desinstaladas y Bases de Datos Huérfanas**: Cuando el usuario desinstala un juego o app pesada, las carpetas con datos adicionales en `/sdcard/Android/data/` o `/sdcard/Android/obb/` muchas veces no son purgadas por el sistema. Además, quedan **bases de datos SQLite huérfanas** (`.db`, `.sqlite`) y **fragmentos de transacciones corruptos o zombis** (`.db-wal`, `.db-shm`, `.db-journal`) que ocupan almacenamiento persistentemente.
4. **Archivos Temporales y Descargas Huérfanas**: Archivos `.tmp`, `.apk` viejos en la carpeta de descargas, logs del sistema (`.log`, `.dump`, `tombstones`).

---

## ⚙️ Arquitectura Técnica

### 1. Suite de Scripts Shell Autónomos (`app/src/main/assets/scripts/`)
- **`trim_ram_and_leaks.sh`**:
  - Script shell para optimización profunda y quirúrgica de la memoria RAM del teléfono sin reinicios.
  - Ejecuta `am trim-memory <paquete> COMPLETE` sobre los paquetes de usuario y aplicaciones en segundo plano para forzar la recolección de basura de la máquina virtual Android y liberar búferes de memoria.
  - Ejecuta `am kill-all` para limpiar procesos muertos en caché de forma transparente.
  - Audita el estado de `/proc/meminfo` antes y después de la operación para certificar los megabytes reales devueltos al sistema operativo.
- **`scan_other_storage.sh`**:
  - Se extrae al directorio de caché interno de la app al momento del escaneo.
  - Se ejecuta mediante `ShizukuHelper.executeAdbCommand("sh ...")` si Shizuku está disponible, o mediante el runtime de Java como fallback.
  - Emite líneas estructuradas: `ITEM|<tamano_bytes>|<ruta_absoluta>|<categoria>|<nivel_seguridad>|<descripcion>`.
  - Detección especializada de **Bases de Datos Huérfanas**: Compara paquetes instalados (`pm list packages`) con carpetas en `Android/data/` y localiza bases de datos SQLite abandonadas, además de rastrear fragmentos de transacciones WAL/SHM que quedaron huérfanos sin su base de datos principal.
  - Niveles de seguridad asignados:
    - `SAFE`: Eliminar sin riesgo alguno (miniaturas, `.tmp`, `.log`, cachés de descarga, bases de datos residuales de apps desinstaladas, fragmentos WAL/SHM huérfanos, procesos de RAM en caché).
    - `CAUTION`: Analizar antes de borrar (carpetas completas sin clasificar, descargas antiguas, servicios en segundo plano).
    - `KEEP`: Archivos protegidos que no deben borrarse jamás (`.obb` de apps activas, bases de datos SQLite en uso activo, procesos vitales del sistema).
- **`clean_orphaned_packages.sh`**:
  - Obtiene la lista activa de paquetes (`pm list packages`) y rastrea `/sdcard/Android/data` y `/sdcard/Android/obb`.
  - Reporta y/o purga carpetas residuales y bases de datos SQLite asociadas a aplicaciones desinstaladas que Android no eliminó.
- **`purge_system_logs_and_dumps.sh`**:
  - Limpia archivos de depuración en `/data/local/tmp`, volcados de caída (*tombstones*, *dropbox*) y registros `.log`/`.dmp`.
- **`trim_art_cache.sh`**:
  - Lanza `pm trim-caches 999999999999999` para que el framework libere espacio de caché sin tocar propiedades `persist.sys.*`.

### 2. Navegación e Interfaces Especializadas
- **`CleanScreen.kt` (Dashboard Principal)**: Resumen general del almacenamiento en tiempo real, estado de Shizuku, tarjeta de acceso a la herramienta de «Otros» y tarjeta de acceso a la herramienta de «Limpieza Quirúrgica de RAM».
- **`OtherStorageScreen.kt` (Interfaz 100% Independiente para «Otros»)**: Pantalla dedicada con barra de navegación superior, soporte nativo de retroceso (`BackHandler`), métricas de espacio liberable, chips de filtrado por nivel de riesgo y categoría, listado pormenorizado y botón de borrado masivo con confirmación.
- **`RamCleanScreen.kt` (Interfaz Exclusiva de Optimización de RAM & Fugas)**: Pantalla dedicada con velocímetro circular de memoria RAM en tiempo real, métricas de memoria total, libre, usada y recuperable, categorización inteligente (Caché, Vacíos, Servicios, Sistema), selección rápida y optimización quirúrgica mediante `trim_ram_and_leaks.sh`.
- **`DebugConsoleScreen.kt` (Herramientas de Depuración Embebidas)**: Panel con visor de Logcat en vivo, terminal interactiva Shizuku y pestaña de diagnóstico de LeakCanary con disparadores de volcado de Heap y simulación de fugas.
- **Transición Fluida**: Transición gestionada por `CleanViewModel.currentScreen` y `Crossfade` en `MainActivity.kt`, garantizando que el estado del escaneo se mantenga intacto al navegar entre pantallas.

### 3. Herramientas de Depuración Embebidas para Móvil (Sin PC)
- **LeakCanary 2.14 Totalmente Funcional**:
  - Integrado en `implementation` para garantizar su presencia activa en compilaciones independientes para Uptodown/móvil.
  - Inicializado de forma prioritaria en `CleanerApp.kt` (`Application`) con `retainedVisibleThreshold = 1` para avisar de inmediato ante cualquier retención.
  - Permiso `POST_NOTIFICATIONS` declarado y solicitado dinámicamente en Android 13+ para garantizar que las notificaciones sonoras y visuales se emitan en la barra de estado.
  - App independiente **"Leaks"** instalada automáticamente en el teléfono y ejecutable desde el botón de la consola de depuración.
  - Botón para forzar volcado de Heap manual (`LeakCanary.dumpHeap()`) y simular fugas de prueba controladas (`AppWatcher.objectWatcher.expectWeaklyReachable`).
- **Visor de Logcat en Vivo**: Streaming de logs del sistema operativo con filtros por texto y niveles (VERBOSE, DEBUG, INFO, WARN, ERROR) con colores distintivos y capacidad de vaciado de buffer (`logcat -c`).
- **Terminal Shell Shizuku**: Ejecución de comandos del sistema directos (`pm list packages`, `df -h`, `id`, `ls -la`) con accesos rápidos preconfigurados y salida formateada en fuente monoespaciada tipo consola.

### 4. Pipeline de Automatización y Scripts CI/CD (`.github/`)
- **`setup_debug_keystore.sh`**: Script en `.github/scripts/` que borra cualquier keystore previa y obliga a `keytool` a generar una firma `debug.keystore` limpia con validez de 10,000 días tanto en la raíz como en `~/.android/debug.keystore`.
- **`setup_debug_tools.sh`**: Script en `.github/scripts/` que comprueba e inyecta la presencia obligatoria de LeakCanary y valida los componentes de depuración en el runner de GitHub.
- **`build-debug-apk.yml`**: Workflow de GitHub Actions que corre manualmente vía `workflow_dispatch`, orquestado por los scripts `.sh` sin usar caché.
- **`override-commit-message.yml`**: Workflow que sincroniza el mensaje de cada commit leyendo el contenido en español de `commit_message.txt`.

### 5. Hilos y Reactividad (Concurrencia)
- **Regla de Oro**: Ninguna operación de lectura de disco, cálculo de tamaño o ejecución de scripts debe correr en el hilo principal (`Main Thread`).
- Todo el escaneo se ejecuta en `Dispatchers.IO` dentro de corrutinas gestionadas por `viewModelScope`.
- Se usa `Kotlin Flow` para emitir el progreso de forma reactiva, permitiendo que la UI muestre en tiempo real el directorio que se está escaneando sin tartamudeos ni congelamientos.
