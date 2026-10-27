#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PORT="${PORT:-8080}"
JAR_FILE="pabellon-v-1.0.0.jar"
RUNTIME_DIR=".runtime/mac"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: No se encontro $JAR_FILE en $(pwd)"
    exit 1
fi

JAVA_CMD=""

if [ -x "$RUNTIME_DIR/Contents/Home/bin/java" ]; then
    JAVA_CMD="$RUNTIME_DIR/Contents/Home/bin/java"
elif [ -x "$RUNTIME_DIR/bin/java" ]; then
    JAVA_CMD="$RUNTIME_DIR/bin/java"
elif [ -x "/usr/libexec/java_home" ]; then
    SYS_JAVA_HOME=$(/usr/libexec/java_home -v 21+ 2>/dev/null || true)
    if [ -n "$SYS_JAVA_HOME" ] && [ -x "$SYS_JAVA_HOME/bin/java" ]; then
        JAVA_CMD="$SYS_JAVA_HOME/bin/java"
    fi
fi

if [ -z "$JAVA_CMD" ]; then
    echo "Java 21+ no detectado en macOS. Descargando JRE 21 portable..."
    UNAME_ARCH="$(uname -m)"
    case "$UNAME_ARCH" in
        arm64|aarch64) ARCH="aarch64" ;;
        x86_64|amd64) ARCH="x64" ;;
        *)
            echo "Arquitectura no soportada automaticamente: $UNAME_ARCH"
            exit 1
            ;;
    esac

    URL="https://api.adoptium.net/v3/binary/latest/21/ga/mac/${ARCH}/jre/hotspot/normal/eclipse"
    mkdir -p "$RUNTIME_DIR"
    TMP_ARCHIVE="$RUNTIME_DIR/jre21.tar.gz"

    curl -fL "$URL" -o "$TMP_ARCHIVE"
    tar -xzf "$TMP_ARCHIVE" -C "$RUNTIME_DIR" --strip-components=1
    rm -f "$TMP_ARCHIVE"
    xattr -cr "$RUNTIME_DIR" 2>/dev/null || true

    if [ -x "$RUNTIME_DIR/Contents/Home/bin/java" ]; then
        JAVA_CMD="$RUNTIME_DIR/Contents/Home/bin/java"
    else
        JAVA_CMD="$RUNTIME_DIR/bin/java"
    fi
    echo "JRE 21 portable instalado correctamente."
fi

echo "============================================================"
echo "  Disponibilidad del Pabellon V - Servidor Local (macOS)"
echo "  URL: http://localhost:${PORT}"
echo "  Presiona Ctrl+C o cierra esta ventana para detener."
echo "============================================================"

(
    for _ in $(seq 1 30); do
        if curl -s "http://127.0.0.1:${PORT}" >/dev/null 2>&1; then
            break
        fi
        sleep 1
    done
    open "http://localhost:${PORT}" >/dev/null 2>&1 || true
) &

exec "$JAVA_CMD" -jar "$JAR_FILE" --server.port="$PORT"
