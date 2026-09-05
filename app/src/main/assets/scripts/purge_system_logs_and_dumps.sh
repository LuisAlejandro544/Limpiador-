#!/system/bin/sh
# ==============================================================================
# Purga de Registros del Sistema y Volcados de Error (purge_system_logs_and_dumps.sh)
# Detecta y purga archivos temporales .dump, .log, .trace, .tombstone y /data/local/tmp
# Modo por defecto: Diagnóstico/Escaneo.
# Modo eliminación: Pasar el parámetro "--delete"
# Formato de salida:
# DUMP|<SIZE_BYTES>|<PATH>|<TYPE>|<STATUS>
# ==============================================================================

ACTION="${1:-scan}"
STORAGE_BASE="${EXTERNAL_STORAGE:-/sdcard}"
TOTAL_LOG_BYTES=0
LOG_COUNT=0

get_file_size() {
    target_file="$1"
    if [ -f "$target_file" ]; then
        stat -c %s "$target_file" 2>/dev/null || wc -c < "$target_file" 2>/dev/null || echo 0
    else
        echo 0
    fi
}

process_log_file() {
    file_path="$1"
    log_type="$2"
    [ -f "$file_path" ] || return

    size=$(get_file_size "$file_path")
    if [ "$size" -gt 0 ]; then
        TOTAL_LOG_BYTES=$((TOTAL_LOG_BYTES + size))
        LOG_COUNT=$((LOG_COUNT + 1))

        if [ "$ACTION" = "--delete" ]; then
            rm -f "$file_path" 2>/dev/null
            echo "DUMP|${size}|${file_path}|${log_type}|DELETED"
        else
            echo "DUMP|${size}|${file_path}|${log_type}|DETECTED"
        fi
    fi
}

# 1. Volcados en /data/local/tmp (Archivos residuales de depuración)
if [ -d "/data/local/tmp" ]; then
    for item in /data/local/tmp/*; do
        if [ -f "$item" ]; then
            process_log_file "$item" "ADB_TMP"
        fi
    done
fi

# 2. Registros de caídas (tombstones y anr) si son accesibles por permisos de Shell
for dump_dir in "/data/tombstones" "/data/anr" "/data/system/dropbox"; do
    if [ -d "$dump_dir" ]; then
        for dump_file in "$dump_dir"/*; do
            if [ -f "$dump_file" ]; then
                process_log_file "$dump_file" "SYSTEM_CRASH_DUMP"
            fi
        done
    fi
done

# 3. Archivos de registro en almacenamiento compartido (.log, .dmp, .trace)
find "$STORAGE_BASE" -maxdepth 4 -type f \( -name "*.log" -o -name "*.dmp" -o -name "*.trace" -o -name "*_crash.txt" \) 2>/dev/null | while read -r log_item; do
    process_log_file "$log_item" "APP_LOG"
done

echo "SUMMARY|${LOG_COUNT}|${TOTAL_LOG_BYTES}|FINISH"
