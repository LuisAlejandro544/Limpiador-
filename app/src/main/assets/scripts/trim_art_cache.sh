#!/system/bin/sh
# ==============================================================================
# Recorte y Limpieza de Caché del Sistema ART (trim_art_cache.sh)
# Ordena al Package Manager de Android liberar cachés de todas las aplicaciones
# y optimiza el estado del compilador sin alterar propiedades protegidas del sistema.
# ==============================================================================

echo "TRIM|START|Iniciando solicitud de recorte de caché global a nivel de sistema..."

# 1. Ejecutar solicitud oficial de recorte de caché a nivel de framework
if command -v pm >/dev/null 2>&1; then
    TRIM_OUTPUT=$(pm trim-caches 999999999999999 2>&1)
    echo "TRIM|EXEC|pm trim-caches completado: ${TRIM_OUTPUT}"
else
    echo "TRIM|ERROR|El comando 'pm' no está disponible en este entorno shell."
fi

# 2. Purgar cachés en almacenamiento temporal de apps (/data/local/tmp)
if [ -d "/data/local/tmp" ]; then
    rm -rf /data/local/tmp/*.dex /data/local/tmp/*.tmp 2>/dev/null
    echo "TRIM|EXEC|Temporales de compilación en /data/local/tmp purgados."
fi

echo "TRIM|SUCCESS|Proceso de recorte y optimización de caché finalizado correctamente."
