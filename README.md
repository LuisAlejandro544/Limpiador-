# Limpiador de Almacenamiento & Analizador de «Otros»

> Aplicación Android nativa en Jetpack Compose con integración profunda de **Shizuku (ADB/Root)** y **scripts Shell (`.sh`)** para auditar, clasificar y liberar espacio del almacenamiento categorizado como **«Otros»** de forma segura y transparente.

---

## 📋 Descripción del Proyecto

En Android, la categoría de almacenamiento **«Otros»** suele devorar decenas de gigabytes (15 GB - 30 GB o más) acumulando miniaturas huérfanas (`.thumbnails`), cachés no indexadas de aplicaciones de mensajería (Telegram, WhatsApp), archivos residuales de apps desinstaladas, logs del sistema y fragmentos temporales. 

Esta aplicación proporciona una interfaz técnica de alta precisión para escanear en hilos secundarios (sin congelar la interfaz de usuario) todo el almacenamiento del dispositivo, clasificar cada elemento según su nivel de riesgo (**SEGURO**, **PRECAUCIÓN**, **VITAL**) y ejecutar la limpieza asistida mediante scripts Shell optimizados y llamadas nativas al gestor de paquetes de Android (`IPackageManager`) a través de **Shizuku**.

---

## 🚀 Características Principales

1. **Suite de Scripts Shell Autónomos (`assets/scripts/`)**:
   - `trim_ram_and_leaks.sh`: **Limpieza quirúrgica de memoria RAM y fugas**. Utiliza `am trim-memory <paquete> COMPLETE` y `am kill-all` para reclamar memoria caché y liberar procesos en segundo plano de manera segura sin forzar reinicios ni dañar procesos vitales del sistema.
   - `scan_other_storage.sh`: Detección profunda de miniaturas ocultas (`.thumbnails`), cachés multimedia, temporales, APKs en desuso y **bases de datos huérfanas/fragmentos SQLite rotos** (`.db-wal`, `.db-shm`, `.db-journal`) abandonados por aplicaciones desinstaladas.
   - `clean_orphaned_packages.sh`: Comparador inteligente que audita `/sdcard/Android/data` y `/sdcard/Android/obb` contra los paquetes instalados (`pm list packages`) para identificar carpetas y bases de datos residuales de juegos y aplicaciones desinstaladas que siguen ocupando gigabytes.
   - `purge_system_logs_and_dumps.sh`: Detección y purga segura de volcados `.dump`, `.log`, `.trace`, *tombstones* y archivos residuales de depuración en `/data/local/tmp`.
   - `trim_art_cache.sh`: Invocación del mecanismo oficial del sistema `pm trim-caches 999999999999999` y limpieza de temporales de compilación DEX sin alterar propiedades protegidas del sistema.
   - Clasificación estricta por niveles de seguridad:
     - 🟢 **SEGURO (SAFE)**: Miniaturas, temporales `.tmp`, cachés de descarga, bases de datos residuales de apps desinstaladas, fragmentos WAL/SHM huérfanos, procesos de RAM en caché y procesos vacíos. Borrado garantizado sin efectos secundarios.
     - 🟡 **PRECAUCIÓN (CAUTION)**: Carpetas de apps ya desinstaladas, descargas antiguas de más de 30 días, servicios en segundo plano. Requiere confirmación del usuario.
     - 🔴 **VITAL (KEEP)**: Archivos `.obb` de juegos instalados, bases de datos SQLite en uso activo, apps del sistema y procesos activos en primer plano. Están protegidos contra cierre o borrado involuntario.

2. **Herramienta Exclusiva de Limpieza de RAM & Fugas (`RamCleanScreen`)**:
   - Pantalla dedicada con medidor circular de memoria RAM en tiempo real, desglose de memoria total, libre, usada y espacio recuperable.
   - Clasificación inteligente de procesos por categorías: *Segundo Plano / Caché* (100% seguro), *Procesos Vacíos*, *Servicios en Segundo Plano* y *Sistema / Activos*.
   - Selección granular e inteligente (selección rápida con un toque de procesos seguros, exclusión automática de aplicaciones críticas del sistema).
   - Optimización en un toque que ejecuta `trim_ram_and_leaks.sh` reportando la ganancia exacta de megabytes liberados sin congelar la interfaz.

3. **Interfaz 100% Independiente para «Otros» (`OtherStorageScreen`)**:
   - Pantalla dedicada con barra de navegación superior, soporte para botón físico de volver (`BackHandler`), métricas de espacio liberable en tiempo real, filtros dinámicos por nivel de seguridad (*Seguros*, *Precaución*, *Vital*) y por categoría (*Bases de Datos Huérfanas*, *Miniaturas*, *Cachés*).
   - Acciones de selección rápida con un toque ("Marcar solo seguros", "Desmarcar todos") y botón flotante de limpieza con diálogo de confirmación de seguridad.

