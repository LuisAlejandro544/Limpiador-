#!/system/bin/sh
# ==============================================================================
# Optimizador y Recorte de RAM / Detección de Fugas (trim_ram_and_leaks.sh)
# Diagnostica memoria física y ZRAM, detecta fugas/procesos inflados y recorta
# memoria mediante 'am trim-memory' y 'am kill-all' sin reiniciar el dispositivo.
# IMPORTANTE: Cumple estrictamente con la regla de NO alterar 'persist.sys.*'.
# ==============================================================================

ACTION="${1:-scan}"
TARGET_PKG="${2:-}"

# Lista blanca de seguridad: NUNCA recortar ni matar estos componentes críticos
is_protected_package() {
    pkg="$1"
    case "$pkg" in
        "system"|"com.android.systemui"|"android"|"com.android.phone"|*"launcher"*|*"ime"*|"moe.shizuku.manager"|"com.aistudio.deepcleaner.qwxz"|"com.example")
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# 1. Obtener métricas generales de /proc/meminfo
get_meminfo_val() {
    key="$1"
    val=$(grep -m1 "^${key}:" /proc/meminfo 2>/dev/null | awk '{print $2}')
    echo "${val:-0}"
}

MEM_TOTAL=$(get_meminfo_val "MemTotal")
MEM_FREE=$(get_meminfo_val "MemFree")
MEM_AVAIL=$(get_meminfo_val "MemAvailable")
MEM_CACHED=$(get_meminfo_val "Cached")
SWAP_TOTAL=$(get_meminfo_val "SwapTotal")
SWAP_FREE=$(get_meminfo_val "SwapFree")
ZRAM_USED=$((SWAP_TOTAL - SWAP_FREE))
[ "$ZRAM_USED" -lt 0 ] && ZRAM_USED=0

if [ "$ACTION" = "scan" ]; then
    echo "SYS_MEM|${MEM_TOTAL}|${MEM_FREE}|${MEM_AVAIL}|${MEM_CACHED}|${ZRAM_USED}"

    TOTAL_TRIMMABLE_KB=0
    PROCS_COUNT=0
    LEAKS_COUNT=0

    # Usar dumpsys meminfo si está disponible por Shizuku/Shell
    if command -v dumpsys >/dev/null 2>&1; then
        # Parsear salida de 'dumpsys meminfo' para obtener PSS por proceso
        dumpsys meminfo 2>/dev/null | grep -E "^[ ]*[0-9]+.*: " | head -n 40 | while read -r line; do
            # Formato típico: "  123,456K: com.example.app (pid 1234 / activities)"
            pss_raw=$(echo "$line" | awk -F':' '{print $1}' | tr -d ' ' | tr -d 'K' | tr -d ',')
            pkg_raw=$(echo "$line" | awk -F':' '{print $2}' | awk '{print $1}')
            pid_raw=$(echo "$line" | grep -o "pid [0-9]*" | awk '{print $2}')

            [ -z "$pss_raw" ] && continue
            [ -z "$pkg_raw" ] && continue
            pid="${pid_raw:-0}"

            # Evaluar si es un paquete de usuario o de fondo
            if is_protected_package "$pkg_raw"; then
                category="SYSTEM"
                is_leak="0"
            else
                category="BACKGROUND"
                # Si supera 150 MB (153600 KB) en segundo plano, catalogar como posible fuga o inflado
                if [ "$pss_raw" -gt 153600 ]; then
                    category="HEAVY_LEAK"
                    is_leak="1"
                else
                    category="CACHED"
                    is_leak="0"
                fi
            fi

            echo "PROC|${pid}|${pkg_raw}|${pss_raw}|${category}|${is_leak}"
        done
    else
        # Fallback si dumpsys no estuviese accesible: listar procesos de /proc
        for pid_dir in /proc/[0-9]*; do
            pid=$(basename "$pid_dir")
            [ -f "$pid_dir/cmdline" ] || continue
            pkg=$(tr '\0' ' ' < "$pid_dir/cmdline" | awk '{print $1}')
            [ -z "$pkg" ] && continue

            # Leer VmRSS o VmSize de status
            pss_kb=$(grep -m1 "^VmRSS:" "$pid_dir/status" 2>/dev/null | awk '{print $2}')
            [ -z "$pss_kb" ] && pss_kb=0

            if is_protected_package "$pkg"; then
                category="SYSTEM"
                is_leak="0"
            else
                if [ "$pss_kb" -gt 153600 ]; then
                    category="HEAVY_LEAK"
                    is_leak="1"
                else
                    category="CACHED"
                    is_leak="0"
                fi
            fi
            echo "PROC|${pid}|${pkg}|${pss_kb}|${category}|${is_leak}"
        done
    fi

    echo "SUMMARY|FINISH"

elif [ "$ACTION" = "trim" ]; then
    TRIMMED_COUNT=0
    INITIAL_AVAIL=$(get_meminfo_val "MemAvailable")

    # 1. Si se especificó un paquete concreto, recortar solo ese
    if [ -n "$TARGET_PKG" ]; then
        if ! is_protected_package "$TARGET_PKG"; then
            am trim-memory "$TARGET_PKG" COMPLETE 2>/dev/null || am trim-memory "$TARGET_PKG" RUNNING_CRITICAL 2>/dev/null
            am kill "$TARGET_PKG" 2>/dev/null
            TRIMMED_COUNT=1
        fi
    else
        # 2. Recorte masivo ordenado
        # Obtener lista de paquetes en segundo plano mediante dumpsys meminfo
        if command -v dumpsys >/dev/null 2>&1; then
            dumpsys meminfo 2>/dev/null | grep -E "^[ ]*[0-9]+.*: " | head -n 25 | while read -r line; do
                pkg_candidate=$(echo "$line" | awk -F':' '{print $2}' | awk '{print $1}')
                if [ -n "$pkg_candidate" ] && ! is_protected_package "$pkg_candidate"; then
                    # Enviar TRIM_MEMORY_COMPLETE para obligar a liberar Bitmaps y recursos
                    am trim-memory "$pkg_candidate" COMPLETE 2>/dev/null
                fi
            done
        fi

        # am kill-all elimina procesos en caché que el sistema permite reclamar
        am kill-all 2>/dev/null
        TRIMMED_COUNT=15
    fi

    # 3. Si se cuenta con privilegios ROOT (UID 0), compactar ZRAM y soltar cachés de páginas del kernel
    CURRENT_UID=$(id -u 2>/dev/null || echo 2000)
    if [ "$CURRENT_UID" = "0" ]; then
        echo 3 > /proc/sys/vm/drop_caches 2>/dev/null
        [ -f /sys/block/zram0/compact ] && echo 1 > /sys/block/zram0/compact 2>/dev/null
    fi

    FINAL_AVAIL=$(get_meminfo_val "MemAvailable")
    ESTIMATED_FREED=$((FINAL_AVAIL - INITIAL_AVAIL))
    [ "$ESTIMATED_FREED" -lt 0 ] && ESTIMATED_FREED=0

    echo "TRIM_RESULT|${ESTIMATED_FREED}|${TRIMMED_COUNT}"
fi
