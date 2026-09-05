# Arquitectura y Estructura del Proyecto (`estructure.md`)

Este documento detalla la estructura física del código fuente, las capas arquitectónicas del sistema y el flujo de datos que rige el funcionamiento del limpiador.

---

## 🏛️ Patrón Arquitectónico: MVVM + Clean Data Flow

La aplicación sigue el patrón **Model-View-ViewModel (MVVM)** con **Flujo Unidireccional de Datos (UDF)**:
1. **View (Jetpack Compose)**: Observa los `StateFlow` del `ViewModel` y reacciona de manera puramente declarativa. Emite intenciones del usuario (eventos).
2. **ViewModel (`CleanViewModel`)**: Mantiene el estado inmutable de la UI, orquesta corrutinas y no tiene referencias directas a elementos del SDK que requieran contexto de Activity.
3. **Domain & Data**:
   - `StorageScanner`: Auditoría de almacenamiento estándar.
   - `OtherStorageScanner`: Ejecución y parseo del script Shell `scan_other_storage.sh`.
   - `ShizukuHelper`: Puente IPC por Binder con el daemon de Shizuku y el gestor de paquetes de Android (`IPackageManager`).

---

## 📂 Árbol de Carpetas y Archivos

```
/
├── .github/
│   ├── scripts/
│   │   ├── setup_debug_keystore.sh      # Generación incondicional de debug.keystore con keytool
│   │   ├── setup_debug_tools.sh         # Inyección y verificación de LeakCanary y visor de depuración
│   │   └── setup_shizuku_deps.sh        # Inyección, verificación y forzado de dependencias Shizuku API/Provider
│   └── workflows/
│       ├── build-debug-apk.yml          # Pipeline CI/CD manual para compilar APK Debug sin caché
│       ├── build-shizuku-debug-apk.yml  # Pipeline CI/CD especializado con Shizuku verificado y empaquetado
│       └── override-commit-message.yml  # Automatización de mensajes de commit mediante commit_message.txt
├── commit_message.txt                   # Mensaje de commit estándar en español
├── README.md                            # Guía general del proyecto y pasos de compilación
├── roadmap.md                           # Hoja de ruta de evolución y características futuras
├── estructure.md                        # Este documento: arquitectura y responsabilidades de código
├── AI_context.md                        # Contexto para modelos de IA y asistentes técnicos
├── AGENTS.md                            # Reglas operativas estrictas para agentes de programación
├── build.gradle.kts                     # Configuración de Gradle raíz
├── settings.gradle.kts                  # Declaración de módulos y repositorios Maven
├── app/
│   ├── build.gradle.kts                 # Dependencias (Compose, Shizuku, LeakCanary Debug)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml      # Declaración de permisos y configuración de Shizuku Provider
│       │   ├── assets/
│       │   │   └── scripts/
│       │   │       ├── scan_other_storage.sh        # Script Shell para auditar carpetas de «Otros»
│       │   │       ├── clean_orphaned_packages.sh   # Cazador de carpetas de apps desinstaladas
│       │   │       ├── purge_system_logs_and_dumps.sh # Purga de volcados y logs de sistema
│       │   │       └── trim_art_cache.sh            # Recorte global de cachés y optimización ART
│       │   ├── java/com/example/
│       │   │   ├── MainActivity.kt      # Punto de entrada de la app Android, enrutador de pantallas
│       │   │   ├── model/
│       │   │   │   ├── StorageModels.kt # Modelos: JunkCategory, OtherStorageItem, SafetyLevel
│       │   │   │   ├── ShizukuModels.kt # Modelos: ShizukuStatus, ShizukuInfo
│       │   │   │   └── LogcatModel.kt   # Modelos: LogEntry, LogLevel para visor en vivo
│       │   │   ├── scanner/
│       │   │   │   ├── StorageScanner.kt # Motor de análisis estándar de carpetas y cachés
│       │   │   │   └── OtherStorageScanner.kt # Deserializador de salida del script shell a objetos Kotlin
│       │   │   ├── shizuku/
│       │   │   │   ├── ShizukuHelper.kt             # Detección resiliente, listeners de binder y ejecución ADB
│       │   │   │   └── ShizukuPermissionManager.java # Puente nativo IPC con IPackageManager y ShizukuBinderWrapper
│       │   │   └── ui/
│       │   │       ├── CleanViewModel.kt # StateFlows de progreso, selección, navegación, logcat y terminal
│       │   │       ├── CleanScreen.kt    # Composable Dashboard principal
│       │   │       ├── OtherStorageScreen.kt # Pantalla 100% independiente para auditoría de «Otros»
│       │   │       ├── DebugConsoleScreen.kt # Visor de Logcat en vivo y Consola Shell Shizuku
│       │   │       ├── components/
│       │   │       │   ├── OtherStorageSection.kt  # Subcomponentes para visualización de ítems
│       │   │       │   ├── ShizukuStatusCard.kt    # Tarjeta de diagnóstico de permisos ADB/Root
│       │   │       │   ├── StorageCircularGauge.kt # Medidor circular de almacenamiento
│       │   │       │   ├── JunkCategoryItem.kt     # Item expandible por categoría de basura
│       │   │       │   └── CleaningDialogs.kt      # Diálogos de progreso y confirmación
│       │   │       └── theme/
│       │   │           ├── Color.kt      # Paleta Deep Tech Dark (Cian, Esmeralda, Violeta, Ámbar)
│       │   │           ├── Theme.kt      # Definición de MaterialTheme M3
│       │   │           └── Type.kt       # Tipografía de la interfaz
│       │   └── res/
│       │       ├── values/
│       │       │   └── strings.xml      # Textos localizados en español
│       │       └── mipmap-*/            # Iconos adaptativos de la aplicación
│       └── test/
│           └── java/com/example/
│               └── ExampleRobolectricTest.kt # Pruebas unitarias locales sobre JVM
```

