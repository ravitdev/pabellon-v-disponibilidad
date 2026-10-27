#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PORT="${PORT:-8080}"
JAR_FILE="pabellon-v-1.0.0.jar"
RUNTIME_DIR=".runtime/linux"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: No se encontro $JAR_FILE en $(pwd)"
    exit 1
fi

get_java_major() {
    local java_bin="$1"
    if [ ! -x "$java_bin" ] && ! command -v "$java_bin" >/dev/null 2>&1; then
        echo 0
        return
    fi
    local ver
    ver=$("$java_bin" -version 2>&1 | awk -F '"' '/version/ {print $2}' | head -n1 || true)
    if [ -z "$ver" ]; then
        echo 0
        return
    fi
    local major
    major=$(echo "$ver" | cut -d'.' -f1)
    if [ "$major" = "1" ]; then
        major=$(echo "$ver" | cut -d'.' -f2)
    fi
    echo "${major:-0}"
}

JAVA_CMD=""

if [ -x "$RUNTIME_DIR/bin/java" ]; then
    JAVA_CMD="$RUNTIME_DIR/bin/java"
elif command -v java >/dev/null 2>&1; then
    MAJOR=$(get_java_major "java")
    if [ "$MAJOR" -ge 21 ] 2>/dev/null; then
        JAVA_CMD="java"
    fi
fi

if [ -z "$JAVA_CMD" ]; then
    echo "Java 21+ no detectado en el sistema. Descargando JRE 21 portable para Linux..."
    UNAME_ARCH="$(uname -m)"
    case "$UNAME_ARCH" in
        x86_64|amd64) ARCH="x64" ;;
        aarch64|arm64) ARCH="aarch64" ;;
        *)
            echo "Arquitectura no soportada automaticamente: $UNAME_ARCH"
            exit 1
            ;;
    esac

    URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/${ARCH}/jre/hotspot/normal/eclipse"
    mkdir -p "$RUNTIME_DIR"
    TMP_ARCHIVE="$RUNTIME_DIR/jre21.tar.gz"

    if command -v curl >/dev/null 2>&1; then
        curl -fL "$URL" -o "$TMP_ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$TMP_ARCHIVE" "$URL"
    else
        echo "Error: Se requiere curl o wget para descargar Java 21."
        exit 1
    fi

    tar -xzf "$TMP_ARCHIVE" -C "$RUNTIME_DIR" --strip-components=1
    rm -f "$TMP_ARCHIVE"
    JAVA_CMD="$RUNTIME_DIR/bin/java"
    echo "JRE 21 portable instalado correctamente."
fi

echo "============================================================"
echo "  Disponibilidad del Pabellon V - Servidor Local"
echo "  URL: http://localhost:${PORT}"
echo "  Presiona Ctrl+C para detener el servidor."
echo "============================================================"

# Abrir el navegador automaticamente cuando el servidor responda
(
    for _ in $(seq 1 30); do
        if command -v curl >/dev/null 2>&1; then
            if curl -s "http://127.0.0.1:${PORT}" >/dev/null 2>&1; then
                break
            fi
        elif command -v wget >/dev/null 2>&1; then
            if wget -q --spider "http://127.0.0.1:${PORT}" 2>/dev/null; then
                break
            fi
        else
            sleep 3
            break
        fi
        sleep 1
    done
    if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "http://localhost:${PORT}" >/dev/null 2>&1 || true
    fi
) &

exec "$JAVA_CMD" -jar "$JAR_FILE" --server.port="$PORT"