4. **Herramientas de Depuración y LeakCanary Integradas (Sin PC)**:
   - 🐤 **LeakCanary 2.14 Activo y Notificaciones Funcionando**: Integrado a nivel de aplicación con clase `CleanerApp` (`Application`) y umbral de 1 objeto retenido para análisis inmediato. Con soporte completo para el permiso `POST_NOTIFICATIONS` de Android 13+ y pestaña dedicada en `DebugConsoleScreen` con botones para:
     - Abrir la app independiente **"Leaks"** directamente desde la interfaz.
     - Forzar volcado de Heap (`dumpHeap`) y emitir la notificación de análisis al instante.
     - Simular una fuga de memoria de prueba (512 KB) para verificar el funcionamiento de las alertas y el rastreo en vivo.
   - 💻 **Visor de Logcat en Vivo (`DebugConsoleScreen`)**: Panel de diagnóstico accesible desde el icono de depuración en la barra superior con streaming de consola en vivo, chips de filtrado por nivel de severidad (VERBOSE, DEBUG, INFO, WARN, ERROR), buscador textual y vaciado de logs (`logcat -c`).
   - ⚡ **Consola Shell Shizuku Embebida**: Ventana interactiva de terminal para teclear o disparar comandos de prueba rápidos (`id`, `whoami`, `pm list packages`, `ls -la /sdcard/Android/data`) directamente en el dispositivo móvil.

5. **Automatización CI/CD con Scripts Shell Dedicados (`.github/scripts/`)**:
   - `setup_debug_keystore.sh`: Script ejecutable que elimina keystores previas y fuerza la generación de un `debug.keystore` limpio con `keytool` en `./debug.keystore` y `~/.android/debug.keystore` con permisos 644.
   - `setup_debug_tools.sh`: Script que verifica, inyecta y pre-descarga las dependencias de depuración (LeakCanary y Visor de Logcat) antes de la compilación de Gradle.
   - `setup_shizuku_deps.sh`: Script ejecutable que verifica, inyecta y descarga anticipadamente las dependencias de Shizuku (`api` y `provider` 13.1.5) y comprueba el `ShizukuProvider` y `<queries>` en el Manifest.
   - `build-debug-apk.yml` & `build-shizuku-debug-apk.yml`: Workflows automatizados para compilar APKs Debug con todas las dependencias forzadas y verificadas.
   - `override-commit-message.yml`: Workflow que estandariza automáticamente los mensajes de commit en español leyendo el archivo `commit_message.txt`.

5. **Integración con Shizuku (IPackageManager & ADB Shell)**:
   - Conexión dinámica por IPC con el servicio Shizuku (`moe.shizuku.manager`).
   - Concesión automática de permisos especiales de almacenamiento en Android 11+ (`MANAGE_EXTERNAL_STORAGE` / `READ_MEDIA_*`) mediante `grantRuntimePermission`.
   - Ejecución de comandos de recorte de caché global a nivel de sistema (`pm trim-caches 999999999999999`).
   - Capacidad de operar en modo ADB (sin PC, usando Depuración Inalámbrica en el propio móvil) o modo Root.

3. **Arquitectura No Bloqueante (Thread Safety)**:
   - Todo el análisis intensivo de I/O y ejecución de comandos shell se procesa en `Dispatchers.IO` mediante Kotlin Coroutines y Kotlin Flow reactivo.
   - Actualizaciones de progreso en tiempo real que informan al usuario sobre el directorio exacto que se está analizando.

4. **Integración Continua con GitHub Actions (Compilación Limpia)**:
   - Workflow automatizado en `.github/workflows/build-debug-apk.yml`.
   - Generación de firma `debug.keystore` al vuelo dentro del propio runner runner de GitHub.
   - Ejecución 100% limpia sin caché (`--no-build-cache --no-daemon`) para garantizar compilaciones reproducibles listas para descargar como artefacto APK.

4. **Diseño de Interfaz Futurista (Deep Tech Dark)**:
   - Construida 100% en Jetpack Compose y Material 3 con fondo oscuro profundo `#0A0F1D`, acentos en Cian Neón `#00E5FF`, Violeta Shizuku `#7C4DFF` y Esmeralda `#00E676`.
   - Medidores circulares de almacenamiento con interpolación de color según ocupación y tarjetas de estado interactivas.

---

## 🛠️ Requisitos Previos y Compatibilidad

