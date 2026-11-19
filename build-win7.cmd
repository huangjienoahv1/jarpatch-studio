@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "WIN7_BUILD_JDK=%~1"
if not defined WIN7_BUILD_JDK set "WIN7_BUILD_JDK=%JARPATCH_WIN7_JDK_HOME%"
if not defined WIN7_BUILD_JDK set "WIN7_BUILD_JDK=%TEMP%\jarpatch-win7-dependencies\jdk-21"

set "WIN7_BUILD_EXIT_CODE=1"
where pwsh.exe >nul 2>nul
if errorlevel 1 (
    echo PowerShell 7 was not found. Install it and add pwsh.exe to PATH.
    goto finish
)

if not exist "%WIN7_BUILD_JDK%\bin\javac.exe" (
    echo BellSoft Liberica JDK 21 x64 was not found at "%WIN7_BUILD_JDK%".
    echo Usage: %~nx0 "C:\Tools\LibericaJDK21"
    echo Or set JARPATCH_WIN7_JDK_HOME to the JDK directory.
    goto finish
)

rem Keep the quoted JDK directory from ending in a backslash.
pwsh.exe -NoLogo -NoProfile -File "%~dp0build.ps1" -Windows7 -JdkHome "%WIN7_BUILD_JDK%\."
set "WIN7_BUILD_EXIT_CODE=%ERRORLEVEL%"

:finish
if "%~1"=="" pause
exit /b %WIN7_BUILD_EXIT_CODE%
