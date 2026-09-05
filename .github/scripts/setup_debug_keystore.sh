#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# Script de Generación Forzada de Keystore de Depuración (CI/CD)
# Obliga a generar una keystore válida e incondicional para la firma de depuración.
# No busca keystores externas ni depende de configuraciones previas.
# ==============================================================================

echo "========================================================"
echo " [CI] Configuración Forzada de Keystore de Depuración"
echo "========================================================"

KEYSTORE_NAME="debug.keystore"
ALIAS="androiddebugkey"
PASSWORD="android"
VALIDITY_DAYS=10000

# 1. Limpieza preventiva: eliminar cualquier keystore existente o corrupta
echo "[1/4] Limpiando residuos previos de keystore..."
rm -f "${KEYSTORE_NAME}"
rm -f "${HOME}/.android/${KEYSTORE_NAME}"

# 2. Generación incondicional de nueva keystore con keytool
echo "[2/4] Generando nueva clave de depuración con keytool..."
keytool -genkey -v \
  -keystore "${KEYSTORE_NAME}" \
  -storepass "${PASSWORD}" \
  -alias "${ALIAS}" \
  -keypass "${PASSWORD}" \
  -keyalg RSA \
  -keysize 2048 \
  -validity "${VALIDITY_DAYS}" \
  -dname "CN=Android Debug,O=Android,C=US"

# 3. Ubicarla en la ruta raíz del proyecto y en el directorio del SDK
echo "[3/4] Asegurando ubicación en raíz del proyecto y ~/.android..."
mkdir -p "${HOME}/.android"
cp "${KEYSTORE_NAME}" "${HOME}/.android/${KEYSTORE_NAME}"

# 4. Establecer permisos de lectura y escritura seguros
echo "[4/4] Estableciendo permisos 644..."
chmod 644 "${KEYSTORE_NAME}"
chmod 644 "${HOME}/.android/${KEYSTORE_NAME}"

echo "========================================================"
echo " [OK] Keystore generada exitosamente y verificada:"
ls -la "${KEYSTORE_NAME}"
ls -la "${HOME}/.android/${KEYSTORE_NAME}"
echo "========================================================"