- **Sistema Operativo**: Android 8.0 (API 26) o superior (Optimizado para Android 11, 12, 13, 14 y 15).
- **Herramienta Shizuku (Opcional pero recomendada)**:
  - Para auditar carpetas protegidas como `Android/data` y ejecutar limpieza global de caché, se recomienda tener instalada la app [Shizuku](https://github.com/RikkaApps/Shizuku/releases) y activada por depuración inalámbrica.
  - La app cuenta con un motor de escaneo nativo de respaldo que opera sin Shizuku en el almacenamiento estándar accesible.
- **Distribución**: Compatible con instalación directa mediante APK de terceros (Uptodown, F-Droid o GitHub Releases) sin depender de Google Play Services ni servicios propietarios de Google.

---

## 📦 Estructura del Código

```
app/src/main/
├── assets/
│   └── scripts/
│       ├── trim_ram_and_leaks.sh        # Limpieza quirúrgica de memoria RAM y am kill-all
│       ├── scan_other_storage.sh        # Script shell para escaneo y clasificación de «Otros»
│       ├── clean_orphaned_packages.sh   # Cazador de carpetas de apps desinstaladas en Android/data
│       ├── purge_system_logs_and_dumps.sh # Purga de logs .dump, .trace y /data/local/tmp
│       └── trim_art_cache.sh            # Recorte global de caché con pm trim-caches
├── java/com/example/
│   ├── CleanerApp.kt                    # Application class para inicialización de LeakCanary
│   ├── MainActivity.kt                  # Activity con Edge-to-Edge, permisos y rutas
│   ├── model/
│   │   ├── StorageModels.kt             # Modelos de datos: JunkItem, OtherStorageItem, SafetyLevel
│   │   ├── RamModels.kt                 # Modelos de RAM: RamStatus, ProcessInfo, ProcessCategory
│   │   └── ShizukuModels.kt             # Estado de conexión Shizuku
│   ├── scanner/
│   │   ├── StorageScanner.kt            # Escáner tradicional de cachés y temporales
│   │   ├── OtherStorageScanner.kt       # Motor de ejecución del script shell + parser por Flow
│   │   └── RamCleaner.kt                # Auditoría y liberación de memoria RAM vía Shell/Shizuku
│   ├── shizuku/
│   │   └── ShizukuHelper.kt             # Binder IPC con Shizuku, IPackageManager y ejecución ADB
│   └── ui/
│       ├── CleanScreen.kt               # Pantalla principal (Dashboard) en Jetpack Compose
│       ├── OtherStorageScreen.kt        # Interfaz independiente para análisis profundo de «Otros»
│       ├── RamCleanScreen.kt            # Pantalla exclusiva para optimización y fugas de RAM
│       ├── DebugConsoleScreen.kt        # Visor de Logcat en vivo, consola Shell y control LeakCanary
│       ├── CleanViewModel.kt            # Lógica de estado reactiva (StateFlow)
│       ├── components/                  # Componentes M3 reutilizables
│       │   ├── OtherStorageSection.kt   # Subcomponentes de visualización de «Otros»
│       │   ├── ShizukuStatusCard.kt     # Diagnóstico y acciones de Shizuku
│       │   ├── StorageCircularGauge.kt  # Gráfico radial de disco
│       │   └── CleaningDialogs.kt       # Diálogos de progreso y resumen
│       └── theme/                       # Paleta Deep Tech Dark y Tipografía
```

---

## ⚙️ Cómo Compilar y Probar

### Requisitos de Desarrollo
- JDK 17 o superior.
- Android SDK con Platform Tools (API 34).
- Gradle 8.x con Android Gradle Plugin.

### Compilar APK en modo Debug:
```bash
gradle :app:assembleDebug
```
El archivo APK generado se ubicará en:
`app/build/outputs/apk/debug/app-debug.apk`

### Ejecutar Tests Unitarios:
```bash
gradle :app:testDebugUnitTest
```

---

## 📖 Modo de Uso Rápido (En el Teléfono)

1. **Instalar el APK**: Descargar e instalar el archivo `.apk` en el dispositivo móvil.
2. **Conceder Permisos de Almacenamiento**:
   - Si tienes **Shizuku** activo, pulsa en el botón **«Conceder Permisos con Shizuku»** en la tarjeta superior para auto-otorgarte todos los permisos de almacenamiento en 1 toque.
   - O bien pulsa el botón del banner para autorizarlos manualmente en los Ajustes del sistema.
3. **Escanear «Otros»**:
   - Desplázate hasta la tarjeta **«Desglose de «Otros»»**.
   - Presiona **«Escanear»**. El script Shell analizará las rutas críticas de la memoria interna en segundo plano.
4. **Revisar y Limpiar**:
   - Filtra los elementos por **«Seguros»**, **«Precaución»** o **«Vital»**.
   - Selecciona los elementos deseados o presiona **«Marcar solo seguros»**.
   - Toca **«Limpiar»** para liberar los gigabytes seleccionados de inmediato.

---

## 🛡️ Seguridad y Privacidad

- **Sin Telemetría ni Rastreadores**: Cero analíticas, cero recolección de datos y cero llamadas a servidores externos.
- **Protección de Archivos Vitales**: Los archivos del sistema y datos de juegos (`.obb`, `.db`) tienen el flag `KEEP` y están bloqueados para evitar su selección accidental.
- **Ejecución Local**: Todo el código se ejecuta exclusivamente de manera local en el microprocesador de tu dispositivo.

---

## 📄 Licencia

Este proyecto está distribuido bajo la licencia MIT. Eres libre de modificarlo, distribuirlo y compilarlo para su distribución en tiendas libres de Android.
