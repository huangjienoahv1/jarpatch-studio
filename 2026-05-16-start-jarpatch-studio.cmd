@echo off
setlocal

set "APP_ROOT=%~dp0"
set "BACKEND_JAR=%APP_ROOT%backend\target\jarpatch-studio-backend.jar"
set "FRONTEND_DIR=%APP_ROOT%frontend"
set "NEED_BUILD=0"

cd /d "%APP_ROOT%"

echo ========================================
echo JarPatch Studio Starter
echo ========================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found. Please install or configure Java 17 first.
    pause
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo npm was not found. Please install Node.js first.
    pause
    exit /b 1
)

if not exist "%BACKEND_JAR%" (
    echo Backend jar was not found:
    echo %BACKEND_JAR%
    set "NEED_BUILD=1"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$jar=$env:BACKEND_JAR; Add-Type -AssemblyName System.IO.Compression.FileSystem; try { $zip=[System.IO.Compression.ZipFile]::OpenRead($jar); try { if ($zip.Entries.FullName -contains 'BOOT-INF/lib/cfr-0.152.jar') { exit 0 } else { exit 1 } } finally { $zip.Dispose() } } catch { exit 1 }"
    if errorlevel 1 (
        echo Backend jar is not a complete executable package:
        echo %BACKEND_JAR%
        set "NEED_BUILD=1"
    )
)

if "%NEED_BUILD%"=="1" (
    echo.
    echo Building backend first...
    where mvn >nul 2>nul
    if errorlevel 1 (
        echo Maven was not found. Please install or configure Maven first.
        pause
        exit /b 1
    )
    call mvn -DskipTests package
    if errorlevel 1 (
        echo.
        echo Backend build failed. Please check Maven and Java 17.
        pause
        exit /b 1
    )
)

if not exist "%FRONTEND_DIR%\node_modules\electron" (
    echo Frontend dependencies were not found. Installing...
    call npm install --prefix "%FRONTEND_DIR%"
    if errorlevel 1 (
        echo.
        echo Frontend dependency installation failed. Please check Node.js and npm.
        pause
        exit /b 1
    )
)

echo Starting JarPatch Studio...
echo.
call npm --prefix "%FRONTEND_DIR%" start

if errorlevel 1 (
    echo.
    echo JarPatch Studio failed to start.
    pause
    exit /b 1
)

endlocal
