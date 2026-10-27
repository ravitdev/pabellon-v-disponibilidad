#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

PORT="${PORT:-8080}"
JAR_FILE="target/pabellon-v-1.0.0.jar"
RUNTIME_DIR="$(pwd)/.runtime/linux"

if [ ! -f "pom.xml" ]; then
    echo "Error: No se encontro pom.xml en $(pwd)"
    exit 1
fi

get_javac_major() {
    local javac_bin="$1"
    if [ ! -x "$javac_bin" ] && ! command -v "$javac_bin" >/dev/null 2>&1; then
        echo 0
        return
    fi
    local ver
    ver=$("$javac_bin" -version 2>&1 | awk '{print $2}' | head -n1 || true)
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

SELECTED_JAVA_HOME=""

if [ -x "$RUNTIME_DIR/bin/javac" ]; then
    SELECTED_JAVA_HOME="$RUNTIME_DIR"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    MAJOR=$(get_javac_major "$JAVA_HOME/bin/javac")
    if [ "$MAJOR" -ge 21 ] 2>/dev/null; then
        SELECTED_JAVA_HOME="$JAVA_HOME"
    fi
elif command -v javac >/dev/null 2>&1; then
    MAJOR=$(get_javac_major "javac")
    if [ "$MAJOR" -ge 21 ] 2>/dev/null; then
        JAVAC_PATH=$(readlink -f "$(command -v javac)")
        SELECTED_JAVA_HOME=$(dirname "$(dirname "$JAVAC_PATH")")
    fi
fi

if [ -z "$SELECTED_JAVA_HOME" ]; then
    echo "JDK 21+ no detectado en el sistema. Descargando JDK 21 portable para Linux..."
    UNAME_ARCH="$(uname -m)"
    case "$UNAME_ARCH" in
        x86_64|amd64) ARCH="x64" ;;
        aarch64|arm64) ARCH="aarch64" ;;
        *)
            echo "Arquitectura no soportada automaticamente: $UNAME_ARCH"
            exit 1
            ;;
    esac

    URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/${ARCH}/jdk/hotspot/normal/eclipse"
    rm -rf "$RUNTIME_DIR"
    mkdir -p "$RUNTIME_DIR"
    TMP_ARCHIVE="$RUNTIME_DIR/jdk21.tar.gz"

    if command -v curl >/dev/null 2>&1; then
        curl -fL "$URL" -o "$TMP_ARCHIVE"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$TMP_ARCHIVE" "$URL"
    else
        echo "Error: Se requiere curl o wget para descargar el JDK 21."
        exit 1
    fi

    tar -xzf "$TMP_ARCHIVE" -C "$RUNTIME_DIR" --strip-components=1
    rm -f "$TMP_ARCHIVE"
    SELECTED_JAVA_HOME="$RUNTIME_DIR"
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
echo "  Disponibilidad del Pabellon V - Servidor Local (Linux)"
echo "  URL: http://localhost:${PORT}"
echo "  Presiona Ctrl+C para detener el servidor."
echo "============================================================"

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
