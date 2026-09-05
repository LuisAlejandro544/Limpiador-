# Roadmap del Proyecto: Limpiador & Analizador de Almacenamiento «Otros»

Este documento traza la evolución técnica del proyecto, desde las bases actuales hasta las próximas iteraciones pensadas para optimizar teléfonos Android sin depender de PC ni de Google Play.

---

## 📌 Fase 1: Fundaciones y Auditoría Básica (Completada ✅)

- [x] Arquitectura base en **Jetpack Compose** con tema técnico *Deep Tech Dark*.
- [x] Detección de almacenamiento nativo de Android (`StatFs` para espacio total, libre y usado).
- [x] Escáner de cachés estándar, archivos temporales, logs y carpetas vacías con Kotlin Coroutines.
- [x] Tarjetas de progreso radial con interpolación de color según ocupación de memoria.
- [x] Gestión de permisos de almacenamiento (`MANAGE_EXTERNAL_STORAGE` y permisos multimedia de Android 13+).

---

## 📌 Fase 2: Integración Profunda de Shizuku y Scripts Shell (Completada ✅)

- [x] Integración de la biblioteca oficial de **Shizuku** (`rikka.shizuku:api`).
- [x] Conexión por IPC al servicio Shizuku para operar como ADB (UID 2000) o Root (UID 0).
- [x] Otorgamiento automático de permisos mediante `IPackageManager` (`grantRuntimePermission`).
- [x] Función de recorte global de caché de todas las aplicaciones (`pm trim-caches`).
- [x] Creación del script Shell autónomo `scan_other_storage.sh` empaquetado en los assets de la app.
- [x] Clasificación inteligente del almacenamiento «Otros» en tres niveles:
  - 🟢 **SAFE**: Miniaturas (`.thumbnails`), archivos temporales y restos de caché.
  - 🟡 **CAUTION**: Carpetas de aplicaciones antiguas y descargas obsoletas.
  - 🔴 **KEEP**: Archivos vitales y bases de datos (`.obb`, `.db`).
- [x] Ejecución no bloqueante en hilo secundario (`Dispatchers.IO`) con emisión reactiva vía `Kotlin Flow`.
- [x] Componente `OtherStorageSection` con filtros interactivos, selección granular y limpieza segura.

---

## 📌 Fase 3: Optimización Avanzada de Scripts y Automatización CI/CD (Completada ✅)

- [x] **Detector de paquetes huérfanos (`clean_orphaned_packages.sh`)**:
  - Script shell que compara las carpetas en `/sdcard/Android/data` y `/sdcard/Android/obb` contra los paquetes del sistema (`pm list packages`) para identificar apps desinstaladas que dejaron basura.
- [x] **Purga de registros del sistema y volcados (`purge_system_logs_and_dumps.sh`)**:
  - Detección y limpieza de archivos `.dump`, `.log`, `.trace` y archivos temporales en `/data/local/tmp`.
- [x] **Recorte de caché de compilación ART (`trim_art_cache.sh`)**:
  - Optimización de cachés sin alterar propiedades del sistema `persist.sys.*`.
- [x] **Cazador de Bases de Datos SQLite Huérfanas y Fragmentos Corruptos**:
  - Auditoría de archivos `.db`, `.sqlite`, y fragmentos transaccionales zombis `.db-wal`, `.db-shm`, `.db-journal` sin archivo principal.
- [x] **Interfaz Dedicada 100% Independiente para «Otros» (`OtherStorageScreen`)**:
  - Pantalla con AppBar propia, `BackHandler` integrado, filtros por categoría y riesgo, métricas en tiempo real y selector masivo.
- [x] **Pipeline CI/CD en GitHub Actions con Scripts Shell Dedicados**:
  - `setup_debug_keystore.sh`: Forzado incondicional de generación de keystore con `keytool` sin depender de búsquedas externas.
  - `setup_debug_tools.sh`: Inyección y validación anticipada de dependencias de depuración en el runner.
  - `override-commit-message.yml`: Estandarización automatizada del mensaje de commit leyendo `commit_message.txt`.
- [x] **Herramientas de Depuración Embebidas para el Móvil**:
  - **LeakCanary 2.14**: Monitoreo de memoria en segundo plano que instala su propia app "Leaks" en el cajón de aplicaciones.
  - **Visor de Logcat en Vivo y Consola Shell (`DebugConsoleScreen`)**: Panel accesible desde el TopAppBar con streaming de logs por nivel (V, D, I, W, E), comandos rápidos ADB y terminal interactiva con Shizuku.

---

## 📌 Fase 4: Limpieza Quirúrgica y Experiencia Móvil (En Progreso 🔄)

- [ ] **Limpieza específica de apps de mensajería**:
  - Script dedicado para detectar audios de voz antiguos, stickers y archivos temporales duplicados de WhatsApp y Telegram sin tocar fotos ni chats.
- [ ] **Historial de limpieza local**:
  - Registro de espacio liberado por fecha para que el usuario conozca cuántos gigabytes ha ahorrado a lo largo del tiempo.
- [ ] **Generador de reglas personalizadas (.sh)**:
  - Permitir al usuario avanzado añadir sus propios comandos de limpieza o rutas específicas a auditar.
- [ ] **Optimización para ejecución rápida en teléfonos con procesadores modestos**:
  - Paginación y streaming de resultados en tiempo real para escaneos con más de 100,000 archivos.
- [ ] **Notificaciones programadas de mantenimiento**:
  - Alerta sutil cuando la carpeta `.thumbnails` supere los 2 GB de tamaño para sugerir una limpieza preventiva.

---

## 📌 Criterios de Distribución Independiente (Sin Google Play)

- **Compatibilidad con Uptodown y APKs libres**:
  - Cero dependencias de Google Play Services o Firebase Analytics.
  - Actualizador integrado o verificación de versiones mediante GitHub Releases API.
  - Firma reproducible para instalación sin advertencias extra en cualquier versión de Android.
