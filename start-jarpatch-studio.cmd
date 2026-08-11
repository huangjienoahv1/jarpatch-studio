@echo off
setlocal

set "APP_ROOT=%~dp0"
set "BACKEND_JAR=%APP_ROOT%backend\target\jarpatch-studio-backend.jar"

where pwsh.exe >nul 2>nul
if errorlevel 1 (
    echo PowerShell 7 was not found. Install PowerShell 7 and add pwsh.exe to PATH.
    pause
    exit /b 1
)

pwsh.exe -NoProfile -Command "if ($PSVersionTable.PSVersion.Major -lt 7) { exit 1 }"
if errorlevel 1 (
    echo PowerShell 7 or later is required.
    pause
    exit /b 1
)

where npm >nul 2>nul
if errorlevel 1 (
    echo Node.js and npm were not found.
    pause
    exit /b 1
)

if not exist "%BACKEND_JAR%" (
    where mvn.cmd >nul 2>nul
    if errorlevel 1 (
        echo Maven was not found and the backend package does not exist.
        pause
        exit /b 1
    )
    call mvn.cmd "-Dmaven.test.skip=true" package
    if errorlevel 1 (
        echo Backend build failed.
        pause
        exit /b 1
    )
)

if not exist "%APP_ROOT%frontend\node_modules\electron" (
    call npm.cmd --prefix "%APP_ROOT%frontend" ci
    if errorlevel 1 (
        echo Frontend dependency installation failed.
        pause
        exit /b 1
    )
)

call npm.cmd --prefix "%APP_ROOT%frontend" start
if errorlevel 1 (
    echo JarPatch Studio failed to start.
    pause
    exit /b 1
)

endlocal
