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
│   └── workflows/
│       └── build-debug-apk.yml          # Pipeline CI/CD para compilar APK Debug sin caché
├── README.md                            # Guía general del proyecto y pasos de compilación
├── roadmap.md                           # Hoja de ruta de evolución y características futuras
├── estructure.md                        # Este documento: arquitectura y responsabilidades de código
├── AI_context.md                        # Contexto para modelos de IA y asistentes técnicos
├── AGENTS.md                            # Reglas operativas estrictas para agentes de programación
├── build.gradle.kts                     # Configuración de Gradle raíz
├── settings.gradle.kts                  # Declaración de módulos y repositorios Maven
├── app/
│   ├── build.gradle.kts                 # Configuración de dependencias (Compose, Shizuku, Coroutines)
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
│       │   │   ├── MainActivity.kt      # Punto de entrada de la app Android, edge-to-edge
│       │   │   ├── model/
│       │   │   │   ├── StorageModels.kt # Modelos: JunkCategory, OtherStorageItem, SafetyLevel
│       │   │   │   └── ShizukuModels.kt # Modelos: ShizukuStatus, ShizukuInfo
│       │   │   ├── scanner/
│       │   │   │   ├── StorageScanner.kt # Motor de análisis estándar de carpetas y cachés
│       │   │   │   └── OtherStorageScanner.kt # Deserializador de salida del script shell a objetos Kotlin
│       │   │   ├── shizuku/
│       │   │   │   └── ShizukuHelper.kt  # Llamadas IPC con Shizuku, IPackageManager y ejecución ADB
│       │   │   └── ui/
│       │   │       ├── CleanViewModel.kt # StateFlows de progreso, selección, navegación y limpieza
│       │   │       ├── CleanScreen.kt    # Composable Dashboard principal
│       │   │       ├── OtherStorageScreen.kt # Pantalla 100% independiente para auditoría de «Otros»
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
