#!/system/bin/sh
# ==============================================================================
# Cazador de Paquetes Huérfanos (clean_orphaned_packages.sh)
# Detecta carpetas de apps desinstaladas en /sdcard/Android/data y /sdcard/Android/obb
# Modo por defecto: Diagnóstico/Escaneo.
# Modo eliminación: Pasar el parámetro "--delete"
# Formato de salida:
# ORPHAN|<SIZE_BYTES>|<PATH>|<PACKAGE_NAME>|<STATUS>
# ==============================================================================

ACTION="${1:-scan}"
STORAGE_BASE="${EXTERNAL_STORAGE:-/sdcard}"
INSTALLED_PKGS_FILE="/data/local/tmp/installed_packages_list.tmp"

# Obtener lista de paquetes instalados actualmente
if command -v pm >/dev/null 2>&1; then
    pm list packages | sed 's/package://' | sort -u > "$INSTALLED_PKGS_FILE" 2>/dev/null
else
    echo "ERROR: El comando 'pm' no está disponible en este entorno shell."
    exit 1
fi

get_dir_size() {
    target_dir="$1"
    if [ -d "$target_dir" ]; then
        kb=$(du -sk "$target_dir" 2>/dev/null | cut -f1)
        if [ -n "$kb" ] && [ "$kb" -ge 0 ] 2>/dev/null; then
            echo $((kb * 1024))
        else
            echo 0
        fi
    else
        echo 0
    fi
}

TOTAL_ORPHAN_BYTES=0
ORPHAN_COUNT=0

check_and_process_folder() {
    folder_path="$1"
    [ -d "$folder_path" ] || return

    for pkg_folder in "$folder_path"/*; do
        [ -d "$pkg_folder" ] || continue
        pkg_name=$(basename "$pkg_folder")

        # Ignorar carpetas del sistema o comunes
        case "$pkg_name" in
            .*|"lost+found"|"media"|"data"|"obb") continue ;;
        esac

        # Verificar si el paquete existe en la lista de paquetes instalados
        if ! grep -Fxq "$pkg_name" "$INSTALLED_PKGS_FILE" 2>/dev/null; then
            dir_size=$(get_dir_size "$pkg_folder")
            if [ "$dir_size" -gt 0 ]; then
                TOTAL_ORPHAN_BYTES=$((TOTAL_ORPHAN_BYTES + dir_size))
                ORPHAN_COUNT=$((ORPHAN_COUNT + 1))

                if [ "$ACTION" = "--delete" ]; then
                    rm -rf "$pkg_folder" 2>/dev/null
                    echo "ORPHAN|${dir_size}|${pkg_folder}|${pkg_name}|DELETED"
                else
                    echo "ORPHAN|${dir_size}|${pkg_folder}|${pkg_name}|DETECTED"
                fi
            fi
        fi
    done
}

# Auditar Android/data y Android/obb
check_and_process_folder "$STORAGE_BASE/Android/data"
check_and_process_folder "$STORAGE_BASE/Android/obb"

# Limpieza del archivo temporal
rm -f "$INSTALLED_PKGS_FILE" 2>/dev/null

echo "SUMMARY|${ORPHAN_COUNT}|${TOTAL_ORPHAN_BYTES}|FINISH"
