# Limpiador de Almacenamiento & Analizador de «Otros»

> Aplicación Android nativa en Jetpack Compose con integración profunda de **Shizuku (ADB/Root)** y **scripts Shell (`.sh`)** para auditar, clasificar y liberar espacio del almacenamiento categorizado como **«Otros»** de forma segura y transparente.

---

## 📋 Descripción del Proyecto

En Android, la categoría de almacenamiento **«Otros»** suele devorar decenas de gigabytes (15 GB - 30 GB o más) acumulando miniaturas huérfanas (`.thumbnails`), cachés no indexadas de aplicaciones de mensajería (Telegram, WhatsApp), archivos residuales de apps desinstaladas, logs del sistema y fragmentos temporales. 

Esta aplicación proporciona una interfaz técnica de alta precisión para escanear en hilos secundarios (sin congelar la interfaz de usuario) todo el almacenamiento del dispositivo, clasificar cada elemento según su nivel de riesgo (**SEGURO**, **PRECAUCIÓN**, **VITAL**) y ejecutar la limpieza asistida mediante scripts Shell optimizados y llamadas nativas al gestor de paquetes de Android (`IPackageManager`) a través de **Shizuku**.

---

## 🚀 Características Principales

1. **Suite de Scripts Shell Autónomos (`assets/scripts/`)**:
   - `scan_other_storage.sh`: Detección profunda de miniaturas ocultas (`.thumbnails`), cachés multimedia, temporales y APKs en desuso.
   - `clean_orphaned_packages.sh`: Comparador inteligente que audita `/sdcard/Android/data` y `/sdcard/Android/obb` contra los paquetes instalados (`pm list packages`) para identificar carpetas de juegos y aplicaciones desinstaladas que siguen ocupando gigabytes.
   - `purge_system_logs_and_dumps.sh`: Detección y purga segura de volcados `.dump`, `.log`, `.trace`, *tombstones* y archivos residuales de depuración en `/data/local/tmp`.
   - `trim_art_cache.sh`: Invocación del mecanismo oficial del sistema `pm trim-caches 999999999999999` y limpieza de temporales de compilación DEX sin alterar propiedades protegidas del sistema.
   - Clasificación estricta por niveles de seguridad:
     - 🟢 **SEGURO (SAFE)**: Miniaturas, temporales `.tmp`, cachés de descarga y logs. Borrado garantizado sin efectos negativos.
     - 🟡 **PRECAUCIÓN (CAUTION)**: Carpetas de apps ya desinstaladas, descargas antiguas de más de 30 días, backups locales. Requiere confirmación del usuario.
     - 🔴 **VITAL (KEEP)**: Archivos `.obb`, bases de datos SQLite (`.db`), copias de seguridad de cifrado y directorios raíz críticos. Están protegidos contra borrado involuntario.

2. **Integración con Shizuku (IPackageManager & ADB Shell)**:
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
│       ├── scan_other_storage.sh        # Script shell para escaneo y clasificación de «Otros»
│       ├── clean_orphaned_packages.sh   # Cazador de carpetas de apps desinstaladas en Android/data
│       ├── purge_system_logs_and_dumps.sh # Purga de logs .dump, .trace y /data/local/tmp
│       └── trim_art_cache.sh            # Recorte global de caché con pm trim-caches
├── java/com/example/
│   ├── MainActivity.kt                  # Activity con Edge-to-Edge y registro de permisos
│   ├── model/
│   │   ├── StorageModels.kt             # Modelos de datos: JunkItem, OtherStorageItem, SafetyLevel
│   │   └── ShizukuModels.kt             # Estado de conexión Shizuku
│   ├── scanner/
│   │   ├── StorageScanner.kt            # Escáner tradicional de cachés y temporales
│   │   └── OtherStorageScanner.kt       # Motor de ejecución del script shell + parser por Flow
│   ├── shizuku/
│   │   └── ShizukuHelper.kt             # Binder IPC con Shizuku, IPackageManager y ejecución ADB
│   └── ui/
│       ├── CleanScreen.kt               # Pantalla principal en Jetpack Compose
│       ├── CleanViewModel.kt            # Lógica de estado reactiva (StateFlow)
│       ├── components/                  # Componentes M3 reutilizables
│       │   ├── OtherStorageSection.kt   # Tarjeta y lista interactiva de «Otros»
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
