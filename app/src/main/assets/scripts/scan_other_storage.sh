#!/system/bin/sh
# ==============================================================================
# Script de Análisis del Almacenamiento "Otros" para Android
# Diseñado para ejecutarse a través de Shell ADB / Shizuku o terminal local.
# Formato de salida por línea:
# ITEM|<SIZE_BYTES>|<PATH>|<CATEGORY>|<SAFETY_LEVEL:SAFE|CAUTION|KEEP>|<DESCRIPTION>
# ==============================================================================

STORAGE_BASE="${EXTERNAL_STORAGE:-/sdcard}"

get_size_bytes() {
    file_path="$1"
    if [ -f "$file_path" ]; then
        # Intentar stat estándar de toybox/toolbox
        stat -c %s "$file_path" 2>/dev/null || wc -c < "$file_path" 2>/dev/null || echo 0
    elif [ -d "$file_path" ]; then
        # Obtener tamaño de carpeta en bytes mediante du -k
        kb=$(du -sk "$file_path" 2>/dev/null | cut -f1)
        if [ -n "$kb" ] && [ "$kb" -ge 0 ] 2>/dev/null; then
            echo $((kb * 1024))
        else
            echo 0
        fi
    else
        echo 0
    fi
}

# 1. Miniaturas gigantes en DCIM/.thumbnails (100% SEGURO)
THUMB_DIR="$STORAGE_BASE/DCIM/.thumbnails"
if [ -d "$THUMB_DIR" ]; then
    for item in "$THUMB_DIR"/*; do
        if [ -e "$item" ]; then
            size=$(get_size_bytes "$item")
            if [ "$size" -gt 0 ]; then
                base_name=$(basename "$item")
                echo "ITEM|${size}|${item}|Miniaturas del Sistema|SAFE|Archivo residual de miniaturas generado por la galería ($base_name)"
            fi
        fi
    done
fi

# 2. Cachés de aplicaciones en /sdcard/Android/data/*/cache (100% SEGURO)
DATA_DIR="$STORAGE_BASE/Android/data"
if [ -d "$DATA_DIR" ]; then
    for pkg_dir in "$DATA_DIR"/*; do
        if [ -d "$pkg_dir" ]; then
            pkg_name=$(basename "$pkg_dir")
            cache_folder="$pkg_dir/cache"
            if [ -d "$cache_folder" ]; then
                c_size=$(get_size_bytes "$cache_folder")
                if [ "$c_size" -gt 10240 ]; then # Mayor a 10KB
                    echo "ITEM|${c_size}|${cache_folder}|Caché de Aplicación|SAFE|Caché externa no indexada de la app ${pkg_name}"
                fi
            fi
        fi
    done
fi

# 3. Archivos temporales y descargas incompletas (.tmp, .crdownload, .part, .log) (100% SEGURO)
TEMP_EXTS="tmp crdownload part dmp log bak"
for ext in $TEMP_EXTS; do
    find "$STORAGE_BASE" -maxdepth 4 -type f -name "*.$ext" 2>/dev/null | while read -r temp_file; do
        if [ -f "$temp_file" ]; then
            t_size=$(get_size_bytes "$temp_file")
            if [ "$t_size" -gt 0 ]; then
                echo "ITEM|${t_size}|${temp_file}|Temporales y Logs|SAFE|Descarga incompleta o log residual (*.$ext)"
            fi
        fi
    done
done

# 4. Volcados en /data/local/tmp accesibles con privilegios ADB/Shizuku (100% SEGURO)
if [ -d "/data/local/tmp" ]; then
    for tmp_item in /data/local/tmp/*; do
        if [ -e "$tmp_item" ]; then
            loc_size=$(get_size_bytes "$tmp_item")
            if [ "$loc_size" -gt 0 ]; then
                echo "ITEM|${loc_size}|${tmp_item}|Temporales del Sistema|SAFE|Archivo temporal del sistema ADB en /data/local/tmp"
            fi
        fi
    done
fi

# 5. Respaldos antiguos de bases de datos de mensajería (PRECAUCIÓN)
find "$STORAGE_BASE/Android/media" -maxdepth 5 -type f -name "msgstore-*.1.db.crypt*" 2>/dev/null | while read -r db_file; do
    if [ -f "$db_file" ]; then
        db_size=$(get_size_bytes "$db_file")
        if [ "$db_size" -gt 0 ]; then
            echo "ITEM|${db_size}|${db_file}|Respaldos Históricos|CAUTION|Copia de seguridad local antigua. Verifica antes de borrar."
        fi
    done
done

# 6. Archivos grandes sin formato estándar en raíz o Download > 25MB (PRECAUCIÓN)
find "$STORAGE_BASE/Download" "$STORAGE_BASE" -maxdepth 2 -type f -size +25000k 2>/dev/null | while read -r big_file; do
    # Si no es video ni audio conocido
    case "$big_file" in
        *.mp4|*.mkv|*.mp3|*.m4a|*.jpg|*.png|*.pdf|*.zip|*.apk)
            ;;
        *)
            b_size=$(get_size_bytes "$big_file")
            if [ "$b_size" -gt 0 ]; then
                echo "ITEM|${b_size}|${big_file}|Archivos Desconocidos|CAUTION|Archivo pesado sin categorizar que suma a la categoría 'Otros'."
            fi
            ;;
    esac
done

# 7. Datos de juegos OBB (NO TOCAR / KEEP)
OBB_DIR="$STORAGE_BASE/Android/obb"
if [ -d "$OBB_DIR" ]; then
    for obb_pkg in "$OBB_DIR"/*; do
        if [ -d "$obb_pkg" ]; then
            obb_size=$(get_size_bytes "$obb_pkg")
            if [ "$obb_size" -gt 0 ]; then
                obb_name=$(basename "$obb_pkg")
                echo "ITEM|${obb_size}|${obb_pkg}|Datos de Juegos (OBB)|KEEP|Datos esenciales de $obb_name. No se recomienda eliminar."
            fi
        fi
    done
fi

echo "DONE|ANALYSIS_COMPLETE"
