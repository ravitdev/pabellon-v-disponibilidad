@echo off
setlocal
cd /d "%~dp0"
title Disponibilidad del Pabellon V - Servidor Local

if not exist "pabellon-v-1.0.0.jar" (
    echo [ERROR] No se encontro pabellon-v-1.0.0.jar en %cd%
    pause
    exit /b 1
)

if "%PORT%"=="" set "PORT=8080"
set "RUNTIME_DIR=.runtime\windows"
set "LOCAL_JAVA=%RUNTIME_DIR%\bin\java.exe"
set "JAVA_CMD="

if exist "%LOCAL_JAVA%" (
    set "JAVA_CMD=%LOCAL_JAVA%"
    goto :run_server
)

where java >nul 2>nul
if not errorlevel 1 (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "if (((& java -version 2>&1 | Out-String) -match 'version .(\d+)') -and ([int]$Matches[1] -ge 21)) { exit 0 } else { exit 1 }"
    if not errorlevel 1 (
        set "JAVA_CMD=java"
        goto :run_server
    )
)

echo Java 21+ no detectado. Descargando JRE 21 portable para Windows...
if not exist "%RUNTIME_DIR%" mkdir "%RUNTIME_DIR%"

powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '.runtime\windows\jre21.zip' -UseBasicParsing"
if errorlevel 1 (
    echo [ERROR] No se pudo descargar Java 21 portable.
    pause
    exit /b 1
)

echo Extrayendo JRE 21 portable...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; if (Test-Path '.runtime\windows\extracted') { Remove-Item -Path '.runtime\windows\extracted' -Recurse -Force }; Expand-Archive -Path '.runtime\windows\jre21.zip' -DestinationPath '.runtime\windows\extracted' -Force; $inner = Get-ChildItem -Path '.runtime\windows\extracted' -Directory | Select-Object -First 1; Get-ChildItem -Path $inner.FullName | Move-Item -Destination '.runtime\windows' -Force; Remove-Item -Path '.runtime\windows\extracted' -Recurse -Force; Remove-Item -Path '.runtime\windows\jre21.zip' -Force"
if not exist "%LOCAL_JAVA%" (
    echo [ERROR] No se pudo extraer Java 21 portable correctamente.
    pause
    exit /b 1
)

set "JAVA_CMD=%LOCAL_JAVA%"
echo JRE 21 portable instalado correctamente.

:run_server
echo ============================================================
echo   Disponibilidad del Pabellon V - Servidor Local (Windows)
echo   URL: http://localhost:%PORT%
echo   Presiona Ctrl+C o cierra esta ventana para detener.
echo ============================================================

start "" /b powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -Command "for ($i=0; $i -lt 30; $i++) { try { $null = Invoke-WebRequest -Uri 'http://127.0.0.1:%PORT%' -UseBasicParsing -TimeoutSec 1; break } catch { Start-Sleep -Seconds 1 } }; Start-Process 'http://localhost:%PORT%'"

"%JAVA_CMD%" -jar "pabellon-v-1.0.0.jar" "--server.port=%PORT%"

if errorlevel 1 (
    echo.
    echo El servidor se detuvo con un error.
    pause
)
