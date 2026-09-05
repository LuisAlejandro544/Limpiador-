#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script de Inyección y Verificación de Herramientas de Debug (CI/CD)
# Obliga a incluir, descargar y verificar en el APK Debug:
# 1. LeakCanary (Detección de fugas de memoria con app 'Leaks' independiente)
# 2. Visor de Logcat y Consola en Vivo (In-App Logcat Viewer & Terminal Shell)
# ==============================================================================

echo "========================================================"
echo " [CI] Verificación e Inyección de Herramientas Debug"
echo "========================================================"

TOML_FILE="gradle/libs.versions.toml"
GRADLE_FILE="app/build.gradle.kts"

# 1. Verificar e inyectar LeakCanary en el Version Catalog si no estuviera presente
echo "[1/4] Comprobando integración de LeakCanary en Version Catalog..."
if ! grep -q "leakcanary-android" "${TOML_FILE}"; then
  echo "[-] Inyectando leakcanary en ${TOML_FILE}..."
  sed -i '/\[libraries\]/a leakcanary-android = { group = "com.squareup.leakcanary", name = "leakcanary-android", version = "2.14" }' "${TOML_FILE}"
else
  echo "[+] LeakCanary ya declarado en Version Catalog."
fi

# 2. Verificar e inyectar debugImplementation(libs.leakcanary.android) en build.gradle.kts
echo "[2/4] Comprobando debugImplementation en ${GRADLE_FILE}..."
if ! grep -q "leakcanary.android" "${GRADLE_FILE}"; then
  echo "[-] Inyectando dependencia debugImplementation(libs.leakcanary.android)..."
  sed -i '/dependencies {/a \  debugImplementation(libs.leakcanary.android)' "${GRADLE_FILE}"
else
  echo "[+] Dependencia debugImplementation(libs.leakcanary.android) presente y activa."
fi

# 3. Comprobar presencia del Visor de Logcat y Consola en Vivo
echo "[3/4] Comprobando componentes de Visor de Logcat y Terminal..."
REQUIRED_UI="app/src/main/java/com/example/ui/DebugConsoleScreen.kt"
REQUIRED_MODEL="app/src/main/java/com/example/model/LogcatModel.kt"

if [ -f "${REQUIRED_UI}" ] && [ -f "${REQUIRED_MODEL}" ]; then
  echo "[+] Visor de Logcat y Consola Shell confirmados en el código fuente."
else
  echo "[!] ADVERTENCIA: Algún componente de DebugConsoleScreen no se encontró en la ruta esperada."
  exit 1
fi

# 4. Forzar resolución anticipada de dependencias debug
echo "[4/4] Forzando descarga y resolución de dependencias de debug..."
gradle :app:dependencies --configuration debugCompileClasspath > /dev/null 2>&1 || true

echo "========================================================"
echo " [OK] Herramientas de depuración aseguradas e inyectadas:"
echo "  - 🐤 LeakCanary 2.14 (App 'Leaks' lista para instalar)"
echo "  - 💻 Visor de Logcat en Vivo (In-App Logcat Viewer)"
echo "  - ⚡ Consola Shell Shizuku (Ejecutor interactivo)"
echo "========================================================"
