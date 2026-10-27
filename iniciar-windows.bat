@echo off
setlocal
cd /d "%~dp0"
title Disponibilidad del Pabellon V - Servidor Local

if not exist "pom.xml" (
    echo [ERROR] No se encontro pom.xml en %cd%
    pause
    exit /b 1
)

if "%PORT%"=="" set "PORT=8080"
set "RUNTIME_DIR=%~dp0.runtime\windows"
set "LOCAL_JAVAC=%RUNTIME_DIR%\bin\javac.exe"

if exist "%LOCAL_JAVAC%" (
    set "JAVA_HOME=%RUNTIME_DIR%"
    goto :compile_and_run
)

if "%JAVA_HOME%"=="" goto :check_path_javac
if not exist "%JAVA_HOME%\bin\javac.exe" goto :check_path_javac
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (((& '%JAVA_HOME%\bin\javac.exe' -version 2>&1 | Out-String) -match 'javac (\d+)') -and ([int]$Matches[1] -ge 21)) { exit 0 } else { exit 1 }"
if not errorlevel 1 goto :compile_and_run

:check_path_javac
where javac >nul 2>nul
if errorlevel 1 goto :download_jdk
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (((& javac -version 2>&1 | Out-String) -match 'javac (\d+)') -and ([int]$Matches[1] -ge 21)) { exit 0 } else { exit 1 }"
if errorlevel 1 goto :download_jdk
for /f "usebackq delims=" %%H in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "(& java -XshowSettings:properties -version 2>&1 | Select-String 'java.home = (.*)').Matches[0].Groups[1].Value.Trim()"`) do set "JAVA_HOME=%%H"
if exist "%JAVA_HOME%\bin\javac.exe" goto :compile_and_run

:download_jdk
echo JDK 21+ no detectado. Descargando JDK 21 portable para Windows...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; if (Test-Path '.runtime\windows') { Remove-Item -Path '.runtime\windows' -Recurse -Force }; New-Item -ItemType Directory -Force -Path '.runtime\windows' | Out-Null; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '.runtime\windows\jdk21.zip' -UseBasicParsing"
if errorlevel 1 (
    echo [ERROR] No se pudo descargar el JDK 21 portable.
    pause
    exit /b 1
)

echo Extrayendo JDK 21 portable...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; Expand-Archive -Path '.runtime\windows\jdk21.zip' -DestinationPath '.runtime\windows\extracted' -Force; $inner = Get-ChildItem -Path '.runtime\windows\extracted' -Directory | Select-Object -First 1; Get-ChildItem -Path $inner.FullName | Move-Item -Destination '.runtime\windows' -Force; Remove-Item -Path '.runtime\windows\extracted' -Recurse -Force; Remove-Item -Path '.runtime\windows\jdk21.zip' -Force"
if not exist "%LOCAL_JAVAC%" (
    echo [ERROR] No se pudo extraer el JDK 21 portable correctamente.
    pause
    exit /b 1
)

set "JAVA_HOME=%RUNTIME_DIR%"
echo JDK 21 portable instalado correctamente.

:compile_and_run
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
set "JAR_FILE=target\pabellon-v-1.0.0.jar"

echo ============================================================
echo   Compilando proyecto y verificando dependencias (Maven)...
echo ============================================================
call "%~dp0mvnw.cmd" package -DskipTests
if errorlevel 1 (
    echo.
    echo [ERROR] Fallo la compilacion del proyecto con Maven.
    pause
    exit /b 1
)

if not exist "%JAR_FILE%" (
    echo [ERROR] No se encontro el ejecutable generado en %JAR_FILE%
    pause
    exit /b 1
)

echo ============================================================
echo   Disponibilidad del Pabellon V - Servidor Local (Windows)
echo   URL: http://localhost:%PORT%
echo   Presiona Ctrl+C o cierra esta ventana para detener.
echo ============================================================

start "" /b powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -Command "for ($i=0; $i -lt 30; $i++) { try { $null = Invoke-WebRequest -Uri 'http://127.0.0.1:%PORT%' -UseBasicParsing -TimeoutSec 1; break } catch { Start-Sleep -Seconds 1 } }; Start-Process 'http://localhost:%PORT%'"

"%JAVA_CMD%" -jar "%JAR_FILE%" "--server.port=%PORT%"

if errorlevel 1 (
    echo.
    echo El servidor se detuvo con un error.
    pause
)