---

## 🔄 Flujo de Datos: Escaneo de «Otros»

```
[Usuario presiona "Escanear"]
          │
          ▼
CleanViewModel.startOtherStorageScan()
          │
          ▼  (Lanza Coroutine en Dispatchers.IO)
OtherStorageScanner.scanOtherStorage(context)
          │
          ├─► Lee /assets/scripts/scan_other_storage.sh
          ├─► Lo escribe en cacheDir y le asigna permiso de ejecución
          │
     ┌────┴───────────────────────────┐
     │ ¿Shizuku activo & autorizado? │
     └────┬───────────────────────────┘
          │ (SÍ)                             │ (NO)
          ▼                                  ▼
ShizukuHelper.executeAdbCommand()      Runtime.getRuntime().exec()
("sh scan_other_storage.sh")           (Modo respaldo nativo)
          │                                  │
          └──────────────┬───────────────────┘
                         ▼
             Lee stdout línea a línea
        Línea: "ITEM|size|path|cat|SAFE|desc"
                         │
                         ▼
        Mapeo a OtherStorageItem(safety=SAFE)
                         │
                         ▼
               Emite por Kotlin Flow
                         │
                         ▼
          CleanViewModel actualiza StateFlow
                         │
                         ▼
       Recomposición en OtherStorageSection
```

---

## 🔒 Reglas de Seguridad en el Módulo Scanner

1. **Nunca borrar sin confirmación**: La interfaz requiere selección explícita por parte del usuario.
2. **Inmunidad de archivos Vitales (`KEEP`)**: Los elementos clasificados con `SafetyLevel.KEEP` tienen deshabilitada la casilla de verificación en la UI para evitar su borrado.
3. **Manejo de Errores Silencioso pero Informativo**: Si un archivo no puede eliminarse por falta de permisos o bloqueo del sistema, se captura la excepción y se informa al usuario mediante Snackbar en lugar de causar un bloqueo (crash).

---

## ⚡ Arquitectura de Reconocimiento Shizuku y Sui

1. **Detección Resiliente**: No depende exclusivamente de `getPackageInfo` (que en Android 11+ requiere visibilidad de paquete en `<queries>`), sino que otorga prioridad a `Shizuku.pingBinder()`. Si el Binder responde (sea vía app oficial o mediante el módulo Magisk **Sui**), el servicio se marca activo de inmediato.
2. **Puente IPC con IPackageManager**: A través de `ShizukuPermissionManager.java`, se invoca `IPackageManager.Stub.asInterface(new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))` para otorgar permisos en tiempo de ejecución (`grantRuntimePermission`) a nivel ADB/Root.
3. **Automatización en CI/CD**:
   - `.github/scripts/setup_shizuku_deps.sh`: Script que comprueba y pre-descarga `dev.rikka.shizuku:api` y `dev.rikka.shizuku:provider`.
   - `.github/workflows/build-shizuku-debug-apk.yml`: Pipeline para compilar el APK Debug con validación obligatoria de la cadena de dependencias de Shizuku.

