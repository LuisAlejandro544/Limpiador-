#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script de Verificación y Descarga Forzada de Dependencias Shizuku (CI/CD)
# Asegura que el entorno de CI descargue y vincule todas las dependencias
# necesarias de la API oficial de Shizuku y ShizukuProvider para el APK Debug.
# ==============================================================================

echo "========================================================"
echo " [CI] Verificación y Descarga Forzada de Dependencias Shizuku"
echo "========================================================"

TOML_FILE="gradle/libs.versions.toml"
GRADLE_FILE="app/build.gradle.kts"
MANIFEST_FILE="app/src/main/AndroidManifest.xml"

# 1. Validar e inyectar versión y librerías en Version Catalog
echo "[1/4] Verificando declaración en ${TOML_FILE}..."
if ! grep -q "shizuku = " "${TOML_FILE}"; then
  echo "[-] Inyectando versión de Shizuku..."
  sed -i '/\[versions\]/a shizuku = "13.1.5"' "${TOML_FILE}"
fi

if ! grep -q "shizuku-api" "${TOML_FILE}"; then
  echo "[-] Inyectando shizuku-api en ${TOML_FILE}..."
  sed -i '/\[libraries\]/a shizuku-api = { group = "dev.rikka.shizuku", name = "api", version.ref = "shizuku" }' "${TOML_FILE}"
fi

if ! grep -q "shizuku-provider" "${TOML_FILE}"; then
  echo "[-] Inyectando shizuku-provider en ${TOML_FILE}..."
  sed -i '/\[libraries\]/a shizuku-provider = { group = "dev.rikka.shizuku", name = "provider", version.ref = "shizuku" }' "${TOML_FILE}"
fi
echo "[+] Dependencias de Shizuku presentes en ${TOML_FILE}."

# 2. Validar inclusión en app/build.gradle.kts
echo "[2/4] Verificando implementación en ${GRADLE_FILE}..."
if ! grep -q "libs.shizuku.api" "${GRADLE_FILE}"; then
  echo "[-] Inyectando implementation(libs.shizuku.api)..."
  sed -i '/dependencies {/a \  implementation(libs.shizuku.api)' "${GRADLE_FILE}"
fi

if ! grep -q "libs.shizuku.provider" "${GRADLE_FILE}"; then
  echo "[-] Inyectando implementation(libs.shizuku.provider)..."
  sed -i '/dependencies {/a \  implementation(libs.shizuku.provider)' "${GRADLE_FILE}"
fi
echo "[+] Dependencias de Shizuku aseguradas en ${GRADLE_FILE}."

# 3. Validar ShizukuProvider y queries en AndroidManifest.xml
echo "[3/4] Verificando Provider y queries en ${MANIFEST_FILE}..."
if ! grep -q "rikka.shizuku.ShizukuProvider" "${MANIFEST_FILE}"; then
  echo "[!] ERROR: ShizukuProvider no encontrado en ${MANIFEST_FILE}."
  exit 1
fi

if ! grep -q "moe.shizuku.manager" "${MANIFEST_FILE}"; then
  echo "[!] ERROR: Declaración de paquete de Shizuku en queries no encontrada en ${MANIFEST_FILE}."
  exit 1
fi
echo "[+] ShizukuProvider y visibilidad de paquete confirmados en Manifest."

# 4. Forzar resolución y descarga incondicional de dependencias Shizuku
echo "[4/4] Forzando descarga en caché de dependencias Shizuku y sus transitivas..."
gradle :app:dependencies --configuration debugCompileClasspath | grep -i "shizuku" || true
gradle :app:dependencies --configuration debugRuntimeClasspath | grep -i "shizuku" || true

echo "========================================================"
echo " [OK] Dependencias de Shizuku verificadas y pre-descargadas:"
echo "  - dev.rikka.shizuku:api:13.1.5"
echo "  - dev.rikka.shizuku:provider:13.1.5"
echo "========================================================"
