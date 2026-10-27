#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PORT="${PORT:-8080}"
JAR_FILE="target/pabellon-v-1.0.0.jar"
RUNTIME_DIR="$(pwd)/.runtime/mac"

if [ ! -f "pom.xml" ]; then
    echo "Error: No se encontro pom.xml en $(pwd)"
    exit 1
fi

SELECTED_JAVA_HOME=""

if [ -x "$RUNTIME_DIR/Contents/Home/bin/javac" ]; then
    SELECTED_JAVA_HOME="$RUNTIME_DIR/Contents/Home"
elif [ -x "$RUNTIME_DIR/bin/javac" ]; then
    SELECTED_JAVA_HOME="$RUNTIME_DIR"
elif [ -x "/usr/libexec/java_home" ]; then
    SYS_JAVA_HOME=$(/usr/libexec/java_home -v 21+ 2>/dev/null || true)
    if [ -n "$SYS_JAVA_HOME" ] && [ -x "$SYS_JAVA_HOME/bin/javac" ]; then
        SELECTED_JAVA_HOME="$SYS_JAVA_HOME"
    fi
fi

if [ -z "$SELECTED_JAVA_HOME" ]; then
    echo "JDK 21+ no detectado en macOS. Descargando JDK 21 portable..."
    UNAME_ARCH="$(uname -m)"
    case "$UNAME_ARCH" in
        arm64|aarch64) ARCH="aarch64" ;;
        x86_64|amd64) ARCH="x64" ;;
        *)
            echo "Arquitectura no soportada automaticamente: $UNAME_ARCH"
            exit 1
            ;;
    esac

    URL="https://api.adoptium.net/v3/binary/latest/21/ga/mac/${ARCH}/jdk/hotspot/normal/eclipse"
    rm -rf "$RUNTIME_DIR"
    mkdir -p "$RUNTIME_DIR"
    TMP_ARCHIVE="$RUNTIME_DIR/jdk21.tar.gz"

    curl -fL "$URL" -o "$TMP_ARCHIVE"
    tar -xzf "$TMP_ARCHIVE" -C "$RUNTIME_DIR" --strip-components=1
    rm -f "$TMP_ARCHIVE"
    xattr -cr "$RUNTIME_DIR" 2>/dev/null || true

    if [ -x "$RUNTIME_DIR/Contents/Home/bin/javac" ]; then
        SELECTED_JAVA_HOME="$RUNTIME_DIR/Contents/Home"
    else
        SELECTED_JAVA_HOME="$RUNTIME_DIR"
    fi
    echo "JDK 21 portable instalado correctamente."
fi

export JAVA_HOME="$SELECTED_JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"
JAVA_CMD="$JAVA_HOME/bin/java"

echo "============================================================"
echo "  Compilando proyecto y verificando dependencias (Maven)..."
echo "============================================================"
chmod +x ./mvnw
./mvnw package -DskipTests

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: No se encontro el ejecutable generado en $JAR_FILE"
    exit 1
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
